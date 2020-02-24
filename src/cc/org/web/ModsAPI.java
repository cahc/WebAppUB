package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import Database.ModsDivaFileParser;
import SwePub.ClassificationCategory;
import SwePub.HsvCodeToName;
import SwePub.Record;
import WebApp.ClassProbPair;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.DataPoint;
import jsat.linear.SparseVector;
import jsat.linear.Vec;
import org.w3c.dom.Element;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

//@MultipartConfig( fileSizeThreshold = 0, maxFileSize = 209715200, maxRequestSize = 209715200) //200BM this is set in ClearivateServlet
public class ModsAPI {

    private DecimalFormat df = new DecimalFormat("#0.00");

    private final static String sep = "\t";
    private final static String newLine = System.getProperty("line.separator");


    public ModsAPI(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/xml;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=klassificeringar.txt");
        response.setHeader("Content-Encoding", "gzip");

        GZIPOutputStream os = new GZIPOutputStream(response.getOutputStream() );

        PrintWriter w = new PrintWriter( new OutputStreamWriter(os, StandardCharsets.UTF_8) ); //response.getWriter();

        String nyckel = request.getParameter("key");
        if(nyckel == null) {

            w.println("key missing..");
            w.flush();
            w.close();
            return;
        }




        String threshold = request.getParameter("multilabelthreshold");
        if(threshold == null) {

            w.println("multilabelthreshold missing..");
            w.flush();
            w.close();
            return;
        }



        double thresholdValue = -1;
        try {
            thresholdValue = Double.valueOf(threshold);
        } catch (NumberFormatException e) {


        }

        if(thresholdValue > 1.0 || thresholdValue < 0.1) {
            w.println("invalid threshold, use values between 0.1 and 1.0..");
            w.flush();
            w.close();
            return;

        }


            Part data = null;
        try {
            data = request.getPart("data");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (data == null) {

            w.println("no data submitted in request!");
            w.flush();
            w.close();
            return;

        } else {

            BufferedReader br = new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8));
            ModsDivaFileParser modsDivaFileParser = new ModsDivaFileParser();

            List<Record> recordList = null;
            try {
                recordList = modsDivaFileParser.parse(br);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }

            if (recordList == null) {

                w.println("could not parse xml!");
                w.flush();
                w.close();
                return;

            } else {

                System.out.println("records Parsed:" + recordList.size());

                //URI DIVA2 LANG CODE PROBABILITY LEVEL 1 LEVEL 2 LEVEL3 (maybe null)
                String header = new StringBuilder().append("URI").append(sep).append("DIVA2-ID").append(sep).append("LANGUAGE").append(sep).append("HSV/SCB CODE").append(sep).append("PROBABILITY").append(sep).append("LEVEL 1").append(sep).append("LEVEL 2").append(sep).append("LEVEL 3").toString();
                w.println(header);

                for (Record record : recordList) {

                    if (record.isContainsEnglish() || record.isContainsSwedish()) {

                        SparseVector vec = null;
                        if (record.isContainsEnglish()) {
                            vec = ClarivateServlet.englishLevel5.getVecForUnSeenRecord(record);

                            if (vec != null) {
                                vec.normalize();
                                CategoricalResults result = ClarivateServlet.classifierlevel5eng.classify(new DataPoint(vec));
                                int hsv = result.mostLikely();
                                double prob = result.getProb(hsv);
                                ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(hsv));

                                StringBuilder line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("english").append(sep).append(true_hsv.getCode()).append(sep).append( df.format(prob) );

                                String[] names = true_hsv.getEng_description().split("-->");
                                for(String s : names) { line.append(sep); line.append(s); }
                                w.println(line.toString());

                                //Also suggest other categories?

                                Vec probabilities = result.getVecView();
                                List<ClassProbPair> classProbPairs = new ArrayList<>(5);

                                for (int i = 0; i < probabilities.length(); i++) {

                                    if (i == hsv) continue;

                                    if (probabilities.get(i) >= thresholdValue) classProbPairs.add(new ClassProbPair(i, probabilities.get(i)));


                                }

                                Collections.sort(classProbPairs, Comparator.reverseOrder());

                                for (int i = 0; i < classProbPairs.size(); i++) {

                                    ClassificationCategory true_hsv2 = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(classProbPairs.get(i).classCode));
                                    double probability2 = classProbPairs.get(i).probability;

                                    line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("english").append(sep).append(true_hsv2.getCode()).append(sep).append( df.format(probability2) );

                                    names = true_hsv2.getEng_description().split("-->");
                                    for(String s : names) { line.append(sep); line.append(s); }
                                    w.println(line.toString());

                                }

                                //other categories end












                            } else {

                                StringBuilder line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("english").append(sep).append( "unknown" ).append(sep).append( -1.0 );
                                line.append(sep).append("unknown").append(sep).append("unknown").append(sep).append("unknown");
                                w.println(line.toString());
                            }


                        } else {

                            //must be swedish

                            vec = ClarivateServlet.swedishLevel3.getVecForUnSeenRecord(record);

                            if (vec != null) {
                                vec.normalize();
                                CategoricalResults result = ClarivateServlet.classifierLevel3swe.classify(new DataPoint(vec));

                                int hsv = result.mostLikely();
                                double prob = result.getProb(hsv);
                                ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level3ToCategoryCodes.inverse().get(hsv));

                                StringBuilder line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("swedish").append(sep).append(true_hsv.getCode()).append(sep).append( df.format(prob) );

                                String[] names = true_hsv.getEng_description().split("-->");
                                for(String s : names) { line.append(sep); line.append(s); }
                                line.append(sep).append("not available for swedish records");

                                w.println(line.toString());

                                //Also suggest other categories?

                                Vec probabilities = result.getVecView();
                                List<ClassProbPair> classProbPairs = new ArrayList<>(5);

                                for (int i = 0; i < probabilities.length(); i++) {

                                    if (i == hsv) continue;

                                    if (probabilities.get(i) >= thresholdValue) classProbPairs.add(new ClassProbPair(i, probabilities.get(i)));


                                }

                                Collections.sort(classProbPairs, Comparator.reverseOrder());

                                for (int i = 0; i < classProbPairs.size(); i++) {

                                    ClassificationCategory true_hsv2 = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level3ToCategoryCodes.inverse().get(classProbPairs.get(i).classCode));
                                    double probability2 = classProbPairs.get(i).probability;

                                    line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("swedish").append(sep).append(true_hsv2.getCode()).append(sep).append( df.format(probability2) );

                                    names = true_hsv2.getEng_description().split("-->");
                                    for(String s : names) { line.append(sep); line.append(s); }
                                    line.append(sep).append("not available for swedish records");
                                    w.println(line.toString());

                                }

                                //other categories end








                            } else {

                                StringBuilder line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("swedish").append(sep).append( "unknown" ).append(sep).append( -1.0 );
                                line.append(sep).append("unknown").append(sep).append("unknown").append(sep).append("unknown");
                                w.println(line.toString());
                            }


                        }


                    } else {

                        StringBuilder line = new StringBuilder().append(record.getURI()).append(sep).append(record.getDiva2Id()).append(sep).append("unsupported language detected").append(sep).append( "unknown" ).append(sep).append( -1.0 );
                        line.append(sep).append("unknown").append(sep).append("unknown").append(sep).append("unknown");
                        w.println(line.toString());

                    }


                } //next record


                w.flush();
                w.close();

            }


        }

    }

}
