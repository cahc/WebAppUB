package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import SwePub.ClassificationCategory;
import SwePub.HsvCodeToName;
import WebApp.ClassProbPair;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPoint;
import jsat.linear.SparseVector;
import jsat.linear.Vec;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



/**
 * Created by crco0001 on 10/2/2017.
 */

import java.io.BufferedReader;

import java.io.File;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.nio.charset.StandardCharsets;

import java.text.DecimalFormat;

import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.Part;

import static cc.org.web.ClarivateToJsat.extractTermsFromClarivateRecord;
import static cc.org.web.ClarivateToJsat.printSparseVector;

/**
 *
 * @author crco0001
 */
@WebServlet(name = "ClarivateServlet", urlPatterns = {"/upload","/sendBack","/mapSize","/clearMap"},

        initParams =  {@WebInitParam(name = "Admin",value="Apan Ola"),
                @WebInitParam(name = "Admin2",value="Apan Ola2")

        }

)

@MultipartConfig(fileSizeThreshold=1024*1024*50, // 50MB
        maxFileSize=1024*1024*200,      // 200B
        maxRequestSize=1024*1024*200)   // 200MB




public class ClarivateServlet extends HttpServlet {

    private final String regex = "diva2:\\d{1,20}";

    private final Pattern r = Pattern.compile(regex);

    private IndexAndGlobalTermWeights englishLevel5 = null;

    private Classifier classifierlevel5eng = null;

    private Classifier classifierLevel3swe = null;

    private  IndexAndGlobalTermWeights swedishLevel3 = null;

    private DecimalFormat df = new DecimalFormat("#0.00");

    private transient ServletConfig servletConfig;


    private final Map<Long,List<ClarivateRecord>> filesReadyForSendingToClient = new HashMap<>();

    protected final Random random = new Random();

    protected byte[] newLine;

    private SparseVector getSparseVectorEngLevel5(List<String> terms) {

       int vectorSpaceSize =  englishLevel5.getNrTerms();

        SparseVector sparseVector = new SparseVector(vectorSpaceSize, terms.size()/2 ); //length and initial capacity

        for (String t : terms) {

            int index = englishLevel5.getIndex(t);
            if(index != -1) sparseVector.increment( index, 1d  );
        }


        englishLevel5.applyGlobalTermWeights(sparseVector);

        return sparseVector;
    }



    @Override
    public void init(ServletConfig config) throws ServletException {

       String whereAmI = new File(".").getAbsolutePath();

        try {
            this.servletConfig = config;
            this.newLine = "\n".getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClarivateServlet.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {
            englishLevel5 = new IndexAndGlobalTermWeights("eng", 5);

            swedishLevel3 = new IndexAndGlobalTermWeights("swe", 3);

            englishLevel5.readFromMapDB();

            swedishLevel3.readFromMapDB();





        } catch (Throwable t) {

            t.printStackTrace();
            System.out.println("readFromMapDB throwed some shit!");

        }


        try {
            this.classifierlevel5eng = TrainAndPredict.HelperFunctions.readSerializedClassifier("classifier.eng.5.ser");
            this.classifierLevel3swe = TrainAndPredict.HelperFunctions.readSerializedClassifier("classifier.swe.3.ser");

        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Could not read serialized model!");
        }


        System.out.println("From servlet init() I am here: " + whereAmI);
    }

    @Override
    public ServletConfig getServletConfig() {

        return servletConfig;
    }


    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */



    protected void reportMapSize(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");


        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ClarivateServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("mapSize is; " + this.filesReadyForSendingToClient.size());

            out.println("</body>")  ;
            out.println("</html>");

        }
    }

    protected void clearMap(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        this.filesReadyForSendingToClient.clear();

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ClarivateServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("map now cleared. New mapSize is; " + this.filesReadyForSendingToClient.size());

            out.println("</body>")  ;
            out.println("</html>");

        }
    }

    protected void sendFile(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=resultat.txt");

        String stringID = request.getParameter("id");

        Long id = null;
        try {
            id = Long.valueOf(stringID);
        } catch(NumberFormatException e) {

            id = null;
        }


        List<ClarivateRecord> listOfProcessedRecords = Collections.emptyList();

        if(id != null) {

            listOfProcessedRecords = this.filesReadyForSendingToClient.get(id);

            if(listOfProcessedRecords == null) listOfProcessedRecords = Collections.emptyList();

        }



        OutputStream os = response.getOutputStream();


        for(ClarivateRecord record : listOfProcessedRecords) {

            byte[] sendThis = record.toString().getBytes("UTF-8");
            os.write(sendThis);
            os.write(newLine);

            List<String> termsForClassifications = extractTermsFromClarivateRecord(record);

            byte[] sendThis2 = termsForClassifications.toString().getBytes("UTF-8");
            os.write(sendThis2);
            os.write(newLine);

            //byte[] sendThis5 = record.getAddressParts().toString().getBytes("UTF-8");
            //os.write(sendThis5);
            //os.write(newLine);


            //can be nnz == 0
            SparseVector sparseVector = getSparseVectorEngLevel5(termsForClassifications);

            sparseVector.normalize();
            int nnz = sparseVector.nnz();

            //classify

            CategoricalResults result = this.classifierlevel5eng.classify( new DataPoint(sparseVector) );

            int hsv = result.mostLikely();
            double prob = result.getProb(hsv);
            ClassificationCategory true_hsv =  HsvCodeToName.getCategoryInfo( IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(hsv)    );

            StringBuilder bestGuess = new StringBuilder("UKÄ/SCB: " + true_hsv.getCode() + " : " + true_hsv.getEng_description().replaceAll("-->","→") +  " (probability: " + df.format(prob) +")");


            //Also suggest other categories?

            Vec probabilities = result.getVecView();
            List<ClassProbPair> classProbPairs = new ArrayList<>(5);

            for(int i=0; i < probabilities.length(); i++) {

                if(i == hsv) continue;

                if(probabilities.get(i) > 0.2) classProbPairs.add( new ClassProbPair(i,probabilities.get(i)));


            }

            Collections.sort(classProbPairs, Comparator.reverseOrder());

            for(int i=0; i<classProbPairs.size(); i++) {

                ClassificationCategory true_hsv2 =  HsvCodeToName.getCategoryInfo( IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get( classProbPairs.get(i).classCode   )    );
                double probability = classProbPairs.get(i).probability;
                bestGuess.append("\n");
                bestGuess.append("UKÄ/SCB: " + true_hsv2.getCode() + " : " + true_hsv2.getEng_description().replaceAll("-->","→") +  " (probability: " + df.format(probability) +")");


            }



            //Stringy
            String sparseVectorString = printSparseVector(sparseVector);

            byte[] sendThis3 = sparseVectorString.toString().getBytes("UTF-8");
            os.write(sendThis3);
            os.write(newLine);

            byte[] sendThis4 = bestGuess.toString().getBytes("UTF-8");
            os.write(sendThis4);
            os.write(newLine);
            os.write(newLine);
        }


        //response.setContentLength((int) sendThis.length );


        os.flush();
        os.close();


        //remove from map
        if(id != null) {


            this.filesReadyForSendingToClient.remove(id);
        }


    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.endsWith("/sendBack")) {

            sendFile(request,response);
        } else if(uri.endsWith("/mapSize")) {

            reportMapSize(request,response);

        } else if(uri.endsWith("/clearMap")) {

            clearMap(request,response);

        }



    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");


        // gets absolute path of the web application

        String appPath = request.getServletContext().getRealPath("");
        final Part filePart = request.getPart("uploadFile"); //TODO check null

        boolean continueParsing = isTextPlain(filePart); // true if text/plain, false if null or not text/plain

        PrintWriter w = response.getWriter();



        if(continueParsing) {

           // final String fileName = filePart.getSubmittedFileName();
          //  final String contentType = filePart.getContentType();

            //ServletOutputStream out = response.getOutputStream();
            //out.write("MY-UTF-8 CODE".getBytes("UTF-8"));

            // w.println("The filename is: " + fileName);
            //  w.println("The size in Kb is: " +    filePart.getSize()/1024.0 );

            //  w.println("The type is: " + contentType);

            InputStream data = filePart.getInputStream();


            BufferedReader br = new BufferedReader(new InputStreamReader(data, StandardCharsets.UTF_8));


            String line;
            boolean isClarivateData = false;
            while( (line = br.readLine()) != null) {

                isClarivateData = ClarivateParser.identifierFound(line);
                if(isClarivateData) break;

                //w.println(line);
            }









            if(isClarivateData) {
                w.print("<BR/>");

                w.print("<p>This is a valid export file from Clarivate Analytics</p>");

                //continute with parsing

                ClarivateParser clarivateParser = new ClarivateParser(br);

                List<ClarivateRecord> recordList = clarivateParser.parse();

                w.println("<p>Number of records parsed: " +  recordList.size() +"</p>");

                for(int i=0; i<recordList.size(); i++) {

                    //    ClarivateRecord record = recordList.get(i);

                    //     w.println("<p>" + record.title + " " + record.UT +"</p>" );

                    //      w.println("<p>" + record.toString() +"</p>" );
                    //       w.println("<BR/>");

                }

                w.println("<p>" +recordList.size() + " poster klassificerade</p>");



                long key = this.random.nextLong();

                w.println(generateDownloadLink(key,recordList));



                br.close();


            } else {

                w.println("<BR/>");
                w.print("<p>You uploaded a text/plain file but not a valid export file from Clarivate. Check again?</p>");
                br.close();
            }







        } else {

            w.println("<BR>");
            w.println("<p>The uploaded file must be of type text/plain.</p>");
            w.flush();
            w.close();
        }







        w.flush();
        w.close();



    }



    protected String generateDownloadLink(long key, List<ClarivateRecord> records ) {



        this.filesReadyForSendingToClient.put(key, records);

        return "<button type=\"button\" onclick=\"location.href='sendBack?id=" + key +"'\" value=\"Go to Google\" class=\"btn btn-info\"><span class=\"glyphicon glyphicon-download-alt\"></span> Klart för nedladdning  </button>\n" +"";


    }


    protected boolean isTextPlain(Part filePart) {


        if(filePart == null) return false;

        String contentType = filePart.getContentType();

        return contentType.equals("text/plain");

    }



}
