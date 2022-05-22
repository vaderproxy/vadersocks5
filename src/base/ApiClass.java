package base;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import struct.*;

public class ApiClass extends Thread {

    private boolean _is_session;
    private String _url = null;
    private String _post = null;

    public ApiClass(String url) {
        this._url = url;
    }

    public ApiClass(HttpInfoResult result) {
        this._is_session = result.is_session;
        this._url = result.url_back();
    }

    public ApiClass(String url, String post) {
        this._url = url;
        this._post = post;
    }

    public void run() {
        miniBrowser br = new miniBrowser();
        if (this._post == null) {
            br.getPage(_url);
        } else {
            br.excutePost(_url, _post);
        }

        try {
            String debugfile = !this._is_session ? Config.debugfile : Config.debugfile2;

            File dFile = new File(debugfile);
            FileWriter fw = new FileWriter(dFile, true);

            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String log = "[" + formatter.format(date) + "] " + _url + "\r\n";
            fw.write(log);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
