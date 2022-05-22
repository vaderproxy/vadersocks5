package struct;

public class HttpResponce {

    public int http_version = 0;
    public int code = 0;
    public int content_length = 0;
    public boolean is_text = false;
    public boolean is_gzip = false;
    public Cookies cookies = new Cookies();

    public String location;
}
