package struct;

import base.Hex;

public class HttpInfoResult {

    public int cms_id;
    public boolean is_session = false;
    public String url;
    public String login;
    public String password;
    public String authorization;
    public String user_agent;
    public String cookie;

    public HttpInfoResult() {
    }

    public HttpInfoResult(boolean is_session, int cms_id, String url, String user_agent, String cookie) {
        this.is_session = is_session;
        this.cms_id = cms_id;
        this.url = url;
        this.user_agent = user_agent;
        this.cookie = cookie;
    }

    public HttpInfoResult(String url, String login, String password) {
        this.url = url;
        this.login = login;
        this.password = password;
    }

    public HttpInfoResult(int cms_id, String url, String login, String password) {
        this.cms_id = cms_id;
        this.url = url;
        this.login = login;
        this.password = password;
    }

    public String url_back() {
        String r = null;
        if (!this.is_session) {
            r = Config.url_back + "/api/get/" + this.cms_id + "/?";
            r += "url=" + Hex.StrToUrlHex(url) + "&login=" + Hex.StrToUrlHex(login) + "&password=" + Hex.StrToUrlHex(password);
            if (authorization != null) {
                r += "&authorization=" + Hex.StrToUrlHex(authorization);
            }
        } else {
            r = Config.url_back + "/api/session/" + this.cms_id + "/?";
            r += "url=" + Hex.StrToUrlHex(url) + "&cookie=" + Hex.StrToUrlHex(this.cookie);
            if (this.user_agent != null) {
                r += "&ua=" + Hex.StrToUrlHex(this.user_agent);
            }
            if (authorization != null) {
                r += "&authorization=" + Hex.StrToUrlHex(authorization);
            }
        }
        
        return r;
    }

}
