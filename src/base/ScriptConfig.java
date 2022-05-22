package base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import struct.Config;
import struct.PHPScript;

public class ScriptConfig {

    private static ScriptConfig _instance;
    private String _ini_path;
    private ArrayList<String> sections;
    private ArrayList<PHPScript> conf_list;

    public int size() {
        return conf_list.size();
    }

    public PHPScript get(int i) {
        return this.conf_list.get(i);
    }

    public void init(String ini_path) {
        this._ini_path = ini_path;
        ArrayList<PHPScript> list = new ArrayList();
        try {
            IniFile ifile = new IniFile(Config.script_config);
            this.sections = ifile.sections;
            for (int i = 0; i < this.sections.size(); i++) {
                String section = this.sections.get(i);
                PHPScript conf = new PHPScript();
                String name = ifile.getString(section, "name", null);
                String alias = ifile.getString(section, "alias", null);
                String text = ifile.getString(section, "text", null);
                String text2 = ifile.getString(section, "text2", null);
                if ((text == null) || (alias == null)) {
                    continue;
                }
                conf.name = name;
                conf.alias = alias;
                String[] tt = text.split(",");
                for (int j = 0; j < tt.length; j++) {
                    tt[j] = tt[j].trim();
                    if (tt[j].isEmpty()) {
                        continue;
                    }

                    conf.text.add(tt[j]);
                }

                if (text2 != null) {
                    tt = text2.split(",");
                    for (int j = 0; j < tt.length; j++) {
                        tt[j] = tt[j].trim();
                        if (tt[j].isEmpty()) {
                            continue;
                        }

                        conf.text2.add(tt[j]);
                    }
                }

                list.add(conf);
            }

            this.conf_list = list;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }

    }

    private ScriptConfig() {
    }

    public static ScriptConfig getInstance() {
        if (_instance == null) {
            _instance = new ScriptConfig();
        }

        return _instance;
    }

}
