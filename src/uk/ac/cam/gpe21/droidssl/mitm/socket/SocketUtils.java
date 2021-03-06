/*
 * Copyright 2013-2014 Graham Edgecombe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.gpe21.droidssl.mitm.socket;

import com.sun.jna.LastErrorException;
import com.sun.jna.ptr.IntByReference;
import uk.ac.cam.gpe21.droidssl.mitm.util.CLibrary;

import javax.net.ssl.SSLSocket;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public final class SocketUtils {

    private static final Class<?> SERVER_SOCKET_CHANNEL_IMPL;
    private static final Field SERVER_SOCKET_CHANNEL_FD;

    private static final Class<?> SOCKET_CHANNEL_IMPL;
    private static final Field SOCKET_CHANNEL_FD;

    private static final Class<?> SSL_SOCKET_IMPL;
    private static final Field SSL_SOCKET_INPUT;

    private static final Field FD;

    static {
        try {
            SERVER_SOCKET_CHANNEL_IMPL = Class.forName("sun.nio.ch.ServerSocketChannelImpl");

            SERVER_SOCKET_CHANNEL_FD = SERVER_SOCKET_CHANNEL_IMPL.getDeclaredField("fd");
            SERVER_SOCKET_CHANNEL_FD.setAccessible(true);

            SOCKET_CHANNEL_IMPL = Class.forName("sun.nio.ch.SocketChannelImpl");

            SOCKET_CHANNEL_FD = SOCKET_CHANNEL_IMPL.getDeclaredField("fd");
            SOCKET_CHANNEL_FD.setAccessible(true);

            SSL_SOCKET_IMPL = Class.forName("sun.security.ssl.SSLSocketImpl");

            SSL_SOCKET_INPUT = SSL_SOCKET_IMPL.getDeclaredField("sockInput");
            SSL_SOCKET_INPUT.setAccessible(true);

            FD = FileDescriptor.class.getDeclaredField("fd");
            FD.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Opens an unbound {@link ServerSocket} with the {@code IP_TRANSPARENT}
     * option enabled.
     *
     * @return The {@link ServerSocket}.
     * @throws IOException if an I/O error occurs opening the socket or if the
     * {@code IP_TRANSPARENT} option could not be set.
     */
    public static ServerSocket openTproxyServerSocket() throws IOException {
        /*
		 * The old IO ServerSocket class obtains an FD and binds to the socket
		 * in one go in a single native call, making it impossible to set the
		 * IP_TRANSPARENT option prior to binding.
		 *
		 * However, the NIO ServerSocketChannel class obtains an FD upon
		 * creation and allows the bind to be delayed, allowing us to insert
		 * the setsockopt() call between these two events.
		 *
		 * It also has a method to return an object which appears like a
		 * ServerSocket but actually translates all method calls into
		 * operations on the underlying ServerSocketChannel instead. This is
		 * useful so we don't have to convert the entire MITM server to use
		 * NIO. (Notably, SSL code is trickier in NIO as you have to implement
		 * it yourself with the SSLEngine class.)
         */
        ServerSocketChannel ch = ServerSocketChannel.open();
        int fd = getFileDescriptor(ch);

        IntByReference yes = new IntByReference(1);
        /* sizeof(int) = 4 */

        try {
            CLibrary.INSTANCE.setsockopt(fd, CLibrary.SOL_IP, CLibrary.IP_TRANSPARENT, yes.getPointer(), 4);
        } catch (LastErrorException ex) {
            throw new IOException("setsockopt: " + CLibrary.INSTANCE.strerror(ex.getErrorCode()));
        }

        return new WorkaroundNioServerSocket(ch);
    }

    /**
     * Opens an unbound, unconnected {@link Socket} with the
     * {@code IP_TRANSPARENT} option enabled.
     *
     * @return The {@link Socket}.
     * @throws IOException if an I/O error occurs opening the socket or if the
     * {@code IP_TRANSPARENT} option could not be set.
     */
    public static Socket openTproxySocket() throws IOException {
        /* See comments in openTproxyServerSocket(). */
        SocketChannel ch = SocketChannel.open();
        int fd = getFileDescriptor(ch);

        IntByReference yes = new IntByReference(1);

        try {
            CLibrary.INSTANCE.setsockopt(fd, CLibrary.SOL_IP, CLibrary.IP_TRANSPARENT, yes.getPointer(), 4);
        } catch (LastErrorException ex) {
            throw new IOException("setsockopt: " + CLibrary.INSTANCE.strerror(ex.getErrorCode()));
        }

        return new WorkaroundNioSocket(ch);
    }

    public static int getFileDescriptor(ServerSocketChannel channel) throws IOException {
        try {
            FileDescriptor fd = (FileDescriptor) SERVER_SOCKET_CHANNEL_FD.get(channel);
            return FD.getInt(fd);
        } catch (IllegalAccessException ex) {
            throw new IOException(ex);
        }
    }

    public static int getFileDescriptor(SocketChannel channel) throws IOException {
        try {
            FileDescriptor fd = (FileDescriptor) SOCKET_CHANNEL_FD.get(channel);
            return FD.getInt(fd);
        } catch (IllegalAccessException ex) {
            throw new IOException(ex);
        }
    }

    public static int getFileDescriptor(Socket socket) throws IOException {
        try {
            Object in;
            if (socket instanceof SSLSocket) {
                /*
				 * Socket.getInputStream() on an SSLSocket returns the
				 * InputStream which reads the decrypted data from a buffer in
				 * memory - in this case, we read the private
				 * SSLSocketImpl.sockInput field with reflection to get at the
				 * InputStream which is backed by a file descriptor.
                 */
                in = SSL_SOCKET_INPUT.get(socket);
            } else {
                in = socket.getInputStream();
            }

            if (!(in instanceof FileInputStream)) {
                throw new IOException("sockInput is not an instance of FileInputStream");
            }

            FileInputStream fin = (FileInputStream) in;
            FileDescriptor fd = fin.getFD();

            return FD.getInt(fd);
        } catch (IllegalAccessException ex) {
            throw new IOException(ex);
        }
    }

    public static InetSocketAddress getOriginalDestination(Socket socket) throws IOException {
        InetAddress localAddress = socket.getLocalAddress();
        if (localAddress instanceof Inet4Address) {
            return getOriginalDestination4(socket);
        } else if (localAddress instanceof Inet6Address) {
            return getOriginalDestination6(socket);
        } else {
            throw new IOException("Unknown local address type (expected java.net.Inet{4,6}Address): " + localAddress.getClass().getName());
        }
    }

    public static InetSocketAddress getOriginalDestination4(Socket socket) throws IOException {
        int fd = getFileDescriptor(socket);

        CLibrary.sockaddr_in addr = new CLibrary.sockaddr_in();
        try {
            IntByReference len = new IntByReference(addr.size());
            CLibrary.INSTANCE.getsockopt(fd, CLibrary.SOL_IP, CLibrary.SO_ORIGINAL_DST, addr.getPointer(), len);

            /*
			 * This call is required to copy the values from the native memory
			 * backing the struct into the fields of the Java object.
             */
            addr.read();
        } catch (LastErrorException ex) {
            throw new IOException("getsockopt: " + CLibrary.INSTANCE.strerror(ex.getErrorCode()));
        }

        if (addr.sin_family == CLibrary.PF_INET) {
            /*
			 * JNA takes care of reversing the order of the bytes in integer
			 * struct fields. However, sin_port is already in network byte
			 * order (big endian), the same byte order as used by Java, so if
			 * we used a short here, JNA would actually convert sin_port to
			 * little endian (if the underlying CPU was little endian).
			 *
			 * Therefore we represent sin_port as a byte array in the structure
			 * and manually shift the bytes around to convert it into an
			 * integer, such that this code works on both big and little endian
			 * CPUs.
             */
            int port = ((addr.sin_port[0] & 0xFF) << 8) | (addr.sin_port[1] & 0xFF);
            InetAddress ip = InetAddress.getByAddress(addr.sin_addr);
            return new InetSocketAddress(ip, port);
        } else {
            throw new IOException("Unknown protocol family (expected PF_INET): " + addr.sin_family);
        }
    }

    public static InetSocketAddress getOriginalDestination6(Socket socket) throws IOException {
        int fd = getFileDescriptor(socket);

        CLibrary.sockaddr_in6 addr = new CLibrary.sockaddr_in6();
        try {
            IntByReference len = new IntByReference(addr.size());
            CLibrary.INSTANCE.getsockopt(fd, CLibrary.SOL_IPV6, CLibrary.IP6T_SO_ORIGINAL_DST, addr.getPointer(), len);

            /* See comment in getOriginalDestination4(). */
            addr.read();
        } catch (LastErrorException ex) {
            throw new IOException("getsockopt: " + CLibrary.INSTANCE.strerror(ex.getErrorCode()));
        }

        if (addr.sin6_family == CLibrary.PF_INET6) {
            /* See comment in getOriginalDestination4(). */
            int port = ((addr.sin6_port[0] & 0xFF) << 8) | (addr.sin6_port[1] & 0xFF);
            InetAddress ip = InetAddress.getByAddress(addr.sin6_addr);
            return new InetSocketAddress(ip, port);
        } else {
            throw new IOException("Unknown protocol family (expected PF_INET6): " + addr.sin6_family);
        }
    }

    private SocketUtils() {
        /* to prevent instantiation */
    }
}
