package base;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import struct.*;

public class Memcached extends Thread {

    private Map<String, HttpInfoResult> cache1;
    private Map<String, HttpInfoResult> cache2;

    private static Memcached instance;

    private Memcached() {
        cache1 = Collections.synchronizedMap(new HashMap<String, HttpInfoResult>());
        cache2 = Collections.synchronizedMap(new HashMap<String, HttpInfoResult>());
    }

    public HttpInfoResult get(String key) {
        HttpInfoResult r = cache1.get(key);
        if (r == null) {
            r = cache2.get(key);
        }

        return r;
    }

    public void set(String key, HttpInfoResult val) {
        LocalDateTime now = LocalDateTime.now();
        int minute = now.getMinute();
        Map<String, HttpInfoResult> cache = (minute >= 5) ? cache2 : cache1;
        Map<String, HttpInfoResult> cache_old = (minute >= 5) ? cache1 : cache2;
        cache_old.put(key, null);
        cache.put(key, val);
    }

    public void set_null(String key) {
        cache1.put(key, null);
        cache2.put(key, null);
    }

    public void run() {
        while (true) {
            Helper.delay(60000);

            LocalDateTime now = LocalDateTime.now();
            int minute = now.getMinute();
            if (minute % 5 < 2) {
                continue;
            }

            //Map<String, HttpInfoResult> cache = (minute >= 5) ? cache2 : cache1;
            Map<String, HttpInfoResult> cache_old = (minute >= 5) ? cache1 : cache2;
            cache_old.clear();
        }
    }

    public static Memcached getInstance() {
        if (instance == null) {
            instance = new Memcached();
        }

        return instance;
    }
}
