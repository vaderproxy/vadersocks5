package base;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import struct.Config;

public class WriteHostThread extends Thread {

    public Set<String> hosts;

    public WriteHostThread() {
        Set<String> set = new HashSet<String>();
        Set<String> synset = Collections.synchronizedSet(set);
        this.hosts = synset;
    }

    public void run() {
        while (true) {
            Helper.delay(10000);

            if (hosts.isEmpty()) {
                continue;
            }
            try {
                File hostFile = new File(Config.logfile);
                FileWriter fw = new FileWriter(hostFile, true);
                for (String host : hosts) {
                    Date date = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String log = "[" + formatter.format(date) + "] " + host + "\r\n";
                    fw.write(log);

                }
                fw.close();
                this.hosts.clear();
            } catch (Exception e) {
            }
        }
    }
}
