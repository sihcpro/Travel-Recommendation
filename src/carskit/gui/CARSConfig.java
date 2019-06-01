package carskit.gui;

import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import librec.util.Logs;

public class CARSConfig {
    private static boolean debug = false;
    public int line_number = 0;
    public String name = "";
    public String value = "";
    public String line = "";
    public Map<String, String> config = new Hashtable<String, String>();

    public CARSConfig(String config_name, String config_line) {
        name = config_name;
        line = config_line;
        add_config(line);
//      if (!debug)
//          Logs.off();
    }

    public String to_string() {
        String configs =  name + "=" + value;
        for(Map.Entry<String, String> detail : config.entrySet()) {
            configs += " -" + detail.getKey() + " " + detail.getValue();
        }
        return configs;
    }
    
    Pattern re_value = Pattern.compile("=[^ \\n,]+");               
    Pattern re_details = Pattern.compile("-[a-zA-Z][\\w-]* -?[.\\w]+");             
    public void add_config(String config_string) {
        String config_detail, config_key, config_value;
        Matcher match_value = re_value.matcher(config_string);
        Matcher match_details = re_details.matcher(config_string);
        if (match_details.find()) {
            while (true) {
                config_detail = match_details.group();
                config_key = config_detail.substring(1, config_detail.indexOf(" "));
                config_value = config_detail.substring(config_detail.indexOf(" ") + 1, config_detail.length());
                if (debug)
                    Logs.debug(String.format("%s|%s|%s", config_detail, config_key, config_value));
                config.put(config_key, config_value);
                if (!match_details.find())
                    break;
            }
        } else if (match_value.find()) {
            value = match_value.group();
            value = value.substring(1, value.length());
        }
    }
    
    public void change_line(String old_config, String new_config) {
        Logs.debug(line + " -> " + line.replace(old_config, new_config));
        line = line.replace(old_config, new_config);
    }
    
    public void change_config(String key, String value) {
        String old_config = String.format("-%s %s", key, config.get(key));
        String new_config = String.format("-%s %s", key, value);
        Logs.debug(line + " -> " + line.replace(old_config, new_config));
        line = line.replace(old_config, new_config);
    }
}
