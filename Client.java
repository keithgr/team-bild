
package research;

import java.io.*;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientEntrys have a one-to-one association with personalId; however, multiple
 * ClientEntrys may represent the same Client
 *
 * @author Keith Grable
 * @version 2018-02-17
 */
public class Client {
    
    private String personalId;
    private String newPersonalId;

    private String fName;
    private String lName;
    private String suffix;
    private String nameDataQuality;

    private String ssn;
    private String ssnDataQuality;

    private String dobS;
    private LocalDate dob;
    private String dobDataQuality;

    private String gender;

    private String race;
    private String raceDataQuality;

    private String line;
    
    /**
     * Constructs a ClientEntry from individual data fields
     *
     * @constructor
     */
    public Client(
            String personalId, String fName, String lName, String suffix, String nameDataQuality, String ssn,
            String ssnDataQuality, String dobS, LocalDate dob, String dobDataQuality, String gender,
            String race, String raceDataQuality, String line
    ) {
        this.personalId = personalId;
        this.fName = fName;
        this.lName = lName;
        this.suffix = suffix;
        this.nameDataQuality = nameDataQuality;
        this.ssn = ssn;
        this.ssnDataQuality = ssnDataQuality;
        this.dobS = dobS;
        this.dob = dob;
        this.dobDataQuality = dobDataQuality;
        this.gender = gender;
        this.race = race;
        this.raceDataQuality = raceDataQuality;
        this.line = line;
    }

    public String getPersonalId() {
        return personalId;
    }

    public void setNewPersonalId(String newPersonalId) {
        this.newPersonalId = newPersonalId;
    }

    public String getfName() {
        return fName;
    }

    public String getlName() {
        return lName;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getNameDataQuality() {
        return nameDataQuality;
    }

    public String getSsn() {
        return ssn;
    }

    public String getSsnDataQuality() {
        return ssnDataQuality;
    }

    public String getDobS() {
        return dobS;
    }

    public LocalDate getDob() {
        return dob;
    }

    public String getDobDataQuality() {
        return dobDataQuality;
    }

    public String getGender() {
        return gender;
    }

    public String getRace() {
        return race;
    }

    public String getRaceDataQuality() {
        return raceDataQuality;
    }

    public String getLine(){
        return line;
    }
    
    public static final String CLIENT_HEADER
            = "PersonalId,First Name,Last Name,Suffix,Name Data Quality,SSN,SSN Data Quality,DoB,DoB Data Quality,Gender\n";

    /**
     * What to output when writing to a CSV per line
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb = sb.append(personalId).append(',').append(fName).append(',')
                .append(lName).append(',').append(suffix).append(',')
                .append(nameDataQuality).append(',').append(ssn).append(',')
                .append(ssnDataQuality).append(',').append(dob).append(',')
                .append(dobDataQuality).append(',').append(gender).append(',');

        return sb.toString();
    }

    private Properties load(FileReader fileReader) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
