package vaderproxy;

import base.*;
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
import struct.*;

public class VaderProxy {

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
    }

}
