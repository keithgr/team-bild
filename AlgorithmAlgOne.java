package research;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Only for test 1 Didn't feel like commenting it again as it is pretty much the same thing
 */
public class AlgorithmAlgOne {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter NEW_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static ArrayList<Client> clients = new ArrayList<>();
    private static HashMap<String, String> matchingClients = new HashMap<>();
    private static HashMap<Client, ArrayList<Client>> map = new HashMap<>();
    
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException{
        final String SPECIAL_DATE = "1/1/1900";
        
        Scanner scanner = new Scanner(new File("Client.csv"), "UTF-8");
        scanner = scanner.useDelimiter("[,\n]");
        String headers = scanner.nextLine();
        
        /**
         * *** Race ****
         */
        int[] raceArray = {10, 11, 12, 13, 14, 15};
        
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            String[] array = line.split(",");
            
            if(array.length == 0){
                break;
            }
            
            String personalId = array[0];
            
            String fName = array[1];
            String lName = array[3];
            
            String suffix = array[4];
            String nameDataQuality = array[5];
            
            String ssn = array[6];
            String ssnDataQuality = array[7];
            
            String dobS = array[8];
            LocalDate dob;
            try{
                dob = LocalDate.parse(dobS, DATE_FORMAT);
            }
            catch(DateTimeParseException e){
                dob = LocalDate.parse(dobS, NEW_DATE_FORMAT);
                dobS = dob.format(DATE_FORMAT);
            }
            
            // Special case
            if(!SPECIAL_DATE.equals(dobS)){
                String dobDataQuality = array[9];
                
                String gender = array[17];
                
                Client client = new Client(personalId, fName, lName, suffix, nameDataQuality, ssn, ssnDataQuality, dobS,
                        dob,
                        dobDataQuality, gender, line, array);
                boolean ifNew = checkTests(client);
                if(ifNew){
                    clients.add(client);
                }
            }
        }
        
        StringBuilder dobSb = new StringBuilder(
                "PersonalId,FirstName,LastName,NameSuffix,NameDataQuality,SSN,SSNDataQuality,DOB,DOBDataQuality,Gender\n");
        StringBuilder genderSb = new StringBuilder(
                "PersonalId,FirstName,LastName,NameSuffix,NameDataQuality,SSN,SSNDataQuality,DOB,DOBDataQuality,Gender\n");
        StringBuilder raceSb = new StringBuilder(headers + "\n");
        
        StringBuilder sb2 = new StringBuilder(headers + "\n");
        
        for(Map.Entry<Client, ArrayList<Client>> entry : map.entrySet()){
            Client key = entry.getKey();
            ArrayList<Client> value = entry.getValue();
            
            sb2 = sb2.append(key.getLine()).append('\n');
            
            boolean dobKeyAdded = false;
            boolean genderKeyAdded = false;
            boolean raceKeyAdded = false;
            
            for(Client client : value){
                sb2 = sb2.append(client.getLine()).append('\n');
                
                if(!key.getDobS().equals(client.getDobS())){
                    if(!dobKeyAdded){
                        dobSb = dobSb.append('\n').append(key.toString()).append('\n');
                        dobKeyAdded = true;
                    }
                    dobSb = dobSb.append(client.toString()).append('\n');
                }
                
                if(!key.getGender().equals(client.getGender())){
                    if(!genderKeyAdded){
                        genderSb = genderSb.append('\n').append(key.toString()).append('\n');
                        genderKeyAdded = true;
                    }
                    genderSb = genderSb.append(client.toString()).append('\n');
                }
                
                /**
                 * *** Race ****
                 */
                String keyLine = key.getLine();
                String[] keyArray = keyLine.split(",");
                StringBuilder keyRacesSb = new StringBuilder();
                
                String clientLine = client.getLine();
                String[] clientArray = clientLine.split(",");
                StringBuilder clientRacesSb = new StringBuilder();
                
                for(int i = raceArray[0]; i <= raceArray[raceArray.length - 1]; i++){
                    keyRacesSb = keyRacesSb.append(keyArray[i]);
                    clientRacesSb = clientRacesSb.append(clientArray[i]);
                }
                
                String keyRaces = keyRacesSb.toString();
                String clientRaces = clientRacesSb.toString();
                
                if(!keyRaces.equals(clientRaces)){
                    if(!raceKeyAdded){
                        raceSb = raceSb.append('\n').append(keyLine).append('\n');
                        raceKeyAdded = true;
                    }
                    raceSb = raceSb.append(clientLine).append('\n');
                }
            }
        }
        
        printToFile("outputAlgOne/ClientOutput.csv", sb2.toString());
        changePersonalIds("outputAlgOne/ClientOutput.csv");
        
        printToFile("outputAlgOne/ClientOutputDOB.csv", dobSb.toString());
        printToFile("outputAlgOne/ClientOutputGender.csv", genderSb.toString());
        printToFile("outputAlgOne/ClientOutputRace.csv", raceSb.toString());
        
        changePersonalIds("outputAlgOne/ClientOutputDOB.csv");
        changePersonalIds("outputAlgOne/ClientOutputGender.csv");
        changePersonalIds("outputAlgOne/ClientOutputRace.csv");
    }
    
    private static boolean checkTests(Client newClient){
        for(Client client : clients){
            if(checkTest1(newClient, client)){
                try{
                    map.get(client).add(newClient);
                    
                }
                catch(NullPointerException e){
                    ArrayList<Client> list = new ArrayList<>();
                    list.add(newClient);
                    map.put(client, list);
                }
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean checkTest1(Client newClient, Client client){
        if(checkSsn(newClient.getSsn(), client)){
            String newClientSsnDataQuality = newClient.getSsnDataQuality();
            String clientSsnDataQuality = client.getSsnDataQuality();
            if(!newClientSsnDataQuality.isEmpty() && !clientSsnDataQuality.isEmpty()){
                if(!checkSsnDataQuality(newClientSsnDataQuality) || !checkSsnDataQuality(clientSsnDataQuality)){
                    return false;
                }
            }
            
            String[] client1Info = {newClient.getfName(), newClient.getlName(), newClient.getDobS()};
            String[] client2Info = {client.getfName(), client.getlName(), client.getDobS()};
            
            int equalCount = countOfEqual(client1Info, client2Info);
            if(equalCount >= 2){
                matchingClients.put(newClient.getPersonalId(), client.getPersonalId());
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean checkSsn(String ssn, Client client){
        return client.getSsn().equals(ssn) && (!"999999999".equals(ssn) && !"000000000".equals(ssn) && !ssn.isEmpty());
    }
    
    private static boolean checkSsnDataQuality(String ssnDataQuality){
        return "1".equals(ssnDataQuality);
    }
    
    private static int countOfEqual(String[] client1Info, String[] client2Info){
        int count = 0;
        for(int i = 0; i < client1Info.length; i++){
            if(client1Info[i].equals(client2Info[i]) && !client1Info[i].isEmpty()){
                count++;
            }
        }
        
        return count;
    }
    
    private static void printToFile(String filename,
                                    String contents) throws FileNotFoundException, UnsupportedEncodingException{
        try(PrintWriter pw = new PrintWriter(filename, "UTF-8")){
            pw.println(contents);
        }
    }
    
    private static void changePersonalIds(String filename) throws FileNotFoundException, UnsupportedEncodingException{
        Scanner scanner = new Scanner(new File(filename), "UTF-8");
        scanner = scanner.useDelimiter("[,\n]");
        
        String headers = scanner.nextLine();
        String[] headersArray = headers.split(",");
        headers = "NewPersonalID," + headers;
        
        int personalIdCol = -1;
        for(int i = 0; i < headersArray.length; i++){
            if("PersonalID".equals(headersArray[i])){
                personalIdCol = i;
                break;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb = sb.append(headers).append('\n');
        
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            
            String[] array = line.split(",");
            String personalId;
            try{
                personalId = array[personalIdCol];
            }
            catch(ArrayIndexOutOfBoundsException e){
                break;
            }
            
            if(matchingClients.containsKey(personalId)){
                personalId = matchingClients.get(personalId);
            }
            
            line = personalId + "," + line;
            sb = sb.append(line).append('\n');
        }
        
        String noExtensionFileName = filename.substring(0, filename.indexOf(".csv"));
        
        printToFile(filename, sb.toString());
    }
}
