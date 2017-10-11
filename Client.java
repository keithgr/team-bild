package research;

import java.time.LocalDate;
import java.util.*;

/**
 * Describes the Client
 */
public class Client {

    //client's data
    private String personalId;
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
    
    private String newPersonalId;
    
    private String line;
    
    
    //client relation to other clients
    private Set<Client> similarClients = new HashSet<>();
    
    
    public Client(String personalId, String fName, String lName, String suffix, String nameDataQuality, String ssn,
                  String ssnDataQuality, String dobS, LocalDate dob, String dobDataQuality, String gender, String line){
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
        this.line = line;
    }
    
    public String getLine(){
        return line;
    }
    
    public String getPersonalId(){
        return personalId;
    }
    
    public void setNewPersonalId(String newPersonalId){
        this.newPersonalId = newPersonalId;
    }
    
    public String getfName(){
        return fName;
    }
    
    public String getlName(){
        return lName;
    }
    
    public String getSuffix(){
        return suffix;
    }
    
    public String getNameDataQuality(){
        return nameDataQuality;
    }
    
    public String getSsn(){
        return ssn;
    }
    
    public String getSsnDataQuality(){
        return ssnDataQuality;
    }
    
    public String getDobS(){
        return dobS;
    }
    
    public LocalDate getDob(){
        return dob;
    }
    
    public String getDobDataQuality(){
        return dobDataQuality;
    }
    
    public String getGender(){
        return gender;
    }
    
    /**
     * What to output when writing to a CSV per line
     *
     * @return
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(200);
        sb = sb.append(personalId).append(',').append(fName).append(',')
                .append(lName).append(',').append(suffix).append(',')
                .append(nameDataQuality).append(',').append(ssn).append(',')
                .append(ssnDataQuality).append(',').append(dob).append(',')
                .append(dobDataQuality).append(',').append(gender).append(',');
        
        return sb.toString();
    }
}
