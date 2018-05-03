package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import Database.MyOwnException;
import jsat.linear.SparseVector;


import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 10/4/2017.
 */
public class PlayGround {

    public static void main(String[] arg) throws IOException, MyOwnException {


      BufferedReader reader = new BufferedReader( new FileReader( new File("/Users/Cristian/Desktop/APIexperiment/wosExportPlainUTF8.txt")));

      ClarivateParser clarivateParser = new ClarivateParser(reader);

      List<ClarivateRecord> recordList = clarivateParser.parse();

      System.out.println(recordList.size());

      System.out.println(  System.getProperty("os.name") );

        IndexAndGlobalTermWeights swedishLevel3 = null;

        swedishLevel3 = new IndexAndGlobalTermWeights("swe", 3);

        swedishLevel3.readFromMapDB("/Users/Cristian/models/");





    }


}
