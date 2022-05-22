package base;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import struct.*;

public class miniBrowser {

    public boolean use_tor = false;
    //////////////////
    Cookies cookies;
    String loc_need = "";

    public miniBrowser() {
        cookies = new Cookies();
    }

    public String getSiteBase(String site) {
        site = site.toLowerCase();
        site = site.replaceAll("https?://(www\\.)?", "");
        int ind = site.indexOf('/');
        if (ind > -1) {
            site = site.substring(0, ind);
        }
        return site;
    }

    public String getHostName(String site) {
        String host = getSiteBase(site);
        int ind = host.indexOf('.');
        if (ind > -1) {
            host = host.substring(0, ind);
        }
        return host;
    }

    public void saveUrl(String filename, String urlString) {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } catch (Exception ee) {

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public String excutePost(String targetURL, String urlParameters, int follow) {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            if (this.use_tor) {
                int pport = 9050;
                if ((new java.io.File(".").getCanonicalPath()).indexOf(":") > 0) {
                    pport = 9150;
                }

                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", pport));
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setInstanceFollowRedirects(false);
            connection.setReadTimeout(60000);
            connection.setConnectTimeout(60000);

            if (urlParameters.length() > 0) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
            } else {
                connection.setRequestMethod("GET");
            }

            //connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Cookie",
                    cookies.cookieSTR());
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:71.0) laika Gecko/20100101 Firefox/71.0");

            connection.setRequestProperty("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            if (urlParameters.length() > 0) {
                connection.setDoOutput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
            };

            String loc = null;
            for (int i = 1;; i++) {
                String headerName = connection.getHeaderFieldKey(i);
                String headerValue = connection.getHeaderField(i);
                if ((headerName != null)) {
                    if (headerName.equals("Set-Cookie")) {
                        cookies.setCookies(headerValue);
                    };

                    if (headerName.equals("Location")) {
                        String loctemp = headerValue;
                        String base1 = this.getSiteBase(targetURL);
                        if (!Pattern.matches("https?://.*?", loctemp)) {
                            if (loctemp.indexOf('/') != 0) {
                                loctemp = '/' + loctemp;
                            }
                            loctemp = "http://" + base1 + loctemp;
                        }
                        String base2 = this.getSiteBase(loctemp);
                        if ((base1.equals(base2)) && (loctemp.indexOf(this.loc_need) > -1)) {
                            loc = loctemp;
                        }
                    };
                }

                if (headerName == null && headerValue == null) {
                    break;
                }
            }

            if (follow == -1) {
                return "";
            }

            if ((follow != 0) && (loc != null)) {
                if (follow < 0) {
                    follow++;
                } else {
                    follow--;
                }
                return this.excutePost(loc, "", follow);
            }

            if (follow < 0) {
                return "";
            }
            //Get Response	
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append("\r\n");
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            //e.printStackTrace();
            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String excutePost(String targetURL, String urlParameters) {
        return excutePost(targetURL, urlParameters, 3);
    }

    public String getPage(String targetURL) {
        try {
            String html = this.excutePost(targetURL, "");
            if (html == null) {
                return "";
            }
            return html;
        } catch (Exception e) {
            return "";
        }
    }
}
