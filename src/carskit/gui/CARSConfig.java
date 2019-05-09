package carskit.gui;

import java.util.Hashtable;
import java.util.Map;

public class CARSConfig {
	public int line_number = 0;
	public String name;
	public Map<String, String> config = new Hashtable<String, String>();
	
	public CARSConfig(String config_name) {
		name = config_name;
	}
	
	public String to_string() {
		String configs = "";
		for(Map.Entry detail : config.entrySet()) {
			configs += "-" + (String)detail.getKey() + " " + (String)detail.getValue();
		}
		return name + "=" + configs;
	}
}
