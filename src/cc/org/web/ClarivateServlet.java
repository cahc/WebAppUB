package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import Database.ModsOnlineParser;
import SwePub.ClassificationCategory;
import SwePub.HsvCodeToName;
import SwePub.Record;
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
import java.io.*;

import com.auxilii.msgparser.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.text.DecimalFormat;

import java.text.ParseException;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.Part;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import static cc.org.web.ClarivateToJsat.extractTermsFromClarivateRecord;
import static cc.org.web.ClarivateToJsat.printSparseVector;

/**
 *
 * @author crco0001
 */
@WebServlet(name = "ClarivateServlet", urlPatterns = {"/upload","/sendBack","/mapSize","/clearMap","/fetchId","/api/v1/wos","/api/v1/mods"},

        initParams =  {@WebInitParam(name = "Admin",value="Apan Ola"),
                @WebInitParam(name = "Admin2",value="Apan Ola2")

        }

)

@MultipartConfig(fileSizeThreshold=1024*1024*50, // 50MB
        maxFileSize=1024*1024*200,      // 200B
        maxRequestSize=1024*1024*200)   // 200MB




public class ClarivateServlet extends HttpServlet {

    private final String regex = "\\d{3,15}";

    private final Pattern r = Pattern.compile(regex);

    public static IndexAndGlobalTermWeights englishLevel5 = null;

    public static Classifier classifierlevel5eng = null;

    public static Classifier classifierLevel3swe = null;

    public static  IndexAndGlobalTermWeights swedishLevel3 = null;

    private DecimalFormat df = new DecimalFormat("#0.00");

    private transient ServletConfig servletConfig;


    private final Map<Long,List<ClarivateRecord>> filesReadyForSendingToClient = new HashMap<>();

    protected final Random random = new Random();

    protected byte[] newLine;

    public static SparseVector getSparseVectorEngLevel5(List<String> terms) {

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


         String osName = System.getProperty("os.name");


         boolean isWinDev = false;
         boolean isMacDev = false;

         if(osName != null && osName.toLowerCase().contains("windows") )   isWinDev = true;
         if(osName != null && osName.toLowerCase().contains("mac os") )   isMacDev = true;

            String dir = null;
            if(isWinDev) {

                dir = ("C:\\opt\\models\\");
            } else if(isMacDev) {

                dir = "/Users/Cristian/models/";

            } else {


                dir = ("/opt/models/");
            }

            System.out.println("looking for models in directory: " + dir);

            try {
            this.servletConfig = config;
            this.newLine = "\r\n".getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClarivateServlet.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {
            englishLevel5 = new IndexAndGlobalTermWeights("eng", 5);

            swedishLevel3 = new IndexAndGlobalTermWeights("swe", 3);

            englishLevel5.readFromMapDB(dir);

            swedishLevel3.readFromMapDB(dir);





        } catch (Throwable t) {

            t.printStackTrace();
            System.out.println("readFromMapDB throwed some shit!: " + t.getLocalizedMessage() + "" + t.getMessage());

        }


        try {
            this.classifierlevel5eng = TrainAndPredict.HelperFunctions.readSerializedClassifier(dir + "classifier.eng.5.ser");
            this.classifierLevel3swe = TrainAndPredict.HelperFunctions.readSerializedClassifier(dir + "classifier.swe.3.ser");

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

        String keyForGettingSavedResult = request.getParameter("id");

        Long id = null;
        try {
            id = Long.valueOf(keyForGettingSavedResult);
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

           // byte[] sendThis = record.toString().getBytes("UTF-8");
           // os.write(sendThis);
          //  os.write(newLine);

            List<String> termsForClassifications = extractTermsFromClarivateRecord(record);

            String language = record.guessLanguage();

            boolean supported_language = false;
            if("en".equals(language)) supported_language = true;

            byte[] sendBasic = record.getUT().concat(" ").concat( record.getTitle()).getBytes("UTF-8");
            os.write(sendBasic);
            os.write(newLine);

           // byte[] sendLanguageGuess = language.getBytes("UTF-8");
           // os.write(sendLanguageGuess);
           // os.write(newLine);


          //  byte[] sendThis2 = termsForClassifications.toString().getBytes("UTF-8");
          //  os.write(sendThis2);
          //  os.write(newLine);

            //byte[] sendThis5 = record.getAddressParts().toString().getBytes("UTF-8");
            //os.write(sendThis5);
            //os.write(newLine);


            //can be nnz == 0

            if(supported_language) {
                SparseVector sparseVector = getSparseVectorEngLevel5(termsForClassifications);

                sparseVector.normalize();
                int nnz = sparseVector.nnz();

                //classify

                CategoricalResults result = this.classifierlevel5eng.classify(new DataPoint(sparseVector));

                int hsv = result.mostLikely();
                double prob = result.getProb(hsv);
                ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(hsv));

                StringBuilder bestGuess = new StringBuilder("UKÄ/SCB: " + true_hsv.getCode() + " : " + true_hsv.getEng_description().replaceAll("-->", "→") + " (probability: " + df.format(prob) + ")");
                byte[] sendThis = bestGuess.toString().getBytes("UTF-8");

                os.write(sendThis);
                os.write(newLine);

                //Also suggest other categories?

                Vec probabilities = result.getVecView();
                List<ClassProbPair> classProbPairs = new ArrayList<>(5);

                for (int i = 0; i < probabilities.length(); i++) {

                    if (i == hsv) continue;

                    if (probabilities.get(i) > 0.2) classProbPairs.add(new ClassProbPair(i, probabilities.get(i)));


                }

                Collections.sort(classProbPairs, Comparator.reverseOrder());

                for (int i = 0; i < classProbPairs.size(); i++) {

                    ClassificationCategory true_hsv2 = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(classProbPairs.get(i).classCode));
                    double probability = classProbPairs.get(i).probability;

                    String extra = ("UKÄ/SCB: " + true_hsv2.getCode() + " : " + true_hsv2.getEng_description().replaceAll("-->", "→") + " (probability: " + df.format(probability) + ")");

                    sendThis = extra.toString().getBytes("UTF-8");
                    os.write(sendThis);
                    os.write(newLine);

                }

                os.write(newLine);
                os.write(newLine);



            } else {

                //not supported language
                StringBuilder bestGuess = new StringBuilder("UKÄ/SCB: SPRÅKET STÖDS EJ, INGET FÖRSÖK ATT KLASSIFICERA POSTEN HAR GJORTS!");
                byte[] sendThis = bestGuess.toString().getBytes("UTF-8");
                os.write(sendThis);
                os.write(newLine);
                os.write(newLine);

            }

            //Stringy
         //   String sparseVectorString = printSparseVector(sparseVector);

        //    byte[] sendThis3 = sparseVectorString.toString().getBytes("UTF-8");
        //    os.write(sendThis3);
       //     os.write(newLine);

         //   byte[] sendThis4 = bestGuess.toString().getBytes("UTF-8");
        //    os.write(sendThis4);
           // os.write(newLine);
          //  os.write(newLine);
        }


        //response.setContentLength((int) sendThis.length );


        os.flush();
        os.close();


        //remove from map
        if(id != null) {


            this.filesReadyForSendingToClient.remove(id);
        }


    }


    //http://localhost:8080/fetchId?divaid=%22diva2:243244%22

    public void sendRequestToDivaAndClassify(HttpServletRequest request, HttpServletResponse response) throws IOException, XMLStreamException {

        response.setContentType("text/plain");  // Set content type of the response so that jQuery knows what it can expect.
        response.setCharacterEncoding("UTF-8"); // You want world domination, huh?

        PrintWriter out = response.getWriter();
        Matcher m = r.matcher(request.getParameterValues("divaid")[0]);

        String divaNumber = null;
        if (m.find( )) {
            divaNumber = m.group(0);
        }

        String diva2 = "diva2:";
        if(divaNumber != null) {

            String url2 = "http://www.diva-portal.org/smash/export.jsf?format=mods&aq=[[{\"publicationId\":\"" + diva2.concat(divaNumber) + "\"}]]&aqe=[]&aq2=[[]]&onlyFullText=false&noOfRows=2&sortOrder=title_sort_asc";

            URL obj = new URL(url2);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url2);
            System.out.println("Response Code : " + responseCode);

            if (responseCode == 500)
                out.write("DiVA svarade med \"Internal Server Error 500\". Har du angivit ett diva2-id som ej existerar?");

            BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream(),StandardCharsets.UTF_8));
            String inputLine;

            StringBuffer dataBackFromDivaServer = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                dataBackFromDivaServer.append(inputLine);
            }
            in.close();
            con.disconnect();

            //print result
            Record record = ModsOnlineParser.parse(dataBackFromDivaServer.toString());

            if (record == null || record.getTitle().size() == 0 ) {

                out.append("DiVA returnerade ingen data. Kontrollera angivet diva2-id");
            } else {


                //return "Title: " + record.getTitle() + " Supported language: " + record.containsSupportedLanguageText() +"\n\n" + record.toString();

                StringBuilder builder = new StringBuilder();

                builder.append("<p>Titel: ").append(record.getTitle()).append("<br>");
                //builder.append("innehåller svensk text: ").append(record.isContainsSwedish()).append("<br>");
                //builder.append("innehåller engelsk text: ").append(record.isContainsEnglish()).append("<br>");
                builder.append("<br>");
                //builder.append("Full record: ").append("<br>");
                //builder.append(record.toString());
                builder.append("Förslag:").append("<br>");
                builder.append("</p>");


                SparseVector vec = null;

                if (record.isContainsEnglish()) {

                    //use english level 5
                    vec = this.englishLevel5.getVecForUnSeenRecord(record);

                    if (vec != null) {

                        vec.normalize();
                        int nnz = vec.nnz();
                        //builder.append( this.englishLevel5.printSparseVector(vec)  );
                        // builder.append("nnz:" + nnz);

                        CategoricalResults result = this.classifierlevel5eng.classify(new DataPoint(vec));

                        int hsv = result.mostLikely();
                        double prob = result.getProb(hsv);

                        ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(hsv));

                        builder.append("<p>");
                        builder.append("<b>" + true_hsv.getCode() + "</b> : " + true_hsv.getEng_description().replaceAll("-->", "&rarr;") + " (sannolikhet: " + df.format(prob) + ")");
                        builder.append("</p>");


                        Vec probabilities = result.getVecView();
                        List<ClassProbPair> classProbPairs = new ArrayList<>(5);

                        for (int i = 0; i < probabilities.length(); i++) {

                            if (i == hsv) continue;

                            if (probabilities.get(i) > 0.2)
                                classProbPairs.add(new ClassProbPair(i, probabilities.get(i)));


                        }

                        Collections.sort(classProbPairs, Comparator.reverseOrder());


                        for (int i = 0; i < classProbPairs.size(); i++) {

                            ClassificationCategory true_hsv2 = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(classProbPairs.get(i).classCode));
                            double probability = classProbPairs.get(i).probability;
                            builder.append("<p>");
                            builder.append("<b>" + true_hsv2.getCode() + "</b> : " + true_hsv2.getEng_description().replaceAll("-->", "&rarr;") + " (sannolikhet: " + df.format(probability) + ")");
                            builder.append("</p>");


                        }

                        //builder.append(result.getVecView());


                    }


                } else if (record.isContainsSwedish()) {

                    vec = this.swedishLevel3.getVecForUnSeenRecord(record);

                    if (vec != null) {

                        vec.normalize();
                        int nnz = vec.nnz();
                        // builder.append("nnz:" + nnz);
                        //builder.append( this.swedishLevel3.printSparseVector(vec)  );

                        CategoricalResults result = this.classifierLevel3swe.classify(new DataPoint(vec));

                        int hsv = result.mostLikely();
                        double prob = result.getProb(hsv);
                        ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level3ToCategoryCodes.inverse().get(hsv));

                        builder.append("<p>");
                        builder.append("<b>" + true_hsv.getCode() + "</b> : " + true_hsv.getEng_description().replaceAll("-->", "&rarr;") + " (probability: " + df.format(prob) + ")");
                        builder.append("</p>");


                        Vec probabilities = result.getVecView();
                        List<ClassProbPair> classProbPairs = new ArrayList<>(5);

                        for (int i = 0; i < probabilities.length(); i++) {

                            if (i == hsv) continue;

                            if (probabilities.get(i) > 0.25)
                                classProbPairs.add(new ClassProbPair(i, probabilities.get(i)));


                        }

                        Collections.sort(classProbPairs, Comparator.reverseOrder());


                        for (int i = 0; i < classProbPairs.size(); i++) {

                            ClassificationCategory true_hsv2 = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level3ToCategoryCodes.inverse().get(classProbPairs.get(i).classCode));
                            double probability = classProbPairs.get(i).probability;
                            builder.append("<p>");
                            builder.append("<b>" + true_hsv2.getCode() + "</b> : " + true_hsv2.getEng_description().replaceAll("-->", "&rarr;") + " (probability: " + df.format(probability) + ")");
                            builder.append("</p>");


                        }


                        //builder.append(result.getVecView());
                    }
                }


                out.append(builder.toString());

            }
        } else {

            out.write("Förefaller inte vara ett giltigt diva2-id.");

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

        } else if(uri.endsWith("/fetchId")) {

            try {
                sendRequestToDivaAndClassify(request,response);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }



    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {



        ////////////////CHECK FOR API REQUEST/////////////////
        String uri = request.getRequestURI();

        //curl -F "data=@uploadfile.txt" "http://127.0.0.1:8080/api/v1/wos?key=cristian.colliander@umu.se&multilabelthreshold=0.5"



        if (uri.endsWith("/api/v1/wos")) {

            try {
                ClarivateAPI clarivateAPI = new ClarivateAPI(request,response);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }

            return;
        }


        if (uri.endsWith("/api/v1/mods")) {

            ModsAPI modsAPI = new ModsAPI(request,response);
            return;
        }







        /////////////////////////////////////////////////


        ///////////////FORM REQUEST FROM HTML////////////


            response.setContentType("text/html; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
        boolean continueParsing = false;

        // gets absolute path of the web application
        //  String appPath = request.getServletContext().getRealPath("");

        final Part filePart = request.getPart("uploadFile"); //TODO check null

        String specifiedUploadFileType = request.getParameter("CustomField1");

        System.out.println("uploaded file of type..: " + specifiedUploadFileType); // write to logg for debugging

        System.out.println("uploaded size: " + filePart.getSize() );

        if (filePart.getSize() >= 10) continueParsing = true;

        //boolean continueParsing = isTextPlain(filePart); // true if text/plain, false if null or not text/plain

        PrintWriter w = response.getWriter();


        if (continueParsing) {

            //final String fileName = filePart.getSubmittedFileName();
            //  final String contentType = filePart.getContentType();

            //ServletOutputStream out = response.getOutputStream();
            //out.write("MY-UTF-8 CODE".getBytes("UTF-8"));

            // w.println("The filename is: " + fileName);
            //  w.println("The size in Kb is: " +    filePart.getSize()/1024.0 );

            //  w.println("The type is: " + contentType);

            InputStream data = filePart.getInputStream();
            BufferedReader br = null;
            String setupInfo = "";

            if ("Outlook-meddelandeformat (unicode)".equals(specifiedUploadFileType)) {

                //outlook parser

                try {


                    MsgParser msgp = new MsgParser();
                    Message msg = msgp.parseMsg(data);

                    InputStream body = new ByteArrayInputStream(msg.getBodyText().getBytes("UTF-8")); //TODO specify charSet in getBytes?
                    br = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
                } catch (Exception e) {

                    setupInfo = "Kontrollera att uppladdad fil verkligen är i outlook-meddelandeformat. (" + e.getLocalizedMessage() +")";

                }


            } else if ("Web of Science (plain text/full record)".equals(specifiedUploadFileType)) {

                try {
                    br = new BufferedReader(new InputStreamReader(data, StandardCharsets.UTF_8));
                } catch (Exception e) {


                    setupInfo = "Web of Science parser failed! " + e.getLocalizedMessage();
                }


            } else {

                //nothing here..

            }


            if (br != null) {


                String line;
                boolean isClarivateData = false;
                int maxRowsConsidered = 500;
                while ((line = br.readLine()) != null && maxRowsConsidered > 0) {

                    isClarivateData = ClarivateParser.identifierFound(line);
                    if (isClarivateData) break;
                    maxRowsConsidered--;
                    //w.println(line);
                }


                if (isClarivateData) {

                    //continute with parsing

                    ClarivateParser clarivateParser = new ClarivateParser(br);

                    List<ClarivateRecord> recordList = clarivateParser.parse();

                    w.println("<p>Antal poster behandlade : " + recordList.size() + "</p>");


                    long key = this.random.nextLong();

                    w.println(generateDownloadLink(key, recordList));


                    br.close();


                } else {

                    w.println("<BR/>");
                    w.print("<p>Uppladdad fil ej igenkänd som en export-fil från Web of Science.</p>");
                    br.close();
                }


                w.flush();
                w.close();

            } else {

                //br was null

                w.println( setupInfo );
            }

        } else {


            w.println("Ingen fil vald..");

        }


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
