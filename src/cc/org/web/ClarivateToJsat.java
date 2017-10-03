package cc.org.web;

import Database.SwePubParser;

import SwePub.Record;
import jsat.linear.IndexValue;
import jsat.linear.SparseVector;
import misc.LanguageTools.HelperFunctions;
import misc.LanguageTools.RemoveCopyRightFromAbstract;
import misc.Parsers.SimpleParser;
import misc.Stemmers.EnglishStemmer;
import misc.Stemmers.SwedishStemmer;
import misc.stopwordLists.EnglishStopWords60;
import misc.stopwordLists.SwedishStopWords60;

import java.text.Normalizer;
import java.util.*;

/**
 * Created by crco0001 on 10/2/2017.
 */
public class ClarivateToJsat {


    static SwedishStopWords60 swedishStopWords60 = new SwedishStopWords60();
    static EnglishStopWords60 englishStopWords60 = new EnglishStopWords60();

    static SwedishStemmer swedishStemmer = new SwedishStemmer();
    static EnglishStemmer englishStemmer = new EnglishStemmer();

    public static List<String> makeFeaturesFromHost(String host) {


        if(host == null) return Collections.emptyList();


        String temp = Normalizer.normalize(host, Normalizer.Form.NFD);
        temp = temp.replaceAll("[^\\p{ASCII}]","");
        temp = temp.replaceAll("[^a-zA-Z]"," ");
        temp = temp.toLowerCase();
        String[] temp2 = temp.split("\\s");

        HashSet<String> set = new HashSet<>();
        for(String term : temp2) {

            if(!HelperFunctions.hostStopwords.contains(term) && term.length() >= 3) set.add(  SwePubParser.prefixFromHost.concat(term)  );

        }

        return new ArrayList<>(set);

    }

    public static List<String> makeFeaturesFromAffiliation(List<String> affiliations) {

        if(affiliations == null) return Collections.emptyList();

        HashSet<String> set = new HashSet<>();

        for(String string :  affiliations) {

            String temp = Normalizer.normalize(string, Normalizer.Form.NFD);
            temp = temp.replaceAll("[^\\p{ASCII}]","");
            temp = temp.replaceAll("[^a-zA-Z]"," ");
            temp = temp.toLowerCase();

            String[] temp2 = temp.split("\\s");

            for(String term : temp2) if(!HelperFunctions.affiliationStopwords.contains(term) && term.length() >= 3) set.add(SwePubParser.prefixFromAffiliation.concat(term));

        }

      return new ArrayList<>(set);

    }

    public static List<String> makeFeaturesFromEngText(String text) {

        if(text == null) return Collections.emptyList();

        List<String> list_eng = SimpleParser.parse(text, true, ClarivateToJsat.englishStopWords60, ClarivateToJsat.englishStemmer);

        List<String> list_eng2 = new ArrayList<>();
        for (String t : list_eng) {
           list_eng2.add(SwePubParser.prefixFromTIABEng.concat(t) ) ;
        }

        return list_eng2;
    }


    public static List<String> makeFeaturesFromSweText(String text) {

        if(text == null) return Collections.emptyList();

        List<String> list_eng = SimpleParser.parse(text, true, ClarivateToJsat.swedishStopWords60, ClarivateToJsat.swedishStemmer);

        List<String> list_eng2 = new ArrayList<>();
        for (String t : list_eng) {
            list_eng2.add(SwePubParser.prefixFromTIABSwe.concat(t) ) ;
        }

        return list_eng2;
    }


    public static List<String> makeFeaturesFromKeywords(String keywordString) {

        if(keywordString == null) return Collections.emptyList();

        //split on ; (should not be the case but errors in registration exists)

        List<String> keywords = new ArrayList<>();

        if (keywordString.contains(";")) {

            String[] splitted = keywordString.split(";"); // TODO to lower?

            for (String s : splitted) keywords.add(SwePubParser.prefixFromKeyWords.concat(s.toLowerCase().trim()));

        } else {


            keywords.add(SwePubParser.prefixFromKeyWords.concat(keywordString.toLowerCase().trim()) );
        }


        return keywords;

    }


    public static String makeFeaturesFromISBN(String ISBNstring) {

        if(ISBNstring == null) return null;
       Record dummy = new Record();
       dummy.setTitle("Dummy title from fake Record");
       String isbn = HelperFunctions.extractAndHyphenateISBN(ISBNstring,true, dummy );

        return isbn; //can be null!
    }


    public static List<String> makeFeaturesFromISSN(String ISSNstring) {

        if(ISSNstring == null) return Collections.emptyList();

        return HelperFunctions.extractISSN(ISSNstring);

    }


    public static String printSparseVector(SparseVector vec) {

        StringBuilder stringbuilder = new StringBuilder();

        Iterator<IndexValue> iter = vec.getNonZeroIterator();
        stringbuilder.append("[");
        while(iter.hasNext()) {

            IndexValue indexValue = iter.next();
            stringbuilder.append(indexValue.getIndex()).append(",");
            stringbuilder.append(indexValue.getValue()).append(" ");

        }
        stringbuilder.append("]");
        return stringbuilder.toString();
    }

    public static List<String> extractTermsFromClarivateRecord(ClarivateRecord record) {


        //TEXT
        String text = record.getTitle();

        if(record.getSummary() != null) {

            String cleanedAbstract = RemoveCopyRightFromAbstract.cleanedAbstract( record.getSummary() );

            text = text.concat(" ").concat( cleanedAbstract );
        }


        List<String> hostTerms = makeFeaturesFromHost( record.getSeries() );
        List<String> textTerms = makeFeaturesFromEngText( text );
        List<String> issnTerms1 = makeFeaturesFromISSN( record.getEissn() );
        List<String> issnTerms2 = makeFeaturesFromISSN( record.getIssn() );
        List<String> keyWordTerms = makeFeaturesFromKeywords( record.getKeywords() );

        String isbnTerms = makeFeaturesFromISBN( record.getIsbn() );


        //TODO analyze Clarivate affiliation strings, not the same as data from swepub..

       List<String> affilTerms = makeFeaturesFromAffiliation( record.getAddressParts() );

        List<String> allTerms = new ArrayList<>();

        allTerms.addAll(hostTerms);
        allTerms.addAll(textTerms);
        allTerms.addAll(issnTerms1);
        allTerms.addAll(issnTerms2);
        allTerms.addAll(keyWordTerms);

        allTerms.addAll(affilTerms);

        if(isbnTerms != null) allTerms.add(isbnTerms);



        return allTerms;

    }




    public static void main(String[] arg){}




}
