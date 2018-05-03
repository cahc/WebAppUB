package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import SwePub.ClassificationCategory;
import SwePub.HsvCodeToName;
import WebApp.ClassProbPair;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.DataPoint;
import jsat.linear.SparseVector;
import jsat.linear.Vec;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static cc.org.web.ClarivateServlet.getSparseVectorEngLevel5;
import static cc.org.web.ClarivateToJsat.extractTermsFromClarivateRecord;

public class ClarivateAPI {

    private DecimalFormat df = new DecimalFormat("#0.00");

    public ClarivateAPI(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, ParserConfigurationException, TransformerException {


        response.setContentType("text/xml;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=resultat.xml");
        response.setHeader("Content-Encoding", "gzip");


        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root element
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("poster");
        doc.appendChild(rootElement);

        //meta tag

        Element meta = doc.createElement("meta");
        Element antalDocs = doc.createElement("uploadedRecords");
        //antalDocs.appendChild( doc.createTextNode("100"));
        meta.appendChild(antalDocs);
        Element key = doc.createElement("key");
        //key.appendChild( doc.createTextNode("cristian.colliander@umu.se"));
        meta.appendChild(key);
        Element tröskelvärde = doc.createElement("multilabelthreshold");
        //tröskelvärde.appendChild(doc.createTextNode("0.5"));
        meta.appendChild(tröskelvärde);
        Element felmeddelande = doc.createElement("errorMessage");
        meta.appendChild(felmeddelande);
        rootElement.appendChild(meta);


        /////////////////////En post/////////////////////////////////////

        /*
        Element record = doc.createElement("post");
        rootElement.appendChild(record);
        Attr attr = doc.createAttribute("UT");
        attr.setValue("1");
        record.setAttributeNode(attr);

        //  classification elements
        Element classification = doc.createElement("forskningsämne");
        classification.setAttribute("sannolikhet","0.1");
        classification.setAttribute("kod", "2222");
        classification.appendChild(doc.createTextNode("historia"));

        record.appendChild(classification);
       */
       //////////////////////////////////////////////////////////////


        String nyckel = request.getParameter("key");
        key.appendChild( doc.createTextNode(nyckel));
        String threshold = request.getParameter("multilabelthreshold");
        tröskelvärde.appendChild(doc.createTextNode(threshold));
        final Part data = request.getPart("data");



            if(data == null) {

                felmeddelande.appendChild(doc.createTextNode("Ingen fil skickad med andropet,data=null"));

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                GZIPOutputStream pw = (new GZIPOutputStream(response.getOutputStream()));
                StreamResult result = new StreamResult(pw); //System.out for debuging


                //OutputStream os = response.getOutputStream();

                //StreamResult result = new StreamResult(os); //System.out for debuging

                transformer.transform(source,result);
                pw.flush();
                pw.close();


                //OutputStream os = response.getOutputStream();

                //StreamResult result = new StreamResult(os); //System.out for debuging


                return;

            }


        BufferedReader br = new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8));
        int maxRowsConsidered = 500;
        boolean isClarivateData = false;
        String line;
        while ((line = br.readLine()) != null && maxRowsConsidered > 0) {

            isClarivateData = ClarivateParser.identifierFound(line);
            if (isClarivateData) break;
            maxRowsConsidered--;

        }


        if(!isClarivateData) {


            felmeddelande.appendChild(doc.createTextNode("Uppladded fil ej igenkänd som en WoS-exportfil"));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);



            GZIPOutputStream pw = (new GZIPOutputStream(response.getOutputStream()));
            StreamResult result = new StreamResult(pw); //System.out for debuging


            //OutputStream os = response.getOutputStream();

            //StreamResult result = new StreamResult(os); //System.out for debuging

            transformer.transform(source,result);
            pw.flush();
            pw.close();

            //OutputStream os = response.getOutputStream();

            //StreamResult result = new StreamResult(os); //System.out for debuging


            return;


           }


            if(nyckel == null) {

                felmeddelande.appendChild(doc.createTextNode("Ingen epost angiven,key=null"));

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);


                GZIPOutputStream pw = (new GZIPOutputStream(response.getOutputStream()));
                StreamResult result = new StreamResult(pw); //System.out for debuging


                //OutputStream os = response.getOutputStream();

                //StreamResult result = new StreamResult(os); //System.out for debuging

                transformer.transform(source,result);
                pw.flush();
                pw.close();

                return;


            }

            double thresholdValue = -1;
            try {
             thresholdValue = Double.valueOf(threshold);
            } catch (NumberFormatException e) {


            }

        if(thresholdValue > 0.5101 || thresholdValue < 0.1) {

            felmeddelande.appendChild(doc.createTextNode("Ogiltigt värde på multilabelthreshold"));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);



            //curl  --compressed --form "data=@uploadfile.txt" "http://127.0.0.1:8080/api/v1/wos?key=cristian.colliander@umu.se&multilabelthreshold=1.11" --proxy 127.0.0.1:8888

            OutputStream os = response.getOutputStream();


            GZIPOutputStream pw = (new GZIPOutputStream(response.getOutputStream()));
            StreamResult result = new StreamResult(pw); //System.out for debuging
            transformer.transform(source,result);

            pw.flush();
            pw.close();


            return;

        }


            ClarivateParser clarivateParser = new ClarivateParser(br);
            List<ClarivateRecord> recordList = clarivateParser.parse();
            br.close();

            antalDocs.appendChild( doc.createTextNode( String.valueOf(recordList.size()) ));


            for(ClarivateRecord clarivateRecord : recordList ) {


                //record element
                Element record = doc.createElement("post");
                rootElement.appendChild(record);
                Attr UTattr = doc.createAttribute("UT");
                UTattr.setValue( clarivateRecord.UT );
                record.setAttributeNode(UTattr);

                String language = clarivateRecord.getLanguage(); // from LA tag
                List<String> termsForClassifications = extractTermsFromClarivateRecord(clarivateRecord);

                SparseVector sparseVector = getSparseVectorEngLevel5(termsForClassifications);
                sparseVector.normalize();
                int nnz = sparseVector.nnz();


                if(language != null && language.equals("English") && nnz>0) {




                    CategoricalResults result = ClarivateServlet.classifierlevel5eng.classify(new DataPoint(sparseVector));


                    int hsv = result.mostLikely();
                    double prob = result.getProb(hsv);

                    ClassificationCategory true_hsv = HsvCodeToName.getCategoryInfo(IndexAndGlobalTermWeights.level5ToCategoryCodes.inverse().get(hsv));


                    //StringBuilder bestGuess = new StringBuilder("UKÄ/SCB: " + true_hsv.getCode() + " : " + true_hsv.getEng_description().replaceAll("-->", "→") + " (probability: " + df.format(prob) + ")");
                    //byte[] sendThis = bestGuess.toString().getBytes("UTF-8");


                    Element classification = doc.createElement("forskningsämne");
                    classification.setAttribute("sannolikhet",df.format(prob) );
                    classification.setAttribute("kod", String.valueOf(true_hsv.getCode()) );
                    classification.appendChild(doc.createTextNode(true_hsv.getSwe_description()));
                    record.appendChild(classification);

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
                        double probability = classProbPairs.get(i).probability;

                        //String extra = ("UKÄ/SCB: " + true_hsv2.getCode() + " : " + true_hsv2.getEng_description().replaceAll("-->", "→") + " (probability: " + df.format(probability) + ")");


                        Element classification2 = doc.createElement("forskningsämne");
                        classification2.setAttribute("sannolikhet",df.format(probability) );
                        classification2.setAttribute("kod", String.valueOf(true_hsv2.getCode()) );
                        classification2.appendChild(doc.createTextNode(true_hsv2.getSwe_description()));
                        record.appendChild(classification2);


                    }




                } else {


                    //not supported language or nnz=0


                }


                rootElement.appendChild(record);

            }


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        //OutputStream os = response.getOutputStream();
        //StreamResult result = new StreamResult(os); //System.out for debuging


        GZIPOutputStream pw = (new GZIPOutputStream(response.getOutputStream()));
        StreamResult result = new StreamResult(pw); //System.out for debuging
        transformer.transform(source,result);
        pw.flush();
        pw.close();


            return;
        }



    }

