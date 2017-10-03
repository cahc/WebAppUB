package cc.org.web;

/**
 * Created by crco0001 on 10/2/2017.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClarivateRecord {


    private final Pattern EXTRACT_ADDRESSES = Pattern.compile("(?<=]).*?(\\[|$)");

    String title;
    String UT;
    String issn;
    String eissn;
    String isbn;
    String DOI;
    int year;
    String WC;
    String series;
    String language;
    String pubType;
    String keywords;
    String summary;

    String authors;
    String authorsAndAffiliations;

    String reprintInfo;

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getEissn() {
        return eissn;
    }

    public void setEissn(String eissn) {
        this.eissn = eissn;
    }

    public String getDOI() {
        return DOI;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setDOI(String DOI) {
        this.DOI = DOI;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getWC() {
        return WC;
    }

    public void setWC(String WC) {
        this.WC = WC;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPubType() {
        return pubType;
    }

    public void setPubType(String pubType) {
        this.pubType = pubType;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAuthorsAndAffiliations() {
        return authorsAndAffiliations;
    }

    public void setAuthorsAndAffiliations(String authorsAndAffiliations) {
        this.authorsAndAffiliations = authorsAndAffiliations;
    }


    public List<String> getAddressParts() {


        if(this.authorsAndAffiliations == null) return Collections.emptyList();

        List<String> result = new ArrayList<>(5);
        Matcher matcher = EXTRACT_ADDRESSES.matcher( getAuthorsAndAffiliations() );
        if(matcher.find()) {

            result.add(matcher.group());

            while(matcher.find()) {
                result.add(matcher.group());
            }
        }


        return result;

    }




    public String getReprintInfo() {
        return reprintInfo;
    }

    public void setReprintInfo(String reprintInfo) {
        this.reprintInfo = reprintInfo;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }





    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUT() {
        return UT;
    }

    public void setUT(String UT) {
        this.UT = UT;
    }

    @Override
    public String toString() {
        return "ClarivateRecord{" + "title=" + title + ", UT=" + UT + ", issn=" + issn + ", eissn=" + eissn + ", DOI=" + DOI + ", year=" + year + ", WC=" + WC + ", series=" + series + ", language=" + language + ", pubType=" + pubType + ", keywords=" + keywords + ", authors=" + authors + ", authorsAndAffiliations=" + authorsAndAffiliations +", abstract=" + summary  +'}';
    }






    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClarivateRecord other = (ClarivateRecord) obj;
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.UT, other.UT)) {
            return false;
        }
        return true;
    }







}
