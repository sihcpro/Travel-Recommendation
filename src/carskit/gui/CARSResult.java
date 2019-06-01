package carskit.gui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import librec.util.Logs;

public class CARSResult {
    private String result_path;
    private String COMMA_DELIMITER = ",";
    public List<List<Double>> all_value_results;
    public List<String> all_algo_names;
    public Map<String, List<Double>> map_result;
    
    public CARSResult(String path) {
        result_path = path;
    }
    
    public CARSResult(List<String> algos, List<List<Double>> values) {
        all_algo_names = algos;
        all_value_results = values;
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
        all_value_results = new ArrayList<>();
        all_algo_names = new ArrayList<String>();
        Logs.debug("all results path: " + result_path);
        try (BufferedReader br = new BufferedReader(new FileReader(result_path))) {
            int line_number = 0;
            String line;
            while ((line = br.readLine()) != null) {
                line_number++;
                if (line_number == 1) {
                    
                } else {
                    List<String> values = Arrays.asList(line.split(COMMA_DELIMITER));
                    List<Double> value_result = new ArrayList<Double>();
                    Double test_nan = Double.parseDouble(values.get(1));
                    if (test_nan.isNaN())
                        continue;
                    for (int i = 1; i < values.size(); i++) {
                        value_result.add(Double.parseDouble(values.get(i)));
                    }
                    all_value_results.add(value_result);
                    all_algo_names.add(values.get(0));
//                  Logs.debug("result size: " + all_value_results.size());               
                }
            }
        }
    }
    
    public void make_map() {
        map_result = new HashMap<>();
        for (int i = 0; i < all_algo_names.size(); i++) {
            String algo_name = all_algo_names.get(i);
            List<Double> algo_value = all_value_results.get(i);
            if (map_result.containsKey(algo_name)) {
                if (greater(map_result.get(algo_name), algo_value)) {
                    map_result.replace(algo_name, algo_value);
                }
            } else {
                map_result.put(algo_name, algo_value);
            }
        }
    }
    
    public boolean greater(List<Double> fi, List<Double> se) {
        Logs.debug("greater");
        return sum(fi) > sum(se);
    }
    
    public Double sum(List<Double> list) {
        Double result = (double) 0;
        for (Double i:list) {
            result += i;
        }
        return result;
    }
}
