package cc.org.web;

import Database.SwePubParser;
import misc.Parsers.SimpleParser;
import misc.Stemmers.EnglishStemmer;
import misc.Stemmers.SwedishStemmer;
import misc.stopwordLists.EnglishStopWords60;
import misc.stopwordLists.SwedishStopWords60;

import java.util.List;

/**
 * Created by crco0001 on 10/2/2017.
 */
public class PlayGround {


    public static void main(String[] arg) {


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
