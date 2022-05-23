package base;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import struct.*;

public class Socks5SniffProxy implements Runnable {

    static final byte[] OK1 = new byte[]{0x05, 0x00};
    static final byte[] ERR1 = new byte[]{0x05, -1};
    static final byte[] OK2 = new byte[]{0x05, 0x0, 0x00, 0x01};

    private int port;

    public String host = "localhost";
    public String network_interface = null;

    int bufferSize = 8192;

    private WriteHostThread write_host;

    public Socks5SniffProxy(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            System.out.println("run");
            write_host = new WriteHostThread();
            write_host.start();
            Memcached memcached = Memcached.getInstance();
            memcached.start();

            Selector selector = SelectorProvider.provider().openSelector();

            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            if (this.network_interface != null) {
                NetworkInterface nif = NetworkInterface.getByName(this.network_interface);
                boolean adr_ok = false;
                InetAddress adr = null;
                try {
                    Enumeration<InetAddress> nifAddresses = nif.getInetAddresses();
                    adr = nifAddresses.nextElement();
                    try {
                        adr = nifAddresses.nextElement();
                        adr = nifAddresses.nextElement();
                        adr_ok = true;
                    } catch (Exception next_el) {
                        next_el.printStackTrace();
                    }
                } catch (Exception ex) {

                }

                if (adr_ok) {
                    serverChannel.socket().bind(new InetSocketAddress(adr, port));
                } else {
                    serverChannel.socket().bind(new InetSocketAddress(Config.my_ip, port));
                }
            } else {
                serverChannel.socket().bind(new InetSocketAddress(this.host, port));
            }
            serverChannel.register(selector, serverChannel.validOps());

            while (selector.select() > -1) {
//                System.out.println("run2");
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        try {
                            if (key.isAcceptable()) {
                                //System.out.println("accept");
                                accept(key);
                            } else if (key.isConnectable()) {
                                //System.out.println("connect");
                                connect(key);
                            } else if (key.isReadable()) {
                                //System.out.println("read");
                                read(key);
                            } else if (key.isWritable()) {
                                //System.out.println("write");
                                write(key);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            close(key);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    private void accept(SelectionKey key) throws IOException, ClosedChannelException {
        SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
        String my_ip = newChannel.socket().getInetAddress().toString();
        //System.out.println("my ip = " + my_ip);
        if (!Config.allow_all_ip) {
            if ((my_ip.indexOf("127.0.0.1") < 0) && (my_ip.indexOf(Config.my_ip) < 0)) {
                System.out.println("lol!");
                return;
            }
        }
        newChannel.configureBlocking(false);
        newChannel.register(key.selector(), (SelectionKey.OP_READ));
    }

    private void cmdError(SelectionKey key, Socks5SniffProxy.Attachment attachment, byte err) throws IllegalStateException, IOException,
            UnknownHostException, ClosedChannelException {
        SocketChannel channel = ((SocketChannel) key.channel());
        attachment.out.put(err).flip();
        channel.write(attachment.out);
    }

    private void readHeader2(SelectionKey key, Socks5SniffProxy.Attachment attachment) throws IllegalStateException, IOException,
            UnknownHostException, ClosedChannelException {
        attachment.readHeader2 = true;
        byte[] ar = attachment.in.array();
        if (ar[0] != 5) {
            close(key);
            return;
        }

        if (ar[1] != 1) {
            this.cmdError(key, attachment, Socks5Error.noCMD);
            close(key);
            return;
        }

        int atype = ar[3];
        if (atype != 1) {
            this.cmdError(key, attachment, Socks5Error.noProtocol);
            close(key);
            return;
        }

        byte[] ip = new byte[]{ar[4], ar[5], ar[6], ar[7]};
        int port = (((0xFF & ar[8]) << 8) + (0xFF & ar[9]));

        SocketChannel peer = SocketChannel.open();
        peer.configureBlocking(false);
        peer.connect(new InetSocketAddress(InetAddress.getByAddress(ip), port));

        SelectionKey peerKey = peer.register(key.selector(), SelectionKey.OP_CONNECT);
        attachment.peer = peerKey;

        Socks5SniffProxy.Attachment peerAtt = new Socks5SniffProxy.Attachment();
        peerAtt.peer = key;
        peerAtt.ip = ip;
        peerAtt.port = new byte[]{ar[8], ar[9]};
        peerKey.attach(peerAtt);
        if (port == 80) {
            peerAtt.info = new HttpInfo();

        }
        attachment.in.clear();
        key.interestOps(0);

    }

    private void readHeader1(SelectionKey key, Socks5SniffProxy.Attachment attachment) throws IllegalStateException, IOException,
            UnknownHostException, ClosedChannelException {
        attachment.readHeader1 = true;
        SocketChannel channel = ((SocketChannel) key.channel());
        byte[] ar = attachment.in.array();
        byte b = attachment.in.get(0);
        if (b != 5) {
            attachment.out.put(ERR1).flip();
            channel.write(attachment.out);
            close(key);
            return;
        }

        attachment.in.clear();
        attachment.out.put(OK1).flip();
        channel.write(attachment.out);

        attachment.out.clear();

    }

    private void close(SelectionKey key) throws IOException {
        try {
            key.cancel();
            key.channel().close();
            SelectionKey peerKey = ((Socks5SniffProxy.Attachment) key.attachment()).peer;
            if (peerKey != null) {
                ((Socks5SniffProxy.Attachment) peerKey.attachment()).peer = null;
                if ((peerKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                    ((Socks5SniffProxy.Attachment) peerKey.attachment()).out.flip();
                }
                peerKey.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("close_error!");
        }
    }

    private void read(SelectionKey key) throws IOException, UnknownHostException, ClosedChannelException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Socks5SniffProxy.Attachment attachment = ((Socks5SniffProxy.Attachment) key.attachment());
        if (attachment == null) {
            attachment = new Socks5SniffProxy.Attachment();
            key.attach(attachment);
            attachment.in = ByteBuffer.allocate(bufferSize);
            attachment.out = ByteBuffer.allocate(bufferSize);
        }

        attachment.iteration++;
        int r_cnt = channel.read(attachment.in);

        if (r_cnt < 1) {

            close(key);
        } else if (attachment.peer == null) {

            if (attachment.readHeader1 == false) {
                attachment.isMain = true;
                readHeader1(key, attachment);
                return;
            }

            if (attachment.readHeader2 == false) {
                readHeader2(key, attachment);
            }

        } else {

            attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_WRITE);
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
            attachment.in.flip();
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());
        Attachment attachment2 = null;
        if (attachment.peer != null) {
            attachment2 = (Attachment) attachment.peer.attachment();
        }

        int cnt_w = channel.write(attachment.out);

        attachment.iteration++;

        if ((attachment.info != null) && (cnt_w > 0)) {
            if (attachment.info.request.host == null) {
                String s = new String(attachment.out.array(), 0, cnt_w);
                attachment.info.parse_request_headers(s);
            }
        }

        if (/*(Math.abs(4) > 5 - 2) &&*/(attachment2 != null) && (attachment2.info != null) && (cnt_w > 0)) {
            boolean first = false;
            HttpInfo info = attachment2.info;

            info.html = null;
            if ((info.responce.http_version == 0) && (cnt_w > 25) && (!info.isTextFile())) {
                info.html = new String(attachment.out.array(), 0, cnt_w);
                first = true;
                info.parse_responce_headers(info.html);

                if ((info.request.host != null) && (!info.isTextFile()) && (info.responce.is_text)) {
                    this.write_host.hosts.add(info.request.method + " http://" + info.request.host + info.request.path);
                }
                if (info.responce.http_version == 2) {
                }

                if ((attachment2.info.responce.http_version == 1) && (attachment2.info.responce.is_text)) {
                    int pos = info.html.indexOf("\r\n\r\n");

                    if (!attachment2.info.responce.is_gzip) {
                        info.html = info.html.substring(pos + 4);
                    } else {
                        info.html = info.unpack_gzip(attachment.out.array(), cnt_w, pos + 4, true);
                        if (info.responce.content_length > 0) {
                            int max_buf_size = 128 * 1024;
                            int buf_size = info.responce.content_length > max_buf_size ? max_buf_size : info.responce.content_length;
                            info.gzip_buffer = ByteBuffer.allocate(buf_size);
                            int rm = info.gzip_buffer.remaining();
                            int cnt_rm = cnt_w - info.gzip_offset;
                            if (cnt_rm > rm) {
                                cnt_rm = rm;
                            }
                            info.gzip_buffer.put(attachment.out.array(), info.gzip_offset, cnt_rm);
                            if (info.gzip_buffer.remaining() == 0) {
                                info.html = info.unpack_gzip(info.gzip_buffer.array(), info.gzip_buffer.array().length, 0, false);
                            }
                        }
                    }
                }

            }

            if ((info.responce.http_version == 1) && (info.responce.is_text) && (!info.isTextFile())) {
                if (!first) {
                    if (!attachment2.info.responce.is_gzip) {
                        info.html = new String(attachment.out.array(), 0, cnt_w);
                    } else if (info.gzip_buffer != null) {
                        int size_w = cnt_w < info.gzip_buffer.remaining() ? cnt_w : info.gzip_buffer.remaining();
                        if (size_w > 0) {
                            info.gzip_buffer.put(attachment.out.array(), 0, size_w);
                            if (info.gzip_buffer.remaining() == 0) {
                                info.html = info.unpack_gzip(info.gzip_buffer.array(), info.gzip_buffer.array().length, 0, false);
                            }
                        }
                    }
                }

                try {
                    HttpInfoParser parser = new HttpInfoParser();
                    parser.info = info;
                    parser.check();
                } catch (Exception e) {
                }
            }
        }
        if (cnt_w == -1) {
            close(key);
        } else if (attachment.out.remaining() == 0) {
            if (attachment.peer == null) {
                close(key);
            } else {
                attachment.out.clear();
                if (attachment.peer.isValid()) {
                    attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_READ);
                }
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);

            }
        }
    }

    private void connect(SelectionKey key) throws IOException {

        Attachment a = (Attachment) (key.attachment());

        if (!a.isMain) {
            int port = a.port[0] * 256 + a.port[1];
        }
        SocketChannel channel = ((SocketChannel) key.channel());
        SocketChannel channel2 = ((SocketChannel) a.peer.channel());

        Attachment attachment = ((Attachment) key.attachment());

        attachment.in = ((Attachment) attachment.peer.attachment()).out;
        attachment.out = ((Attachment) attachment.peer.attachment()).in;
        ((Attachment) attachment.peer.attachment()).out = attachment.in;

        try {
            channel.finishConnect();
            attachment.in.put(OK2);
            attachment.in.put(a.ip);
            attachment.in.put(a.port).flip();
        } catch (Exception ex) {
            close(key);
            return;
        }

        attachment.peer.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        key.interestOps(0);
    }

    static class Attachment {

        ByteBuffer in;
        ByteBuffer out;
        SelectionKey peer;
        boolean readHeader1 = false;
        boolean readHeader2 = false;
        boolean isMain = false;

        byte[] ip = null;
        byte[] port = null;

        int iteration = 0;
        HttpInfo info = null;

    }

}
