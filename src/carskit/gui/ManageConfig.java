package carskit.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import librec.util.Logs;

public class ManageConfig {
    private static boolean debug = true;
    private static boolean show_detail = false;
    private String config_path;
    private Vector<String> config_lines = new Vector<String>();
    private Vector<Boolean> is_config = new Vector<Boolean>();
    private boolean checked_path_user;
    public Map<String, CARSConfig> configs = new Hashtable<String, CARSConfig>();
    

    public ManageConfig(String path) {
        config_path = path;
        checked_path_user = false;
//        if (!debug)
//            Logs.off();
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
    	if (checked_path_user)
    		return;
    	String separator=System.getProperty("file.separator");
    	String file_name = config_path.substring(config_path.lastIndexOf(separator) + 1, config_path.length());
    	if (file_name == "setting_user.conf") {
    		checked_path_user = true;
    	} else {
    		config_path = config_path.substring(0, config_path.lastIndexOf(separator) + 1) + "setting_user.conf";
    		if (debug)
            	Logs.debug("New config path: " + config_path);
    	}
    }

    Pattern re_name = Pattern.compile("[\\w.]*=");              
    public void read_config() throws IOException {
    	if (debug)
        	Logs.debug("Start read config at: " + config_path);
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
                    if (debug && show_detail)
                    	Logs.debug("Config_name: " + config_name);
                    config_lines.add(config_name);
                }
                if (debug && show_detail)
                	Logs.debug(String.format("[Config] %2d | %s", line_number, line));
            } else {
                is_config.add(false);
                config_lines.add(line);
            }
        }
        reader.close();
    }
    
    public void write_config() {
    	if (!checked_path_user)
    		check_path();
    	if (debug)
        	Logs.debug("Start write config to: " + config_path + "  size : " + is_config.size());
        BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(config_path));
	        for (int i= 0; i < is_config.size(); i++) {
	            if (is_config.get(i)) {
//                    writer.write(configs.get(config_lines.get(i)).to_string());
	                writer.write(configs.get(config_lines.get(i)).line);
	            } else {
	                writer.write(config_lines.get(i));
	            }
	            writer.write("\n");
	        }
            writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public boolean change_config(String name, String conf, String value) {
    	name = name.toLowerCase();
        if (configs.containsKey(name)) {
            if (configs.get(name).config.containsKey(conf)) {
            	if (debug)
                	Logs.debug(String.format("Relpaced config : %s: -%s from %s to %s", name,
                        conf, configs.get(name).config.get(conf), value));
                configs.get(name).change_config(conf, value);
                
                write_config();
                return true;
            } else {
            	if (debug) {
                	Logs.debug(String.format("Don't have conf in : %s: -%s %s", name, conf, value));
            	}
            }
        } else {
        	if (debug) {
            	Logs.debug(String.format("Don't have name in : %s: -%s %s", name, conf, value));
            	String keys = "";
            	for(Entry<String, CARSConfig> en: configs.entrySet()) {
            		keys += en.getKey() + " ";
            	}
            	Logs.debug("Keys : " + keys);
        	}
        }
        return false;
    }
    
    public boolean change_config_data_path(String data_path_old, String data_path_new) {
    	// Lenh nay xu li cho duong dan tren windows
    	if (System.getProperty("file.separator").compareTo("\\") == 0) {
    		data_path_old = data_path_old.replace("\\", "\\\\");
    		data_path_new = data_path_new.replace("\\", "\\\\");
    	}
    	if (data_path_old == data_path_new)
    		return true;
    	for (java.util.Map.Entry<String, CARSConfig> en : configs.entrySet()) {
    		if (en.getValue().value.compareTo(data_path_old) == 0) {
    			en.getValue().change_line(data_path_old, data_path_new);
    			write_config();
    			return true;
    		}
    	}
    	return false;
    }
    
    public String get_path() {
    	return config_path;
    }
}
