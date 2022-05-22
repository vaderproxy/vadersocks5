package base;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hex {

    public static String StrToHex(String str) {
        String res = "";
        for (int i = 0; i < str.length(); i++) {
            int code = (int) str.charAt(i);
            res += Integer.toHexString(code);
        }

        return res;
    }

    public static String StrToUrlHex(String str) {
        try {
            return URLEncoder.encode(str, "UTF8");
        } catch (UnsupportedEncodingException ex) {
           return null;
        }
    }

    public static String formatHex(String str) {
        String res = StrToHex(str);
        res = "0x" + res;
        return res;
    }

    public static String unhex(String str) {
        if (str.length() % 2 > 0) {
            return null;
        }

        byte[] res = new byte[str.length() / 2];
        for (int i = 0; i < str.length(); i += 2) {
            String chr = str.substring(i, i + 2);
            try {
                res[i / 2] = (byte) Integer.parseInt(chr, 16);
            } catch (Exception ex) {
                return "";
            }
        }

        try {
            String rStr = new String(res, "UTF8");
            return rStr;
        } catch (Exception ex) {
            return null;
        }

    }

}
