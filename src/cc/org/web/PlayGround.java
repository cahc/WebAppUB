package cc.org.web;

import Database.IndexAndGlobalTermWeights;
import Database.MyOwnException;
import Database.SwePubParser;

import SwePub.Record;
import misc.LanguageTools.HelperFunctions;
import misc.Parsers.SimpleParser;
import misc.Stemmers.EnglishStemmer;
import misc.Stemmers.SwedishStemmer;
import misc.stopwordLists.EnglishStopWords60;
import misc.stopwordLists.SwedishStopWords60;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by crco0001 on 10/2/2017.
 */
public class PlayGround {


    static SwedishStopWords60 swedishStopWords60 = new SwedishStopWords60();
    static EnglishStopWords60 englishStopWords60 = new EnglishStopWords60();

    static SwedishStemmer swedishStemmer = new SwedishStemmer();
    static EnglishStemmer englishStemmer = new EnglishStemmer();

    public static List<String> makeFeaturesFromHost(String host) {



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

        List<String> list_eng = SimpleParser.parse(text, true, PlayGround.englishStopWords60, PlayGround.englishStemmer);

        List<String> list_eng2 = new ArrayList<>();
        for (String t : list_eng) {
           list_eng2.add(SwePubParser.prefixFromTIABEng.concat(t) ) ;
        }

        return list_eng2;
    }


    public static List<String> makeFeaturesFromSweText(String text) {

        List<String> list_eng = SimpleParser.parse(text, true, PlayGround.swedishStopWords60, PlayGround.swedishStemmer);

        List<String> list_eng2 = new ArrayList<>();
        for (String t : list_eng) {
            list_eng2.add(SwePubParser.prefixFromTIABSwe.concat(t) ) ;
        }

        return list_eng2;
    }


    public static List<String> makeFeaturesFromKeywords(String keywordString) {

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


    public static List<String> makeFeaturesFromISBN(String ISBNstring) {

       Record dummy = new Record();
       dummy.setTitle("Dummy title from fake Record");
       HelperFunctions.extractAndHyphenateISBN(ISBNstring,true, dummy );


        return Collections.emptyList();
    }


    public static List<String> makeFeaturesFromISSN(String ISSNstring) {

        return HelperFunctions.extractISSN(ISSNstring);

    }


    public static void main(String[] arg) throws MyOwnException

    {


        SwedishStopWords60 swedishStopWords60 = new SwedishStopWords60();
        EnglishStopWords60 englishStopWords60 = new EnglishStopWords60();

        SwedishStemmer swedishStemmer = new SwedishStemmer();
        EnglishStemmer englishStemmer = new EnglishStemmer();

        String englishString = "Looking at the facts, one could never ting that this shit will fly!";
        String swedishString = "I en annan värld skulle det aldrig bli av! Skriv ett papper eller en artikel!";

        List<String> list_swe = SimpleParser.parse(swedishString, true, swedishStopWords60, swedishStemmer);
        List<String> list_eng = SimpleParser.parse(englishString, true, englishStopWords60, englishStemmer);

        for (String t : list_eng) {
            System.out.println( SwePubParser.prefixFromTIABEng.concat(t) );
        }

        for (String t : list_swe) {
            System.out.println( SwePubParser.prefixFromTIABSwe.concat(t) );
        }



    }

}
