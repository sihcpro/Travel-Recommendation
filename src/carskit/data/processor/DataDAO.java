// Copyright (C) 2015 Yong Zheng
//
// This file is part of CARSKit.
//
// CARSKit is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// CARSKit is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with CARSKit. If not, see <http://www.gnu.org/licenses/>.
//

package carskit.data.processor;


import carskit.main.CARSKit;
import com.google.common.primitives.Doubles;
import happy.coding.io.FileIO;
import happy.coding.io.Lists;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.math.Stats;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;

import carskit.data.structure.SparseMatrix;
import librec.data.SparseTensor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import librec.data.SparseVector;
import org.apache.commons.math3.analysis.function.Max;
import org.apache.commons.math3.stat.inference.TTest;

/**
 * A data access object (DAO) to a data file
 *
 * @author Yong Zheng
 *
 */
public class DataDAO {

    // name of data file
    private String dataName;
    // directory of data file
    private String dataDir;
    // path to data file
    private String dataPath;
    // store rate data as {UserItem, Ctx, rate} matrix
    private SparseMatrix rateMatrix;
    // store rate data as a sparse tensor
    private SparseTensor rateTensor;

    private double MaxRate=-1, MinRate=-1;

    // is first head line
    private boolean isHeadline = true;

    // variables for full data statistics
    private boolean fullStat = false;
    private SparseMatrix rateMatrix_UI;
    private SparseMatrix rateMatrix_UC;
    private SparseMatrix rateMatrix_IC;
    private double density_unique_ui;
    private double density_unique_uc;
    private double density_unique_ic;
    private HashMap<Integer, Double> rates_c;
    private HashMap<Integer, Double> rates_c_count;
    private HashMap<Integer, Double> rates_u_count;
    private HashMap<Integer, Double> rates_i_count;
    private Collection<Double> ratingDist_ui;
    private Collection<Double> ratingDist_uc;
    private Collection<Double> ratingDist_ic;


    // data scales
    private List<Double> ratingScale;
    // scale distribution
    private Multiset<Double> scaleDist;

    // number of rates
    private int numRatings;

    // user/item {raw id, inner id} map
    private BiMap<String, Integer> userIds, itemIds, ctxIds, uiIds, dimIds, condIds;

    // inverse views of userIds, itemIds, contextidmensionIds, contextconditionIds, UIIds (i.e.,  <user, item> ids)
    private BiMap<Integer, String> idUsers, idItems, idCtx, idUIs, idDims, idConds;

    // u--> ui1, ui3, ui4,,,; i--> u1i,u3i,u4i,...
    private Multimap<Integer, Integer> uRatedList, iRatedList, dimConditionsList, condContextsList;

    private HashMap<Integer, Integer> uiUserIds, uiItemIds, condDimensionMap;
    private HashMap<Integer,  ArrayList<Integer>> dimensionConditionsList, contextConditionsList;

    private ArrayList<Integer> EmptyContextConditions;


    /**
     * Constructor for a data DAO object
     *
     * @param path
     *            path to data file
     */
    public DataDAO(String path, BiMap<String, Integer> userIds, BiMap<String, Integer>itemIds, BiMap<String, Integer> ctxIds, BiMap<String, Integer> uiIds,
                   BiMap<String, Integer> dimIds, BiMap<String, Integer> condIds, Multimap<Integer, Integer> uRatedList, Multimap<Integer, Integer> iRatedList,
                   Multimap<Integer, Integer> dimConditionsList, HashMap<Integer, Integer> condDimensionMap, Multimap<Integer, Integer> condContextsList,
                   HashMap<Integer,  ArrayList<Integer>> contextConditionsList, HashMap<Integer, Integer> uiUserIds, HashMap<Integer, Integer>  uiItemIds) {
        dataPath = path;

        this.userIds = (userIds==null) ? HashBiMap.create() : (HashBiMap)userIds;
        this.itemIds = (itemIds==null) ? HashBiMap.create() : (HashBiMap)itemIds;
        this.ctxIds = (ctxIds==null) ? HashBiMap.create() : (HashBiMap)ctxIds;
        this.uiIds = (uiIds==null) ? HashBiMap.create() : (HashBiMap)uiIds;
        this.dimIds = (dimIds==null) ? HashBiMap.create() : (HashBiMap)dimIds;
        this.condIds = (condIds==null) ? HashBiMap.create() : (HashBiMap)condIds;

        this.uRatedList = (uRatedList==null) ? HashMultimap.create() : (HashMultimap)uRatedList;
        this.iRatedList = (iRatedList==null) ? HashMultimap.create() : (HashMultimap)iRatedList;
        this.dimConditionsList = (dimConditionsList==null) ? HashMultimap.create() : (HashMultimap)dimConditionsList;
        this.condContextsList = (condContextsList==null) ? HashMultimap.create() : (HashMultimap)condContextsList;
        this.contextConditionsList = (contextConditionsList==null) ? new HashMap<Integer,  ArrayList<Integer>>() : contextConditionsList;

        this.uiUserIds = (uiUserIds==null)?new HashMap<>():(HashMap)uiUserIds;
        this.uiItemIds = (uiItemIds==null)?new HashMap<>():(HashMap)uiItemIds;
        this.condDimensionMap = (condDimensionMap==null)?new HashMap<>():(HashMap)condDimensionMap;

        scaleDist = HashMultiset.create();
    }

    /**
     * Contructor for data DAO object
     *
     * @param path
     *            path to data file
     */
    public DataDAO(String path) {
        this(path, null, null, null, null, null, null, null, null, null, null,null,null,null,null);
    }


    /**
     * Read data from the data file. Note that we didn't take care of the duplicated lines.
     *

     * @param binThold
     *            the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     *            otherwise 0. To disable this feature, i.e., keep the original rating value, set the threshold a
     *            negative value
     * @return a sparse matrix storing all the relevant data
     */
    public SparseMatrix readData(double binThold) throws Exception {

        if (CARSKit.isMeasuresOnly)
            Logs.debug(String.format("Dataset: %s", Strings.last(dataPath, 38)));
        else
            Logs.info(String.format("Dataset: %s", Strings.last(dataPath, 38)));

        // Table {row-id, col-id, rate}
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        // Map {col-id, multiple row-id}: used to fast build a rating matrix
        Multimap<Integer, Integer> colMap = HashMultimap.create();


        Table<Integer, Integer, Double> dataTable_ui=null, dataTable_uc=null, dataTable_ic=null, dataTable_ui_counter=null, dataTable_uc_counter=null, dataTable_ic_counter=null;
        Multimap<Integer, Integer> colMap_ui=null, colMap_uc=null, colMap_ic=null;

        if(fullStat){
            dataTable_ui=HashBasedTable.create();
            dataTable_uc=HashBasedTable.create();
            dataTable_ic=HashBasedTable.create();
            dataTable_ui_counter=HashBasedTable.create();
            dataTable_uc_counter=HashBasedTable.create();
            dataTable_ic_counter=HashBasedTable.create();

            colMap_ui = HashMultimap.create();
            colMap_uc = HashMultimap.create();
            colMap_ic = HashMultimap.create();
        }


        EmptyContextConditions=new ArrayList<>();
        Logs.info("DataPath: "+dataPath);
        BufferedReader br = FileIO.getReader(dataPath);
        String line = br.readLine(); // 1st line is header in shape of: user, item, rating, dim1:c1, dim1:c2, ....
        String[] data = line.trim().split("[\t,]+");
        // indexing context dimensions and ctx
        for(int i=3;i<data.length;++i){
            String context=data[i].trim();
            String[] cs=context.split(":");
            String dim=cs[0].trim();
            int dimc = dimIds.containsKey(dim) ? dimIds.get(dim) : dimIds.size(); // hash dimension Ids, from 0 to N
            dimIds.put(dim,dimc);
            condIds.put(context,i-3);
            dimConditionsList.put(dimc,i-3);
            condDimensionMap.put(i-3,dimc); // key = condId, value = dimId

            // record which conditions are the empty contexts, i.e., the condition value = NA
            if(context.endsWith(":na"))
                EmptyContextConditions.add(i-3);
        }

        rates_c = new HashMap<>();
        rates_c_count = new HashMap<>();
        rates_u_count = new HashMap<>();
        rates_i_count = new HashMap<>();

        while ((line = br.readLine()) != null) {
            //data = line.trim().split("[\t,]+");
            data = line.trim().split(",",-1);

            String user = data[0];
            String item = data[1];
            Double rate = Double.valueOf(data[2]);

            // binarize the rating for item recommendation task
            //if (binThold >= 0)
            //rate = rate > binThold ? 1.0 : 0.0;

            scaleDist.add(rate);

            // inner id starting from 0
            int row = userIds.containsKey(user) ? userIds.get(user) : userIds.size();
            userIds.put(user, row);

            int col = itemIds.containsKey(item) ? itemIds.get(item) : itemIds.size();
            itemIds.put(item, col);

            // create UI matrix
            if(fullStat){
                if(dataTable_ui.contains(row,col)){
                    dataTable_ui.put(row,col,rate+dataTable_ui.get(row,col));
                    dataTable_ui_counter.put(row,col,1+dataTable_ui_counter.get(row,col));
                }else{
                    dataTable_ui.put(row,col,rate);
                    dataTable_ui_counter.put(row,col,1.0);
                    colMap_ui.put(col,row);
                }

                if(rates_u_count.containsKey(row))
                    rates_u_count.put(row, 1.0+rates_u_count.get(row));
                else
                    rates_u_count.put(row, 1.0);

                if(rates_i_count.containsKey(col))
                    rates_i_count.put(col, 1.0+rates_i_count.get(col));
                else
                    rates_i_count.put(col, 1.0);
            }

            // also, indexing (user,item); note: user inner id as key
            String useritem=row+","+col;
            int uic = uiIds.containsKey(useritem) ? uiIds.get(useritem) : uiIds.size();
            uiIds.put(useritem,uic);


            // add ui to uList and iList; multiple non-duplicate values will be added to a same key
            uRatedList.put(row,uic);
            iRatedList.put(col,uic);

            uiUserIds.put(uic,row);
            uiItemIds.put(uic,col);

            // indexing ctx; only record ID which is correlated with the header
            StringBuilder sb_ctx=new StringBuilder();
            ArrayList<Integer> condList=new ArrayList<>();
            for(int i=3;i<data.length;++i)
            {
                if(data[i].trim().equals(""))
                    Logs.error(line+"; "+data[i]);
                int value=Integer.valueOf(data[i].trim());
                if(value==1) {
                    if (sb_ctx.length() > 0) sb_ctx.append(",");
                    sb_ctx.append(i - 3);
                    condList.add(i-3);

                    if(fullStat && !EmptyContextConditions.contains(i-3)){
                        // create UC matrix
                        if(dataTable_uc.contains(row, i-3)){
                            dataTable_uc.put(row, i-3, rate+dataTable_uc.get(row, i-3));
                            dataTable_uc_counter.put(row, i-3, 1.0+dataTable_uc_counter.get(row, i-3));
                        }else
                        {
                            dataTable_uc.put(row, i-3, rate);
                            dataTable_uc_counter.put(row, i-3, 1.0);
                            colMap_uc.put(i-3, row);
                        }

                        // create IC matrix
                        if(dataTable_ic.contains(col, i-3)){
                            dataTable_ic.put(col, i-3, rate+dataTable_ic.get(col, i-3));
                            dataTable_ic_counter.put(col, i-3, 1.0+dataTable_ic_counter.get(col, i-3));
                        }else
                        {
                            dataTable_ic.put(col, i-3, rate);
                            dataTable_ic_counter.put(col, i-3, 1.0);
                            colMap_ic.put(i-3, col);
                        }

                        // collect ratings for each context condition
                        if(rates_c.containsKey(i-3)){
                            rates_c.put(i-3, rate+rates_c.get(i-3));
                            rates_c_count.put(i-3, 1.0+rates_c_count.get(i-3));
                        }else
                        {
                            rates_c.put(i-3, rate);
                            rates_c_count.put(i-3, 1.0);
                        }
                    }
                }




            }
            String ctx=sb_ctx.toString();
            // inner id starting from 0
            int cc=ctxIds.containsKey(ctx) ? ctxIds.get(ctx) : ctxIds.size();
            ctxIds.put(ctx, cc);
            contextConditionsList.put(cc, condList);
            for(Integer cond:condList) {
                //contextConditionsList.put(cc, cond);
                this.condContextsList.put(cond, cc);
            }

            //System.out.println(useritem+"; "+ctx+"; "+rate);

            dataTable.put(uic, cc, rate); // useritem, ctx, rating
            colMap.put(cc, uic);

        }
        br.close();

        numRatings = scaleDist.size();
        ratingScale = new ArrayList<>(scaleDist.elementSet());
        Collections.sort(ratingScale);


        // build rating matrix
        rateMatrix = new SparseMatrix(numUserItems(), numContexts(), dataTable, colMap);

        // build other matrices
        if(fullStat){
            density_unique_ui = 0;
            density_unique_uc = 0;
            density_unique_ic = 0;


            // UI Matrix
            for(int row:dataTable_ui.rowKeySet())
                for(int col:dataTable_ui.columnKeySet()){
                    if(dataTable_ui.contains(row, col)) {
                        double counter = dataTable_ui_counter.get(row, col);
                        if(counter>1) density_unique_ui+=1.0;
                        dataTable_ui.put(row, col, dataTable_ui.get(row, col) / counter);
                    }
                }
            rateMatrix_UI = new SparseMatrix(numUsers(), numItems(), dataTable_ui, colMap_ui);

            // UC matrix
            for(int row:dataTable_uc.rowKeySet())
                for(int col:dataTable_uc.columnKeySet()){
                    if(dataTable_uc.contains(row, col)) {
                        double counter = dataTable_uc_counter.get(row, col);
                        if(counter>1) density_unique_uc+=1.0;
                        dataTable_uc.put(row, col, dataTable_uc.get(row, col) / counter);
                    }
                }
            rateMatrix_UC = new SparseMatrix(numUsers(), numConditions(), dataTable_uc, colMap_uc);

            // IC matrix
            for(int row:dataTable_ic.rowKeySet())
                for(int col:dataTable_ic.columnKeySet()){
                    if(dataTable_ic.contains(row, col)) {
                        double counter = dataTable_ic_counter.get(row, col);
                        if (counter > 1) density_unique_ic += 1.0;
                        dataTable_ic.put(row, col, dataTable_ic.get(row, col) / counter);
                    }
                }
            rateMatrix_IC = new SparseMatrix(numItems(), numConditions(), dataTable_ic, colMap_ic);

            ratingDist_ui=dataTable_ui_counter.values();
            ratingDist_uc=dataTable_uc_counter.values();
            ratingDist_ic=dataTable_ic_counter.values();
        }

        // release memory of data table
        dataTable = null;
        dataTable_ui = null;
        dataTable_ui_counter=null;
        dataTable_uc = null;
        dataTable_uc_counter = null;
        dataTable_ic = null;
        dataTable_ic_counter = null;

        Logs.info("Rating data set has been successfully loaded.");
        return rateMatrix;
    }

    public HashMap<Integer, ArrayList<Integer>> getDimensionConditionsList()
    {
        return this.dimensionConditionsList;
    }

    /**
     * Convert loaded SparseMatrix to SparseTensor
     */

    public SparseTensor toSparseTensor(SparseMatrix sm)
    {
        int numDims=2+this.numContextDims();
        int[] dims = new int[numDims];
        List<Integer>[] ndLists = (List<Integer>[]) new List<?>[numDims];
        Set<Integer>[] ndSets = (Set<Integer>[]) new Set<?>[numDims];
        List<Double> vals = new ArrayList<Double>();
        for (int d = 0; d < numDims; d++) {
            ndLists[d] = new ArrayList<Integer>();
            ndSets[d] = new HashSet<Integer>();
        }

        dimensionConditionsList=new HashMap<>();

        for(librec.data.MatrixEntry me:sm){
            int rowid=me.row();
            int ctxid=me.column();
            double rate=me.get();
            int uid=getUserIdFromUI(rowid);
            int iid=getItemIdFromUI(rowid);


            vals.add(rate);
            ndLists[0].add(uid); // user dimension
            ndSets[0].add(uid);
            ndLists[1].add(iid); // item dimension
            ndSets[1].add(iid);

            // start recording context dimensions
            Collection<Integer> listOfConditions=contextConditionsList.get(ctxid);
            for(int condId: listOfConditions){
                int dimId = condDimensionMap.get(condId);
                int index_condId=-1;
                if(dimensionConditionsList.containsKey(dimId)){
                    ArrayList<Integer> list=dimensionConditionsList.get(dimId);
                    index_condId=list.indexOf(condId);
                    if(index_condId==-1)
                        list.add(condId);
                    index_condId=list.size()-1;
                    dimensionConditionsList.put(dimId, list);
                }else
                {
                    ArrayList<Integer> list=new  ArrayList<>();
                    list.add(condId);
                    dimensionConditionsList.put(dimId,list);
                    index_condId=0;
                }
                ndLists[2+dimId].add(index_condId); // since user and item dimensions are the first two dimensions
                ndSets[2+dimId].add(index_condId);
            }
        }
        for (int d = 0; d < numDims; d++) {
            dims[d] = ndSets[d].size();
        }
        SparseTensor st = new SparseTensor(dims, ndLists, vals);
        st.setUserDimension(0);
        st.setItemDimension(1);
        return st;
    }

    /**
     * Load data as SparseTensor
     */
    public void LoadAsTensor()
    {
        rateTensor = this.toSparseTensor(rateMatrix);
    }

    /**
     * write the rate data to another data file given by the path {@code toPath}
     *
     * @param toPath
     *            the data file to write to
     * @param sep
     *            the sparator of the written data file

    public void writeData(String toPath, String sep) throws Exception {
    FileIO.deleteFile(toPath);

    List<String> lines = new ArrayList<>(1500);
    for (MatrixEntry me : rateMatrix) {
    String line = Strings.toString(new Object[] { me.row() + 1, me.column() + 1, (float) me.get() }, sep);
    lines.add(line);

    if (lines.size() >= 1000) {
    FileIO.writeList(toPath, lines, null, true);
    lines.clear();
    }
    }

    if (lines.size() > 0)
    FileIO.writeList(toPath, lines, null, true);

    Logs.debug("Data has been exported to {}", toPath);
    } */

    /**
     * Default sep=" " is adopted

     public void writeData(String toPath) throws Exception {
     writeData(toPath, " ");
     } */

    public void setFullStat(boolean full){
        this.fullStat = full;
    }


    /**
     * print out specifications of the dataset
     */
    public void printSpecs() throws Exception {
        if (rateMatrix == null)
            readData(-1);

        List<String> sps = new ArrayList<>();

        int users = numUsers();
        int items = numItems();

        int numRates = rateMatrix.size();
        int dims = numContextDims();
        int conds=numConditions();
        int numctx=numContexts();
        int cdims = 1;

        StringBuilder condcount=new StringBuilder();
        StringBuilder sdims=new StringBuilder();
        for (int dim:dimConditionsList.keySet()){
            String sdim=this.getContextDimensionId(dim);
            int counter=dimConditionsList.get(dim).size();
            if(condcount.length()>0) condcount.append(", ");
            condcount.append(sdim+": "+counter);
            cdims*=counter;
            if(sdims.length()>0) sdims.append(", ");
            sdims.append(sdim);
        }


        sps.add(String.format("Dataset: %s", dataPath));
        sps.add("");
        sps.add("Statistics of U-I-C Matrix:");
        sps.add("User amount: " + users);
        sps.add("Item amount: " + items);
        sps.add("Rate amount: " + numRatings());
        sps.add("Context dimensions: " + dims +" ("+sdims.toString()+")");
        sps.add("Context conditions: " + conds + " ("+condcount.toString()+")");
        sps.add("Context situations: " + numctx);
        sps.add(String.format("Data density: %.4f%%", (numRates + 0.0) / users / items / cdims * 100));
        sps.add("Scale distribution: " + scaleDist.toString());

        // user/item mean
        double[] data = rateMatrix.getData();
        float mean = (float) (Stats.sum(data) / numRates);
        float std = (float) Stats.sd(data);
        float mode = (float) Stats.mode(data);
        float median = (float) Stats.median(data);
        sps.add(String.format("Average value of all ratings: %f", mean));
        sps.add(String.format("Standard deviation of all ratings: %f", std));
        sps.add(String.format("Mode of all rating values: %f", mode));
        sps.add(String.format("Median of all rating values: %f", median));

        if(fullStat){

            sps.add("");
            Collection<Double> ratingDist_c=rates_c_count.values();
            Collection<Double> ratingDist_u=rates_u_count.values();
            Collection<Double> ratingDist_i=rates_i_count.values();
            sps.add("Distribution of rate counts per user: mean = "+Stats.mean(ratingDist_u)+", median = "+Stats.median(ratingDist_u)+", sd = "+Stats.sd(ratingDist_u));
            sps.add("Distribution of rate counts per item: mean = "+Stats.mean(ratingDist_i)+", median = "+Stats.median(ratingDist_i)+", sd = "+Stats.sd(ratingDist_i));
            sps.add("Distribution of rate counts per context condition: mean = "+Stats.mean(ratingDist_c)+", median = "+Stats.median(ratingDist_c)+", sd = "+Stats.sd(ratingDist_c));
            sps.add("");
            sps.add("Average rating in each context condition: (Average, Counts)");
            Lists.sortMap(rates_c,false);
            for(int c:rates_c.keySet()){
                sps.add(this.getContextConditionId(c)+" - "+String.format("%.6f", rates_c.get(c)/rates_c_count.get(c)) + ", "+rates_c_count.get(c).intValue());
            }

            data = rateMatrix_UI.getData();
            double numRates_M = data.length;
            sps.add("");
            sps.add("Statistics of UI Matrix:");
            sps.add("User amount: " + users);
            sps.add("Item amount: " + items);
            sps.add("Rate amount: " + numRates_M);
            sps.add(String.format("Data density: %.4f%%", (numRates_M + 0.0) / users / items * 100));
            sps.add(String.format("Data density (unique pairs): %.4f%%", density_unique_ui / numRates_M * 100));
            mean = (float) (Stats.sum(data) / numRates_M);
            std = (float) Stats.sd(data);
            mode = (float) Stats.mode(data);
            median = (float) Stats.median(data);
            sps.add(String.format("Average value of all ratings: %f", mean));
            sps.add(String.format("Standard deviation of all ratings: %f", std));
            sps.add(String.format("Mode of all rating values: %f", mode));
            sps.add(String.format("Median of all rating values: %f", median));
            sps.add("Distribution of rate counts per UI pair: mean = "+Stats.mean(ratingDist_ui)+", median = "+Stats.median(ratingDist_ui)+", sd = "+Stats.sd(ratingDist_ui));


            data = rateMatrix_UC.getData();
            numRates_M = data.length;
            sps.add("");
            sps.add("Statistics of UC Matrix:");
            sps.add("User amount: " + users);
            sps.add("Condition amount: " + conds);
            sps.add("Rate amount: " + numRates_M);
            sps.add(String.format("Data density: %.4f%%", (numRates_M + 0.0) / users / numConditions() * 100));
            sps.add(String.format("Data density (unique pairs): %.4f%%", density_unique_uc / numRates_M * 100));
            mean = (float) (Stats.sum(data) / numRates_M);
            std = (float) Stats.sd(data);
            mode = (float) Stats.mode(data);
            median = (float) Stats.median(data);
            sps.add(String.format("Average value of all ratings: %f", mean));
            sps.add(String.format("Standard deviation of all ratings: %f", std));
            sps.add(String.format("Mode of all rating values: %f", mode));
            sps.add(String.format("Median of all rating values: %f", median));
            sps.add("Distribution of rate counts per UC pair: mean = "+Stats.mean(ratingDist_uc)+", median = "+Stats.median(ratingDist_uc)+", sd = "+Stats.sd(ratingDist_uc));

            data = rateMatrix_IC.getData();
            numRates_M = data.length;
            sps.add("");
            sps.add("Statistics of IC Matrix:");
            sps.add("Item amount: " + items);
            sps.add("Condition amount: " + conds);
            sps.add("Rate amount: " + numRates_M);
            sps.add(String.format("Data density: %.4f%%", (numRates_M + 0.0) / items / numConditions() * 100));
            sps.add(String.format("Data density (unique pairs): %.4f%%", density_unique_ic / numRates_M * 100));
            mean = (float) (Stats.sum(data) / numRates_M);
            std = (float) Stats.sd(data);
            mode = (float) Stats.mode(data);
            median = (float) Stats.median(data);
            sps.add(String.format("Average value of all ratings: %f", mean));
            sps.add(String.format("Standard deviation of all ratings: %f", std));
            sps.add(String.format("Mode of all rating values: %f", mode));
            sps.add(String.format("Median of all rating values: %f", median));
            sps.add("Distribution of rate counts per IC pair: mean = "+Stats.mean(ratingDist_ic)+", median = "+Stats.median(ratingDist_ic)+", sd = "+Stats.sd(ratingDist_ic));

            sps.add("");
            ArrayList<Double> urates = new ArrayList<>();
            ArrayList<Double> urates_c = new ArrayList<>();
            ArrayList<Double> irates = new ArrayList<>();
            ArrayList<Double> irates_c = new ArrayList<>();

            for(int u:rateMatrix_UI.rows())
            {
                if(rateMatrix_UC.rows().contains(u)){
                    SparseVector sv=rateMatrix_UI.row(u);
                    SparseVector svc=rateMatrix_UC.row(u);
                    if(sv.size()!=0 && svc.size()!=0){
                        urates.add(sv.mean());
                        urates_c.add(svc.mean());
                    }
                }
            }

            for(int i:rateMatrix_UI.columns())
            {
                if(rateMatrix_IC.rows().contains(i)){
                    SparseVector sv=rateMatrix_UI.column(i);
                    SparseVector svc=rateMatrix_IC.row(i);
                    if(sv.size()!=0 && svc.size()!=0){
                        irates.add(sv.mean());
                        irates_c.add(svc.mean());
                    }
                }
            }

            TTest tt = new TTest();
            sps.add("Paired t-test on user's average rating between UI and UC matrix: absolute mean diff = "+Math.abs(Stats.mean(urates)-Stats.mean(urates_c))+", p-value = "+tt.pairedTTest(Doubles.toArray(urates), Doubles.toArray(urates_c)));
            sps.add("Paired t-test on item's average rating between UI and IC matrix: absolute mean diff = "+Math.abs(Stats.mean(irates)-Stats.mean(irates_c))+", p-value = "+tt.pairedTTest(Doubles.toArray(irates), Doubles.toArray(irates_c)));
        }

        Logs.info(Strings.toSection(sps));
    }


    /**
     * @return number of User-item
     */
    public int numUserItems() {
        return uiIds.size();
    }

    /**
     * @return number of ctx
     */
    public int numContexts() {
        return ctxIds.size();
    }

    /**
     * @return number of users
     */
    public int numUsers() {
        return userIds.size();
    }

    /**
     * @return number of items
     */
    public int numItems() {
        return itemIds.size();
    }

    /**
     * @return number of rates
     */
    public int numRatings() {
        return numRatings;
    }

    /**
     * @return number of context dimensions
     */
    public int numContextDims() {
        return dimIds.size();
    }

    /**
     * @return number of context conditions
     */
    public int numConditions() {
        return condIds.size();
    }

    /**
     * @param rawId
     *            raw user id as String
     * @return inner user id as int
     */
    public int getUserId(String rawId) {
        return userIds.get(rawId);
    }

    /**
     * @param innerId
     *            inner user id as int
     * @return raw user id as String
     */
    public String getUserId(int innerId) {

        if (idUsers == null)
            idUsers = userIds.inverse();

        return idUsers.get(innerId);
    }


    /**
     * @param rawId
     *            raw item id as String
     * @return inner item id as int
     */
    public int getItemId(String rawId) {
        return itemIds.get(rawId);
    }

    /**
     * @param innerId
     *            inner user id as int
     * @return raw item id as String
     */
    public String getItemId(int innerId) {

        if (idItems == null)
            idItems = itemIds.inverse();

        return idItems.get(innerId);
    }

    /**
     * @param rawId
     *            raw user-item id as String
     * @return inner user-item id as int
     */
    public int getUserItemId(String rawId) {
        if(uiIds.containsKey(rawId))
            return uiIds.get(rawId);
        else
            return -1;
    }

    /**
     * @param rawUserId
     *            raw user id as String
     * @param rawItemId
     *            raw item id as String
     * @return inner user-item id as int
     */
    public int getUserItemId(String rawUserId, String rawItemId) {

        int inn_uid=getUserId(rawUserId);
        int inn_iid=getItemId(rawItemId);
        return getUserItemId(inn_uid + "," + inn_iid);
    }

    /**
     * @param innerId
     *            inner user id
     * @return inner UserItem ID as a set
     */
    public Collection<Integer>  getUserRatedList(int innerId)
    {
        return uRatedList.get(innerId);
    }

    /**
     * @param innerId
     *            inner item id
     * @return inner UserItem ID as a set
     */
    public Collection<Integer> getItemRatedList(int innerId)
    {
        return iRatedList.get(innerId);
    }

    /**
     * @param innerId
     *            inner user-item id as int
     * @return raw user-item id as String
     */
    public String getUserItemId(int innerId) {

        if (idUIs == null)
            idUIs = uiIds.inverse();

        return idUIs.get(innerId);
    }

    /**
     * @param innerId
     *            inner condition id as int
     * @return inner dimension id as int
     */
    public int getDimensionByConditionId(int innerId){
        return this.condDimensionMap.get(innerId);
    }

    /**
     * @param innerId
     *            inner dimension id as int
     * @return List of inner condition ids as output
     */
    public Collection<Integer> getConditionByDimensionId(int innerId){
        return this.dimConditionsList.get(innerId);
    }


    /**
     * @param rawId
     *            raw dimension id as String
     * @return inner dimension id as int
     */
    public int getContextDimensionId(String rawId) {
        return dimIds.get(rawId);
    }

    /**
     * @param innerId
     *            inner dimension id as int
     * @return raw dimension id as String
     */
    public String getContextDimensionId(int innerId) {

        if (idDims == null)
            idDims = dimIds.inverse();

        return idDims.get(innerId);
    }

    /**
     * @param rawId
     *            raw condition id as String
     * @return inner condition id as int
     */
    public int getContextConditionId(String rawId) {
        return condIds.get(rawId);
    }

    /**
     * @param innerId
     *            inner condition id as int
     * @return raw condition id as String
     */
    public String getContextConditionId(int innerId) {

        if (idConds == null)
            idConds = condIds.inverse();

        return idConds.get(innerId);
    }

    /**
     * @param rawId
     *            raw context id as String
     * @return inner context id as int
     */
    public int getContextId(String rawId) {
        //System.out.println(rawId);
        int id;
        if(rawId.contains(":")){ // in shape of dim:c, dim:c
            String[] ccs=rawId.toLowerCase().split(",");
            SortedSet<Integer> set=new TreeSet<Integer>();
            for(int i=0;i<ccs.length;++i)
                set.add(this.getContextConditionId(ccs[i].trim()));
            StringBuilder sb=new StringBuilder();
            Iterator<Integer> itor=set.iterator();
            while(itor.hasNext())
            {
                if(sb.length()>0) sb.append(",");
                sb.append(itor.next());
            }
            return getContextId(sb.toString());
        }else // in shape of 1,2,3,
            id=ctxIds.get(rawId);
        return id;
    }

    /**
     * @param innerId
     *            inner context id as int
     * @return raw context id as String
     */
    public String getContextId(int innerId) {

        if (idCtx == null)
            idCtx = ctxIds.inverse();

        return idCtx.get(innerId);
    }

    public String getContextSituationFromInnerId(int innerId){
        String id=this.getContextId(innerId);
        String[] ids=id.split(",");
        StringBuilder rawcontext=new StringBuilder();
        for(String index:ids){
            if(rawcontext.length()>0) rawcontext.append(";");
            rawcontext.append(this.getContextConditionId(Integer.valueOf(index)));
        }
        return rawcontext.toString();
    }



    /**
     * @return the path to the dataset file
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * @return the rate matrix
     */
    public SparseMatrix getRateMatrix() {
        return rateMatrix;
    }

    /**
     * @return rating scales
     */
    public List<Double> getRatingScale() {
        return ratingScale;
    }

    /**
     * @return user {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getUserIds() {
        return userIds;
    }

    /**
     * @return item {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getItemIds() {
        return itemIds;
    }


    /**
     * @return UserItem {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getUserItemIds() {
        return uiIds;
    }

    /**
     * @return Context {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getContextIds() {
        return ctxIds;
    }

    /**
     * @return Context dimension {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getContextDimensionIds() {
        return dimIds;
    }

    /**
     * @return Context condition {rawid, inner id} mappings
     */
    public BiMap<String, Integer> getContextConditionIds() {
        return condIds;
    }

    public Multimap<Integer, Integer> getURatedList(){return this.uRatedList;}
    public Multimap<Integer, Integer> getIRatedList(){return this.iRatedList;}
    public Multimap<Integer, Integer> getDimConditionsList(){return this.dimConditionsList;}
    public HashMap<Integer, Integer> getConditionDimensionMap(){return this.condDimensionMap;}
    public Multimap<Integer, Integer> getConditionContextsList(){return this.condContextsList;}
    public HashMap<Integer,  ArrayList<Integer>> getContextConditionsList(){return this.contextConditionsList;}
    public ArrayList<Integer> getEmptyContextConditions(){return this.EmptyContextConditions;}

    public int getUserIdFromUI(int uiid)
    {
        return this.uiUserIds.get(uiid);
    }

    public int getItemIdFromUI(int uiid)
    {
        return this.uiItemIds.get(uiid);
    }

    public HashMap<Integer, Integer> getUiUserIds(){return this.uiUserIds;}
    public HashMap<Integer, Integer> getUiItemIds(){return this.uiItemIds;}

    public double getRatingMax()
    {
        if(MaxRate==-1)
            MaxRate=Collections.max(getRatingScale());
        return MaxRate;
    }
    public double getRatingMin()
    {
        if(MaxRate==-1)
            MinRate=Collections.min(getRatingScale());
        return MinRate;
    }


    /**
     * @return name of the data file with file type extension
     */
    public String getDataName() {
        if (dataName == null) {
            dataName = dataPath.substring(dataPath.lastIndexOf(File.separator) + 1, dataPath.lastIndexOf("."));
        }

        return dataName;
    }

    /**
     * @return directory of the data file
     */
    public String getDataDirectory() {
        if (dataDir == null) {
            int pos = dataPath.lastIndexOf(File.separator);
            dataDir = pos > 0 ? dataPath.substring(0, pos + 1) : "." + File.separator;
        }

        return dataDir;
    }

    public HashMap<Integer, HashMultimap<Integer, Integer>> getUserCtxList(SparseMatrix sm)
    {
        HashMap<Integer, HashMultimap<Integer, Integer>> uciList=new HashMap<>();
        for(int uiid:sm.rows()){
            int uid=this.getUserIdFromUI(uiid);
            SparseVector sv=sm.row(uiid);
            if(sv.getCount()>0){
                int[] ctx=sv.getIndex();
                for(int c:ctx){
                    int itemid = this.getItemIdFromUI(uiid);
                    if(uciList.containsKey(uid))
                    {
                        uciList.get(uid).put(c,itemid);
                    }else
                    {
                        HashMultimap<Integer, Integer> cis=HashMultimap.create();
                        cis.put(c,itemid);
                        uciList.put(uid, cis);
                    }

                }
            }
        }
        return uciList;
    }

    public HashMap<Integer, HashMultimap<Integer, Integer>> getUserCtxList(SparseMatrix sm, double rateThreshold)
    {
        HashMap<Integer, HashMultimap<Integer, Integer>> uciList=new HashMap<>();
        for(int uiid:sm.rows()){
            int uid=this.getUserIdFromUI(uiid);
            SparseVector sv=sm.row(uiid);
            if(sv.getCount()>0){
                int[] ctx=sv.getIndex();
                for(int c:ctx){
                    if(sv.get(c)>rateThreshold) {

                        int itemid = this.getItemIdFromUI(uiid);
                        if (uciList.containsKey(uid)) {
                            uciList.get(uid).put(c, itemid);
                        } else {
                            HashMultimap<Integer, Integer> cis = HashMultimap.create();
                            cis.put(c, itemid);
                            uciList.put(uid, cis);
                        }
                    }

                }
            }
        }
        return uciList;
    }

    public HashMap<Integer, HashMultimap<Integer, Integer>> getCtxUserList(SparseMatrix sm)
    {
        HashMap<Integer, HashMultimap<Integer, Integer>> cuiList=new HashMap<>();
        for(int c:sm.columns())
        {
            SparseVector sv = sm.column(c);
            if(sv.getCount()>0) {
                int[] uis = sv.getIndex();
                for(int uiid:uis){
                    int uid = this.getUserIdFromUI(uiid);
                    int itemid = this.getItemIdFromUI(uiid);
                    if(cuiList.containsKey(c))
                    {
                        cuiList.get(c).put(uid,itemid);
                    }else
                    {
                        HashMultimap<Integer, Integer> uiss=HashMultimap.create();
                        uiss.put(uid,itemid);
                        cuiList.put(c, uiss);
                    }
                }
            }


        }

        return cuiList;
    }

    public HashMap<Integer, HashMultimap<Integer, Integer>> getCtxUserList(SparseMatrix sm, double rateThreshold)
    {
        HashMap<Integer, HashMultimap<Integer, Integer>> cuiList=new HashMap<>();
        for(int c:sm.columns())
        {
            SparseVector sv = sm.column(c);
            if(sv.getCount()>0) {
                int[] uis = sv.getIndex();
                for(int uiid:uis){
                    if(sv.get(uiid)>rateThreshold) {
                        int uid = this.getUserIdFromUI(uiid);
                        int itemid = this.getItemIdFromUI(uiid);
                        if (cuiList.containsKey(c)) {
                            cuiList.get(c).put(uid, itemid);
                        } else {
                            HashMultimap<Integer, Integer> uiss = HashMultimap.create();
                            uiss.put(uid, itemid);
                            cuiList.put(c, uiss);
                        }
                    }
                }
            }


        }

        return cuiList;
    }

    public Set<Integer> getRatedItemsList(SparseMatrix sm, int uid)
    {
        Set<Integer> list=new HashSet<>();
        Collection<Integer> uiids=uRatedList.get(uid);
        for(Integer ui:uiids){
            if(sm.row(ui).getCount()>0)
                list.add(getItemIdFromUI(ui));
        }
        return list;
    }

    public Set<Integer> getItemList(SparseMatrix sm)
    {
        Set<Integer> items=new HashSet<>();
        for(int uiid:sm.rows())
        {
            items.add(getItemIdFromUI(uiid));
        }
        return items;
    }
    public Set<Integer> getUserList(SparseMatrix sm)
    {
        Set<Integer> users=new HashSet<>();
        for(int uiid:sm.rows())
        {
            users.add(getUserIdFromUI(uiid));
        }
        return users;
    }

    public boolean isHeadline() {
        return isHeadline;
    }

    public void setHeadline(boolean isHeadline) {
        this.isHeadline = isHeadline;
    }

    public SparseTensor getRateTensor() {
        return rateTensor;
    }

    public librec.data.SparseMatrix toTraditionalSparseMatrix(SparseMatrix sm)
    {
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for(int uiid:sm.rows()){
            int uid=getUserIdFromUI(uiid);
            int iid=getItemIdFromUI(uiid);
            SparseVector sv=sm.row(uiid);
            if(sv.getCount()>0) {
                dataTable.put(uid, iid, sv.mean());
                colMap.put(iid,uid);
            }
        }

        return new librec.data.SparseMatrix(numUsers(), numItems(), dataTable, colMap);
    }

    public int getRatingCountByItem(SparseMatrix sm, int itemid)
    {
        int total=0;
        for(int ui: iRatedList.get(itemid))
            total+=sm.getColumns(ui).size();
        return total;
    }

    public double getUserContextAvg(SparseMatrix sm, int userId, int contextId){
        Collection<Integer> uiids=uRatedList.get(userId);
        String sctx=idCtx.get(contextId);
        String[] conditions = sctx.split(",");
        double[] sums=new double[conditions.length];
        double[] counters=new double[conditions.length];

        for(Integer ui:uiids){
            Collection<Integer> ctx=sm.getColumns(ui);
            for(Integer c:ctx){
                for(int i=0;i<conditions.length;++i)
                {
                    Collection<Integer> ccs=condContextsList.get(Integer.valueOf(conditions[i]));
                    if(ccs.contains(c)) {
                        sums[i] += sm.get(ui,c);
                        counters[i] += 1.0;
                    }
                }
            }
        }
        double avg=0;
        double counter=0;
        for(int i=0;i<sums.length;++i)
        {
            if(counters[i]>0) {
                avg += sums[i] / counters[i];
                counter+=1.0;
            }
        }
        if(counter>0) {
            avg /= counter;
            return avg;
        }else
            return 0;
    }

    public double getItemContextAvg(SparseMatrix sm, int itemId, int contextId){
        Collection<Integer> uiids=iRatedList.get(itemId);
        String sctx=idCtx.get(contextId);
        String[] conditions = sctx.split(",");
        double[] sums=new double[conditions.length];
        double[] counters=new double[conditions.length];

        for(Integer ui:uiids){
            Collection<Integer> ctx=sm.getColumns(ui);
            for(Integer c:ctx){
                for(int i=0;i<conditions.length;++i)
                {
                    Collection<Integer> ccs=condContextsList.get(Integer.valueOf(conditions[i]));
                    if(ccs.contains(c)) {
                        sums[i] += sm.get(ui, c);
                        counters[i] += 1.0;
                    }
                }
            }
        }
        double avg=0;
        double counter=0;
        for(int i=0;i<sums.length;++i)
        {
            if(counters[i]>0) {
                avg += sums[i] / counters[i];
                counter+=1.0;
            }
        }
        if(counter>0) {
            avg /= counter;
            return avg;
        }else
            return 0;
    }

    public double getContextAvg(SparseMatrix sm, int contextId)
    {
        // aggregate average ratings by each context dimension
        String sctx=idCtx.get(contextId);
        String[] conditions=sctx.split(",");
        double[] sums=new double[conditions.length];
        double[] counters=new double[conditions.length];

        for(int i=0;i<conditions.length;++i)
        {
            int cond=Integer.valueOf(conditions[i]);
            Collection<Integer> ctx=condContextsList.get(cond);
            for(Integer c:ctx){
                SparseVector sv=sm.column(c);
                if(sv.size()!=0) {
                    sums[i] += sv.sum();
                    counters[i] += sv.size();
                }
            }
        }
        double avg=0;
        double counter=0;
        for(int i=0;i<sums.length;++i)
        {
            if(counters[i]>0) {
                avg += sums[i] / counters[i];
                counter+=1.0;
            }
        }
        if(counter>0) {
            avg /= counter;
            return avg;
        }else
            return 0;
    }

}
