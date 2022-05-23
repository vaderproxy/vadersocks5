package transparentproxy;

import base.Helper;
import base.IniFile;
import base.ScriptConfig;
import base.Socks5SniffProxy;
import base.miniBrowser;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import struct.Config;

public class TransparentProxy {

    public static IniFile iFile = null;

    public static String readConfig(String section, String name, String error) {
        String value = iFile.getString(section, name, "");
        if (value.isEmpty()) {
            if (error != null) {
                Helper.error(error);
            }
        }

        return value;
    }

    private static String readFile(String filePath) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        content = content.trim() + "\r\n\r\n";

        return content;
    }

    public static void test2() {
        int port = 8888;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                String text;

                do {
                    text = reader.readLine();
                    String reverseText = new StringBuilder(text).reverse().toString();
                    System.out.println(text);

                } while (!text.equals("bye"));

                socket.close();
            }

        } catch (Exception ex) {
            // System.out.println("Server exception: " + ex.getMessage());
            // ex.printStackTrace();
        }
    }

    public static String decompress(final byte[] compressed) throws Exception {
        final StringBuilder outStr = new StringBuilder();
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }

        final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }

        return outStr.toString();
    }

    public static void unGunzipFile(String compressedFile, String decompressedFile) {
        byte[] buffer = new byte[1024];
        try {

            FileInputStream fileIn = new FileInputStream(compressedFile);

            GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);

            FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);

            int bytes_read;

            while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {

                fileOutputStream.write(buffer, 0, bytes_read);
            }

            gZIPInputStream.close();
            fileOutputStream.close();

            System.out.println("The file was decompressed successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void test() {

        try {
            SocketAddress proxyAddr = new InetSocketAddress("127.0.0.1", 8887);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);

            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(InetAddress.getByName("localhost"), 80));
            //Socket socket = new Socket(InetAddress.getByName("localhost"), 80);

            // Create input and output streams to read from and write to the server
            String dt = readFile("headers.txt");
            socket.getOutputStream().write(dt.getBytes());
            //PrintStream out = new PrintStream(socket.getOutputStream());
            //InputStream is = socket.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = in.readLine();
            while (line != null) {
                System.out.println(line);
                line = in.readLine();
                byte[] bb = line.getBytes();
                int t = 8;
            }

            // Close our streams
            in.close();
            //out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            String iname = "main.ini";

            IniFile ifile = new IniFile(iname);
            iFile = ifile;
            Config.network_interface = iFile.getString("main", "network_interface", null);
            Config.my_ip = iFile.getString("main", "my_ip", "127.0.0.1");
            Config.url_back = iFile.getString("main", "url_back", "http://localhost/vader");
            Config.script_config = iFile.getString("main", "script_config", "1.ini");
            Config.port = iFile.getInt("main", "port", 8887);
            Config.port_forward_start = iFile.getInt("main", "port_forward_start", 8080);
            Config.port_forward_cnt = iFile.getInt("main", "port_forward_cnt", 1);
            Config.allow_all_ip = iFile.getString("main", "allow_all_ip", "n").trim().toLowerCase().equals("y");

            String allowed_ips = readConfig("main", "allowed_ips", "Не указан host");

            String[] ips = allowed_ips.split(",");
            for (int i = 0; i < ips.length; i++) {
                String ip = ips[i].trim();
                if (!ip.isEmpty()) {
                    Config.allowed_ips.add(ip);
                }
            }

            Config.logfile = iFile.getString("main", "log", "logs.txt");

        } catch (Exception ex) {
            System.out.println("Ошибка! Ини файл лаговый");
            System.exit(0);
        }

        ScriptConfig sc = ScriptConfig.getInstance();
        sc.init(Config.script_config);

        Socks5SniffProxy s5 = new Socks5SniffProxy(Config.port);
        s5.network_interface = Config.network_interface;
        s5.run();
        // } catch (Exception e) {
        //}
        //}
    }

}
