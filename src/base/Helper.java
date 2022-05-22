package base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import struct.*;

public class Helper {

    public static void delay(int d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException ex) {

        }
    }

    public static String randStr(int len, String chars) {
        char[] arr = chars.toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            char c = arr[random.nextInt(arr.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public static int getInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static String replace(String in, String ths, String that) {
        StringBuilder sb = new StringBuilder(in);
        int idx = sb.indexOf(ths);
        while (idx > -1) {
            sb.replace(idx, idx + ths.length(), that);
            idx = sb.indexOf(ths);
        }

        return sb.toString();

    }

    public static String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }

            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }

    public static void strToFile(String file, String str, boolean append) {
        try {
            File newTextFile = new File(file);
            FileWriter fw = new FileWriter(newTextFile, append);
            fw.write(str);
            fw.close();

        } catch (Exception iox) {
            //        iox.printStackTrace();
        }
    }

    public static void error(String text) {
        System.out.println(text);
        System.exit(0);
    }

    public static HashMap<String, String> parse_query_string(String post) {
        HashMap<String, String> post_map = new HashMap();
        String[] param_list = post.split("&");
        for (int i1 = 0; i1 < param_list.length; i1++) {
            String pv = param_list[i1];
            String[] pv_arr = pv.split("=");
            if (pv_arr.length != 2) {
                continue;
            }

            try {
                post_map.put(pv_arr[0], java.net.URLDecoder.decode(pv_arr[1], StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                // not going to happen - value came from JDK's own StandardCharsets
            }
        }
        return post_map;
    }

    public static String md5Custom(String st) {
        MessageDigest messageDigest = null;
        byte[] digest = new byte[0];

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(st.getBytes());
            digest = messageDigest.digest();
        } catch (Exception e) {
            // тут можно обработать ошибку
            // возникает она если в передаваемый алгоритм в getInstance(,,,) не существует
            //e.printStackTrace();
        }

        BigInteger bigInt = new BigInteger(1, digest);
        String md5Hex = bigInt.toString(16);

        while (md5Hex.length() < 32) {
            md5Hex = "0" + md5Hex;
        }

        return md5Hex;
    }
}
