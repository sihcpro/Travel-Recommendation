package carskit.gui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import librec.util.Logs;

public class CARSResult {
	private String result_path;
	
	public CARSResult(String path) {
		result_path = path;
	}
	
	public static void main(String[] args) {
		CARSResult re = new CARSResult("./sampleData/CARSKit.Workspace/");
		try {
			re.read_result();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void read_result() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(result_path));
        String line;
        int line_number = 0;
        while ((line = reader.readLine()) != null) {
        	++line_number;
        	Logs.debug(String.format("%2d | %s", line_number, line));
        }
        reader.close();
	}
}
