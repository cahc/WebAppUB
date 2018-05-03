package cc.org.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author crco0001
 *
 * Parsing "plain text" exported from a search in Web of Knowledge or e-mailed by a saved search
 *
 * Identifiers:
 *
 * FN ISI Export Format  (saved query, e-mail as of 2017-09)
 *
 * FN Clarivate Analytics Web of Science (normal export as of 2017-09)
 *
 *
 *
 */
public class ClarivateParser {

    final static String IDENTIFIER_EXPORTED = "FN Clarivate Analytics Web of Science";
    final static String IDENTIFIER_EXPORTED2 = "FN Thomson Reuters Web of Science";
    final static String IDENTIFIER_EMAILED = "FN ISI Export Format";
    final static int READ_AHEAD_LIMIT = 1048576; // 1MB
    private final List<ClarivateRecord> recordList = new ArrayList<>();

    private final BufferedReader reader;

    public ClarivateParser(BufferedReader br) {


        this.reader = br;


    }


    public List<ClarivateRecord> parse() throws IOException {


        //TODO check that # ER matches # UT

        String line;
        boolean check = false;
        while( (line = this.reader.readLine()) != null) {


            if(line.startsWith("PT ")) {

                //start of record

                ClarivateRecord record = new ClarivateRecord();

                String line2;
                while( (line2 = this.reader.readLine()) != null) {

                    if(line2.equals("ER")) {

                        //end of record

                        recordList.add(record);
                        break;
                    }


                    //get data from taggs of interest


                    if(line2.startsWith("TI ")) {

                        StringBuilder titleBuilder = new StringBuilder();

                        titleBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                titleBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }


                        }



                        record.setTitle( titleBuilder.toString() );
                    }



                    if(line2.startsWith("UT ")) {

                        record.setUT(  (line2.substring(3)).replaceAll("[^\\d]", "" )   ) ;


                    }


                    if(line2.startsWith("SN ")) {

                        record.setIssn( line2.substring(3));


                    }

                    if(line2.startsWith("EI ")) {

                        record.setEissn( line2.substring(3));

                    }


                    if(line2.startsWith("DI ")) {

                        record.setDOI( line2.substring(3));

                    }


                    if(line2.startsWith("PY ")) {

                        record.setYear( Integer.valueOf( line2.substring(3) ) );


                    }

                    if(line2.startsWith("SO ")) {

                        //can span multiple lines


                        StringBuilder seriesBuilder = new StringBuilder();

                        seriesBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                seriesBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setSeries( seriesBuilder.toString() );
                    }


                    if(line2.startsWith("LA ")) {

                        record.setLanguage( line2.substring(3)  );


                    }


                    if(line2.startsWith("BN ")) {

                        record.setIsbn( line2.substring(3)  );


                    }



                    if(line2.startsWith("DT ")) {

                        record.setPubType( line2.substring(3)  );


                    }




                    if(line2.startsWith("DE ")) {

                        //can span multiple lines


                        StringBuilder keywordBuilder = new StringBuilder();

                        keywordBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                keywordBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setKeywords( keywordBuilder.toString() );
                    }



                    if(line2.startsWith("AF ")) {

                        //can span multiple lines

                        StringBuilder authorBuilder = new StringBuilder();

                        authorBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                authorBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setAuthors( authorBuilder.toString() );
                    }



                    if(line2.startsWith("WC ")) {

                        //can span multiple lines


                        StringBuilder subCatBuilder = new StringBuilder();

                        subCatBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                subCatBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setWC( subCatBuilder.toString() );
                    }



                    if(line2.startsWith("C1 ")) {

                        //can span multiple lines


                        StringBuilder authAndAffilBuilder = new StringBuilder();

                        authAndAffilBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                authAndAffilBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setAuthorsAndAffiliations( authAndAffilBuilder.toString() );
                    }






                    if(line2.startsWith("AB ")) {

                        //can span multiple lines


                        StringBuilder summaryBuilder = new StringBuilder();

                        summaryBuilder.append( line2.substring(3) );

                        //Now check if the title spanns multiple lines
                        this.reader.mark(READ_AHEAD_LIMIT);

                        while(true) {

                            String peekNextLine = this.reader.readLine();

                            if(peekNextLine.startsWith("   ")) {
                                //title is over multiple lines
                                summaryBuilder.append(peekNextLine.substring(2));
                                this.reader.mark(READ_AHEAD_LIMIT);

                            } else {

                                //title is single line or all read
                                this.reader.reset();
                                break;
                            }



                        }

                        record.setSummary( summaryBuilder.toString() );
                    }




                    //TODO add reprint author..



                } //inner while loop, i.e., record loop




            } //if TAG is PT i.e., start of record


            //w.println(line);
        } //outer while loop





        return recordList;
    }





    public static boolean identifierFound(String s) {

        return s.contains(IDENTIFIER_EXPORTED) || s.contains(IDENTIFIER_EMAILED) || s.contains(IDENTIFIER_EXPORTED2);


    }




}
