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
        data_path_old = CARS.normallize_path(data_path_old, true);
        data_path_new = CARS.normallize_path(data_path_new, true);
        Logs.debug(data_path_new);
        Logs.debug(data_path_old);
        if (data_path_old == data_path_new)
            return true;
        for (java.util.Map.Entry<String, CARSConfig> en : configs.entrySet()) {
            Logs.debug("config: {}", en.getValue());
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
    
    public static String default_config() {
    	return "################################################### Essential Setup #############################################\n" + 
    			"# dataset: contextual rating data, or raw rating\n" + 
    			"# dataset.ratings.wins=D:\\\\Sihc\\\\Travel-Recommendation\\\\sampleData\\\\user-history.csv\n" + 
    			"dataset.ratings.wins=D:\\\\Sihc\\\\Travel-Recommendation\\\\sampleData\\\\train_compact.csv\n" + 
    			"# dataset.ratings.lins=/Users/sihc/eclipse-workspace/CARSKit-master/sampleData/user-history.csv\n" + 
    			"dataset.ratings.lins=/Users/sihc/eclipse-workspace/CARSKit-master/sampleData/train_compact.csv\n" + 
    			"\n" + 
    			"dataset.social.wins=-1\n" + 
    			"dataset.social.lins=-1\n" + 
    			"\n" + 
    			"# options: -columns: (user, item, [rating, [timestamp]]) columns of rating data; -threshold: to binary ratings;\n" + 
    			"# --time-unit [DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, NANOSECONDS, SECONDS]\n" + 
    			"# if there is already a binary rating data under folder \"CARSKit.Workspace\" and you do not need data transformation, set negative value to -datatransformation; otherwise, set it as any positive value, e.g., 1\n" + 
    			"ratings.setup= -threshold -1 -datatransformation 1 -fullstat -1\n" + 
    			"\n" + 
    			"\n" + 
    			"# baseline-Avg recommender: GlobalAvg, UserAvg, ItemAvg, UserItemAvg\n" + 
    			"# baseline-Context average recommender: ContextAvg, ItemContextAvg, UserContextAvg\n" + 
    			"# baseline-CF recommender: ItemKNN, UserKNN, SlopeOne, PMF, BPMF, BiasedMF, NMF, SVD++\n" + 
    			"# baseline-Top-N ranking recommender: SLIM, BPR, RankALS, RankSGD, LRMF\n" + 
    			"# CARS - splitting approaches: UserSplitting, ItemSplitting, UISplitting; algorithm options: e.g., usersplitting -traditional biasedmf -minlenu 2 -minleni 2\n" + 
    			"# CARS - filtering approaches: SPF, DCR, DCW\n" + 
    			"# CARS - independent models: CPTF\n" + 
    			"# CARS - dependent-dev models: CAMF_CI, CAMF_CU, CAMF_C, CAMF_CUCI, CSLIM_C, CSLIM_CI, CSLIM_CU, CSLIM_CUCI, GCSLIM_CC\n" + 
    			"# CARS - dependent-sim models: CAMF_ICS, CAMF_LCS, CAMF_MCS, CSLIM_ICS, CSLIM_LCS, CSLIM_MCS, GCSLIM_ICS, GCSLIM_LCS, GCSLIM_MCS\n" + 
    			"# Notes: SLIM based models and dependent-sim models are top-N recommendation models which can be examined by top-N recommendations only.\n" + 
    			"\n" + 
    			"# recommender=usersplitting -traditional biasedmf -minlenu 2 -minleni 2\n" + 
    			"\n" + 
    			"recommender=camf_cu\n" + 
    			"\n" + 
    			"# main option: 1. test-set -f test-file-path; 2. cv (cross validation) -k k-folds [-p on, off]\n" + 
    			"# 3. leave-one-out; 4. given-ratio -r ratio;\n" + 
    			"# other options:  [--rand-seed n] [--test-view all] [--early-stop loss, MAE, RMSE]\n" + 
    			"# evaluation.setup=cv -k 5 -p on --rand-seed 1 --test-view all --early-stop RMSE\n" + 
    			"# evaluation.setup=given-ratio -r 0.8 -target r --test-view all --rand-seed 1\n" + 
    			"# main option: is ranking prediction\n" + 
    			"# other options: -ignore NumOfPopularItems\n" + 
    			"\n" + 
    			"evaluation.setup=cv -k 5 -p on --rand-seed 1 --test-view all\n" + 
    			"item.ranking=on -topN 10\n" + 
    			"\n" + 
    			"# main option: is writing out recommendation results; [--fold-data --measures-only --save-model]\n" + 
    			"output.setup= -folder CARSKit.Workspace -verbose on, off --to-file results_all_2016.txt\n" + 
    			"\n" + 
    			"# Guava cache configuration\n" + 
    			"guava.cache.spec=maximumSize=200,expireAfterAccess=2m\n" + 
    			"\n" + 
    			"################################################### Model-based Methods ##########################################\n" + 
    			"num.factors=10\n" + 
    			"num.max.iter=100\n" + 
    			"\n" + 
    			"\n" + 
    			"# options: -bold-driver, -decay ratio, -moment value\n" + 
    			"learn.rate=2e-2 -max -1 -bold-driver\n" + 
    			"\n" + 
    			"reg.lambda=0.0001 -c 0.001\n" + 
    			"#reg.lambda=10 -u 0.001 -i 0.001 -b 0.001 -s 0.001 -c 0.001\n" + 
    			"# probabilistic graphic models\n" + 
    			"pgm.setup= -alpha 2 -beta 0.5 -burn-in 300 -sample-lag 10 -interval 100\n" + 
    			"\n" + 
    			"################################################### Memory-based Methods #########################################\n" + 
    			"# similarity method: PCC, COS, COS-Binary, MSD, CPC, exJaccard; -1 to disable shrinking;\n" + 
    			"similarity=pcc\n" + 
    			"num.shrinkage=-1\n" + 
    			"\n" + 
    			"# neighborhood size; -1 to use as many as possible.\n" + 
    			"num.neighbors=10\n" + 
    			"\n" + 
    			"################################################### Method-specific Settings #######################################\n" + 
    			"\n" + 
    			"AoBPR= -lambda 0.3\n" + 
    			"BUCM= -gamma 0.5\n" + 
    			"BHfree= -k 10 -l 10 -gamma 0.2 -sigma 0.01\n" + 
    			"FISM= -rho 100 -alpha 0.4\n" + 
    			"Hybrid= -lambda 0.5\n" + 
    			"LDCC= -ku 20 -kv 19 -au 1 -av 1 -beta 1\n" + 
    			"PD= -sigma 2.5\n" + 
    			"PRankD= -alpha 20\n" + 
    			"RankALS= -sw on\n" + 
    			"RSTE= -alpha 0.4\n" + 
    			"DCR= -wt 0.9 -wd 0.4 -p 5 -lp 2.05 -lg 2.05\n" + 
    			"DCW= -wt 0.9 -wd 0.4 -p 5 -lp 2.05 -lg 2.05 -th 0.8\n" + 
    			"SPF= -i 0 -b 5 -th 0.9 -f 10 -t 100 -l 0.02 -r 0.001\n" + 
    			"SLIM= -l1 1 -l2 1 -k 1\n" + 
    			"CAMF_LCS= -f 10\n" + 
    			"CSLIM_C= -lw1 1 -lw2 5 -lc1 1 -lc2 5 -k 3 -als 0\n" + 
    			"CSLIM_CI= -lw1 1 -lw2 5 -lc1 1 -lc2 1 -k 1 -als 0\n" + 
    			"CSLIM_CU= -lw1 1 -lw2 0 -lc1 1 -lc2 5 -k 10 -als 0\n" + 
    			"CSLIM_CUCI= -lw1 1 -lw2 5 -lc1 1 -lc2 5 10 -1 -als 0\n" + 
    			"GCSLIM_CC= -lw1 1 -lw2 5 -lc1 1 -lc2 5 -k -1 -als 0\n" + 
    			"CSLIM_ICS= -lw1 1 -lw2 5 -k 1 -als 0\n" + 
    			"CSLIM_LCS= -lw1 1 -lw2 5 -k 1 -als 0\n" + 
    			"CSLIM_MCS= -lw1 -20000 -lw2 100 -k 3 -als 0\n" + 
    			"GCSLIM_ICS= -lw1 1 -lw2 5 -k 10 -als 0\n" + 
    			"GCSLIM_LCS= -lw1 1 -lw2 5 -k -1 -als 0\n" + 
    			"GCSLIM_MCS= -lw1 1 -lw2 5 -k -1 -als 0\n" + 
    			"FM= -lw 0.01 -lf 0.02\n";
    }
}
