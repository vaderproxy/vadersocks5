package struct;

import java.util.ArrayList;

public class Config {

    public static ArrayList<String> allowed_ips = new ArrayList();
    public static String url_back = "http://localhost/vader";
    public static String my_ip = "127.0.0.1";
    public static String network_interface = null;
    public static int port = 8887;
    public static int port_forward_start = 8080;
    public static int port_forward_cnt = 5;
    public static boolean allow_all_ip = false;
    /////
    public static String script_config = "1.ini";
    public static String logfile = "logs.txt";
    public static String debugfile = "debug.txt";
    public static String debugfile2 = "debug2.txt";
}
