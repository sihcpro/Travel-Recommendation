package carskit.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import librec.util.Logs;

public class ManageConfig {
    private static boolean debug = true;
    private String config_path;
    private Vector<String> config_lines = new Vector<String>();
    private Vector<Boolean> is_config = new Vector<Boolean>();
    private boolean check_path_user;
    public Map<String, CARSConfig> configs = new Hashtable<String, CARSConfig>();
    

    public ManageConfig(String path) {
        config_path = path;
        check_path_user = false;
        if (!debug)
            Logs.off();
    }

    public static void main(String[] args){
        try {
            ManageConfig mc = new ManageConfig("./setting.conf");
            mc.read_config();
            mc.write_config();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void check_path() {
    	String separator=System.getProperty("file.separator");
    	String file_name = config_path.substring(config_path.lastIndexOf(separator) + 1, config_path.length());
    	if (file_name == "setting_user.conf") {
    		check_path_user = true;
    	} else {
    		config_path = config_path.substring(0, config_path.lastIndexOf(separator) + 1) + "setting_user.conf";
    		Logs.debug(config_path);
    	}
    }

    Pattern re_name = Pattern.compile("[\\w.]*=");              
    public void read_config() throws IOException {
        Logs.debug("Start read config");
        BufferedReader reader = new BufferedReader(new FileReader(config_path));
        String line;
        int line_number = 0;
        String config_name;
        while ((line = reader.readLine()) != null) {
            line_number++;
            if (line.matches("^[\\w.]*=.*$")) {
                Matcher match_name = re_name.matcher(line);
                is_config.add(true);
                if (match_name.find()) {
                    config_name = match_name.group().toLowerCase();
                    config_name = config_name.substring(0, config_name.length() - 1);
                    configs.put(config_name, new CARSConfig(config_name, line));
                    Logs.debug("Config_name: " + config_name);
                    config_lines.add(config_name);
                }
                Logs.debug(String.format("[Config] %2d | %s", line_number, line));
            } else {
                is_config.add(false);
                config_lines.add(line);
            }
        }
        reader.close();
    }
    
    public void write_config() throws IOException {
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(config_path + ".test"));
        for (int i= 0; i < is_config.size(); i++) {
            if (is_config.get(i)) {
//              writer.write(configs.get(config_lines.get(i)).to_string());
                writer.write(configs.get(config_lines.get(i)).line);
            } else {
                writer.write(config_lines.get(i));
            }
            writer.write("\n");
       }
        writer.close();
    }
    
    public boolean change_config(String name, String conf, String value) {
        if (configs.containsKey(name)) {
            if (configs.get(name).config.containsKey(conf)) {
                Logs.debug(String.format("Relpaced config : %s.%s from %s to %s", name,
                        conf, configs.get(name).config.get(conf), value));
                configs.get(name).config.replace(conf, value);
                return true;
            } else {
                Logs.debug(String.format("Don't have conf in : %s.%s.%s", name, conf, value));
            }
        } else {
            Logs.debug(String.format("Don't have name in : %s.%s.%s", name, conf, value));
        }
        return false;
    }
}
