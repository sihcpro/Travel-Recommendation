package carskit.gui;

import java.util.Hashtable;
import java.util.Map;

public class CARSConfig {
	public int line_number = 0;
	public String name = "";
	public String value = "";
	public String line = "";
	public Map<String, String> config = new Hashtable<String, String>();
	
	public CARSConfig(String config_name, String config_line) {
		name = config_name;
		line = config_line;
	}
	
	public String to_string() {
		String configs =  name + "=" + value;
		for(Map.Entry<String, String> detail : config.entrySet()) {
			configs += " -" + detail.getKey() + " " + detail.getValue();
		}
		return configs;
	}
}
