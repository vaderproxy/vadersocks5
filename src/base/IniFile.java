package base;

import java.util.regex.Matcher;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class IniFile {

    private Pattern _section;
    private Pattern _keyValue;
    private Map<String, Map<String, String>> _entries;
    public ArrayList<String> sections = new ArrayList();

    public IniFile(final String path) throws IOException {
        this._section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
        this._keyValue = Pattern.compile("\\s*([^=]*)=(.*)");
        this._entries = new HashMap<String, Map<String, String>>();
        this.load(path);
    }

    public void load(final String path) throws IOException {
        try (final BufferedReader br = new BufferedReader(new FileReader(path))) {
            String section = null;
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = this._section.matcher(line);
                if (m.matches()) {
                    section = m.group(1).trim();
                    if (!sections.contains(m)) {
                        sections.add(section);
                    }
                } else {
                    if (section == null) {
                        continue;
                    }
                    m = this._keyValue.matcher(line);
                    if (!m.matches()) {
                        continue;
                    }
                    final String key = m.group(1).trim();
                    final String value = m.group(2).trim();
                    Map<String, String> kv = this._entries.get(section);
                    if (kv == null) {
                        this._entries.put(section, kv = new HashMap<String, String>());
                    }
                    kv.put(key, value);
                }
            }
        }
    }

    public String getString(final String section, final String key, final String defaultvalue) {
        final Map<String, String> kv = this._entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        String r = kv.get(key);
        if (r == null) {
            r = defaultvalue;
        }
        return r;
    }

    public int getInt(final String section, final String key, final int defaultvalue) {
        final Map<String, String> kv = this._entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Helper.getInt(kv.get(key), defaultvalue);
    }

    public float getFloat(final String section, final String key, final float defaultvalue) {
        final Map<String, String> kv = this._entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Float.parseFloat(kv.get(key));
    }

    public double getDouble(final String section, final String key, final double defaultvalue) {
        final Map<String, String> kv = this._entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Double.parseDouble(kv.get(key));
    }
}
