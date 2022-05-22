package struct;

import java.util.HashMap;

public class HttpRequest {

    public boolean is_post = false;
    public HashMap<String, String> post;
    public String method;
    public String path;
    public String path_lower;
    public String uagent;
    public String host;
    public String referer;
    public String authorization;
    public Cookies cookies = new Cookies();

    public String get_request() {
        return this.method + " http://" + this.host + this.path;
    }

}
