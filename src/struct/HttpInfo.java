package struct;

import base.Helper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class HttpInfo {

    public String html = null;
    public ByteBuffer gzip_buffer;
    public int gzip_offset;

    public HttpRequest request = new HttpRequest();
    public HttpResponce responce = new HttpResponce();

    public void parse_request_headers(String text) {
        //System.out.println(text);
        Scanner sc = new Scanner(text);
        for (int i = 0, skip_empty = 0; sc.hasNext(); i++) {
            String line = sc.nextLine();

            if (i == 0) {
                int pos = line.indexOf(' ');

                if (pos < 0) {
                    continue;
                }

                String name = line.substring(0, pos);
                String value = line.substring(pos + 1).trim();
                int pos2 = value.indexOf(' ');
                if (pos2 > 0) {
                    value = value.substring(0, pos2);
                }
                this.request.method = name;
                this.request.is_post = name.equals("POST");
                this.request.path = value;
                this.request.path_lower = this.request.path.toLowerCase();
            } else {
                if (line.isEmpty()) {
                    skip_empty++;
                    continue;
                }

                if ((this.request.is_post) && (skip_empty > 0)) {
                    this.request.post = Helper.parse_query_string(line);
                    continue;
                }

                int pos = line.indexOf(':');
                if (pos < 0) {
                    continue;
                }

                String name = line.substring(0, pos);
                String lname = name.toLowerCase();
                String value = line.substring(pos + 1).trim();

                if (lname.equals("user-agent")) {
                    this.request.uagent = value;
                    continue;
                }

                if (lname.equals("host")) {
                    this.request.host = value;
                    continue;
                }

                if (lname.equals("cookie")) {
                    this.request.cookies.setCookies(value);
                    continue;
                }

                if (lname.equals("referer")) {
                    this.request.referer = value;
                    continue;
                }

                if (lname.equals("authorization")) {
                    this.request.authorization = value;
                    continue;
                }

            }
        }
    }

    public void parse_responce_headers(String text) {
        //System.out.println(text);
        Scanner sc = new Scanner(text);
        for (int i = 0, skip_empty = 0; sc.hasNext(); i++) {
            String line = sc.nextLine();
            if (i == 0) {
                line = line.trim();
                int pos0 = line.indexOf("HTTP/");
                int pos = line.indexOf(' ');
                int pos2 = line.lastIndexOf(' ');
                if ((pos < 0) || (pos2 < 0) || (pos0 < 0)) {
                    this.responce.http_version = 2;
                    return;
                    //continue;
                }

                char ver = line.charAt(pos0 + 5);
                this.responce.http_version = (ver == '1') ? 1 : 2;
                String code = line.substring(pos, pos2).trim();
                pos2 = code.indexOf(' ');
                if (pos2 > 0) {
                    code = code.substring(0, pos2);
                }
                this.responce.code = Helper.getInt(code, -1);
                if (this.responce.code < 0) {
                    return;
                }

            } else {
                if (line.isEmpty()) {
                    skip_empty++;
                    break;
                }

                int pos = line.indexOf(':');
                if (pos < 0) {
                    continue;
                }

                String name = line.substring(0, pos);
                String lname = name.toLowerCase();
                String value = line.substring(pos + 1).trim();

                if (lname.equals("content-type")) {
                    this.responce.is_text = value.indexOf("text/") >= 0;
                    continue;
                }

                if (lname.equals("content-encoding")) {
                    this.responce.is_gzip = value.indexOf("gzip") >= 0;
                    continue;
                }

                if (lname.equals("set-cookie")) {
                    this.responce.cookies.setCookies(value);
                    continue;
                }

                if (lname.equals("content-length")) {
                    this.responce.content_length = Helper.getInt(value, 0);
                    continue;
                }

                if (lname.equals("location")) {
                    this.responce.location = value;
                    continue;
                }

            }
        }
    }

    public String unpack_gzip(byte[] buff, int size, int offset, boolean only_get_length) {
        try {
            int off = offset;

            if (offset > 0) {
                for (int i = offset; (i < offset + 6) && (i < size - 3); i++) {
                    if ((buff[i] == 13) && (buff[i + 1] == 10)) {
                        off = i + 2;
                        break;
                    }
                }

                if (off != offset) {
                    byte[] content_length_array = Arrays.copyOfRange(buff, offset, off - 2);
                    try {
                        if (this.responce.content_length == 0) {
                            this.responce.content_length = Integer.parseInt(new String(content_length_array), 16);
                        }

                    } catch (Exception ex) {
                        off = offset;
                    }
                }
            }

            if (only_get_length) {
                gzip_offset = off;
                return null;
            }

            byte[] slice = Arrays.copyOfRange(buff, off, size - off + 1);

            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(slice));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

            StringBuilder otvet = new StringBuilder();
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    otvet.append(line);
                    otvet.append("\r\n");
                }
            } catch (Exception ex) {
            }

            return otvet.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTextFile() {
        if (this.request.path_lower == null) {
            return true;
        }
        if (this.request.path_lower.indexOf(".js") > 0) {
            return true;
        }
        if (this.request.path_lower.indexOf(".css") > 0) {
            return true;
        }
        if (this.request.path_lower.indexOf(".txt") > 0) {
            return true;
        }

        return false;
    }
}
