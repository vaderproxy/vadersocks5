package struct;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class Cookies {

    public Map<String, String> cookies;

    public Cookies() {
        cookies = new HashMap<String, String>();
    }

    public void setCookies(String cookie) {
        String[] carr = cookie.split(";");
        for (int i = 0; i < carr.length; i++) {

            String elem = carr[i].trim();
            String[] esplit = elem.split("=");
            if (esplit.length != 2) {
                continue;
            }

            String test = esplit[0].toLowerCase();

            if (test.equals("expires")) {
                continue;
            }

            if (test.equals("domain")) {
                continue;
            }

            if (test.equals("path")) {
                continue;
            }

            if (test.equals("max-age")) {
                continue;
            }

            cookies.put(esplit[0], esplit[1]);
        }
    }

    public String cookieSTR() {
        String res = "";
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            res += entry.getKey() + "=" + (entry.getValue()) + ";";
        }

        return res;
    }

}
