package research;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data program
 *
 * @author Keith Grable
 * @version 2017-10-31
 */
public class DataAnalysis {
    
    private static final String INPUT_PATH = "./";

    //
    //CONSTANTS THAT CONCERN DATES
    //
    /**
     * the standard date format for our data
     */
    private static final DateTimeFormatter DATE_FORMAT
            = DateTimeFormatter.ofPattern("M/d/yyyy");

    /**
     * an atypical date format for our data
     */
    private static final DateTimeFormatter NEW_DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-M-d");

    /**
     * maps personal ids to dates of entry
     */
    private static final HashMap<String, LocalDate> ENTRY_DATES = new HashMap<>();

    /**
     * indicates that DoB was not given
     */
    private static final String SPECIAL_DATE = "1/1/1900";

    //
    //CLIENT MATCHING VARS
    //
    /**
     * dynamic list of ALL entries
     */
    private static ArrayList<Client> entries = new ArrayList<>();

    /**
     * maps personal ids that represent duplicate entries, according to our
     * tests
     */
    private static HashMap<String, String> matchingClients = new HashMap<>();

    /**
     * maps an original client to a list of matching clients
     */
    private static HashMap<Client, ArrayList<Client>> map = new HashMap<>();

    /**
     * frequency array for groups of singles(0), twins(1), triplets(2), ...
     */
    private static int[] multFreqs = new int[1000];
    private static int test1MultCount = 0, test2MultCount = 0;

    //
    // VARS FOR FEATURE ACCURACY
    //
    /**
     * Maps SSN to a list of clients with that SSN Lists the groups of clients
     */
    private static HashMap<String, HashSet<Client>> ssnGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> fNameGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> lNameGroupMap = new HashMap<>();
    private static HashMap<LocalDate, HashSet<Client>> dobGroupMap = new HashMap<>();

    /**
     * Arrays to count frequency of field matches for various groups
     */
    //array of inividual fields to be compared
    private static String[] matchTypes = {
        "SSN", "FName", "LName", "Suffix",
        "DoB", "Day", "Month", "Year",
        "Gender",
        "Pass2"
    };

    //array of groups to compare within
    private static String[] groupTypes = {
        "SSN", "FName", "LName", "DoB", "Fail1"
    };

    //for each group
    //a frequency array for matches in each field
    private static long[][] matchFreqs = new long[groupTypes.length][matchTypes.length];

    //for each group
    //the total number of comparisons made per field
    private static long[] compFreqs = new long[groupTypes.length];

    //turning on DEBUG will cause data analysis to run through a small sample (1000)
    //of entries to get a quick result
    private static final boolean DEBUG = true;

    /**
     * Reads the client.csv file, gathering data about matches
     *
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.UnsupportedEncodingException
     * @main
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        getEntriesDates();

        //reads the file
        Scanner sc = new Scanner(new File(INPUT_PATH + "Client.csv"), "UTF-8");

        System.out.println("READING FROM: " + INPUT_PATH + "Client.csv ...");

        // split it up by commas (considering it is a CSV)
        sc = sc.useDelimiter("[,\n]");

        // column header
        String headers = sc.nextLine();
        StringBuilder sb = new StringBuilder();
        sb = sb.append(headers).append('\n');

        System.out.println("READING ENTRIES ...");

        //for DEBUG
        int scanCount = 0;

        //for each entry
        while (sc.hasNextLine() && !(DEBUG && scanCount > 2000)) {

            String line = sc.nextLine();
            String[] array = line.split(",");

            if (array.length == 0) {
                break;
            }

            String dobS = array[8];

            //if a client has the special
            if (!SPECIAL_DATE.equals(dobS)) {

                String personalId = array[0];

                String fName = array[1];
                String lName = array[3];
                String suffix = array[4];
                String nameDataQuality = array[5];

                String ssn = array[6];
                String ssnDataQuality = array[7];

                LocalDate dob;
                try {
                    dob = LocalDate.parse(dobS, DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    dob = LocalDate.parse(dobS, NEW_DATE_FORMAT);
                    dobS = dob.format(DATE_FORMAT);
                }

                String dobDataQuality = array[9];

                String gender = array[17];

                Client client = new Client(
                        personalId, fName, lName, suffix, nameDataQuality,
                        ssn, ssnDataQuality,
                        dobS, dob, dobDataQuality, gender, line, array
                );

                //map client's data to congruence classes
                if (isFullSsn(client)) {
                    mapSsn(client);
                }
                mapFName(client);
                mapLName(client);
                mapDob(client);

                //add entry to list
                entries.add(client);

                scanCount++;
            }//end special date condition

        }//END SCANNING
        sc.close();

        //BEGIN ANALYSIS
        //
        //for each entry
        for (int e = 0; e < entries.size(); e++) {
            gatherData(entries.get(e), e + 1);
        }

        //DISPLAY RESULTS
        //
        //matchType labels
        System.out.print("\t");
        for (int m = 0; m < matchTypes.length; m++) {
            System.out.print(matchTypes[m] + "\t");
        }
        System.out.println("| CompFreqs");

        for (int g = 0; g < groupTypes.length; g++) {
            System.out.print(groupTypes[g] + "\t");
            for (int m = 0; m < matchTypes.length; m++) {
                System.out.print(matchFreqs[g][m] + "\t");
            }
            System.out.println("|  " + compFreqs[g]);
        }
    }//end main

    //
    //
    // MAPPING METHODS
    //
    //
    private static void mapSsn(Client entry) {
        String ssn = entry.getSsn();
        HashSet<Client> group = ssnGroupMap.get(ssn);
        if (group == null) {
            group = new HashSet<>();
            ssnGroupMap.put(ssn, group);
        }
        group.add(entry);
    }

    private static void mapFName(Client entry) {
        String fName = entry.getfName();
        HashSet<Client> group = fNameGroupMap.get(fName);
        if (group == null) {
            group = new HashSet<>();
            fNameGroupMap.put(fName, group);
        }
        group.add(entry);
    }

    private static void mapLName(Client entry) {
        String lName = entry.getlName();
        HashSet<Client> group = lNameGroupMap.get(lName);
        if (group == null) {
            group = new HashSet<>();
            lNameGroupMap.put(lName, group);
        }
        group.add(entry);
    }

    private static void mapDob(Client entry) {
        LocalDate dob = entry.getDob();
        HashSet<Client> group = dobGroupMap.get(dob);
        if (group == null) {
            group = new HashSet<>();
            dobGroupMap.put(dob, group);
        }
        group.add(entry);
    }

    //
    //
    // DATA COLLECTING METHODS
    //
    //
    private static void gatherData(Client entry, int start) {

        try {
            //compare data fields for entries with matching SSNs
            for (Client otherEntry : ssnGroupMap.get(entry.getSsn())) {
                if (entry != otherEntry) {
                    compareFields(entry, otherEntry, 0);
                }
            }
        } catch (NullPointerException e) {
            //System.out.println("SSSSSSSSSSSSSSSSSSSSSSN");
        }

        try {
            //compare data fields for entries with matching FirstNames
            for (Client otherEntry : fNameGroupMap.get(entry.getfName())) {
                if (entry != otherEntry) {
                    compareFields(entry, otherEntry, 1);
                }
            }
        } catch (NullPointerException e) {
            //System.out.println("FFFFFFFFFFFFFFFFFFFFN");
        }

        try {
            //compare data fields for entries with matching LastNames
            for (Client otherEntry : lNameGroupMap.get(entry.getlName())) {
                if (entry != otherEntry) {
                    compareFields(entry, otherEntry, 2);
                }
            }
        } catch (NullPointerException e) {
            //System.out.println("LLLLLLLLLLLLLLLLLLLN");
        }

        try {
            //compare data fields for entries with matching DoBs
            for (Client otherEntry : dobGroupMap.get(entry.getDob())) {
                if (entry != otherEntry) {
                    compareFields(entry, otherEntry, 3);
                }
            }
        } catch (NullPointerException e) {
            //System.out.println("DDDDDDDDDDDDDDDDDDDDOOOOOOOOOOOOOOOBBBBBBBBB");
        }

        //compare data fields for all successive entries that fail test1 [4]
        for (int oe = start; oe < entries.size(); oe++) {
            Client otherEntry = entries.get(oe);
            if (!isMatch1(entry, otherEntry)) {
                compareFields(entry, otherEntry, 4);
            }
        }

    }//end gatherData

    private static void compareFields(Client entry, Client otherEntry, int groupId) {
        if (hasSsnMatch(entry.getSsn(), otherEntry)) {
            matchFreqs[groupId][0]++;
        }
        if (entry.getfName().equals(otherEntry.getfName()) && !entry.getfName().isEmpty()) {
            matchFreqs[groupId][1]++;
        }
        if (entry.getlName().equals(otherEntry.getlName()) && !entry.getlName().isEmpty()) {
            matchFreqs[groupId][2]++;
        }
        if (!entry.getSuffix().isEmpty() && entry.getSuffix().equals(otherEntry.getSuffix())) {
            matchFreqs[groupId][3]++;
        }
        LocalDate dob = entry.getDob(), otherDob = otherEntry.getDob();
        if (dob.equals(otherDob)) {
            matchFreqs[groupId][4]++;
        }
        if (dob.getDayOfMonth() == otherDob.getDayOfMonth()) {
            matchFreqs[groupId][5]++;
        }
        if (dob.getMonthValue() == otherDob.getMonthValue()) {
            matchFreqs[groupId][6]++;
        }
        if (dob.getYear() == otherDob.getYear()) {
            matchFreqs[groupId][7]++;
        }
        if (entry.getGender().equals(otherEntry.getGender())) {
            matchFreqs[groupId][8]++;
        }
        if (isMatch2(entry, otherEntry)) {
            matchFreqs[groupId][9]++;
        }

        compFreqs[groupId]++;
    }

    /**
     * Run test 1
     *
     *
     *
     * @return true if a match is detected false if not
     *
     */
    private static boolean isMatch1(Client newClient, Client client) {

        //if SSNs are NOT: equal, full, and valid values
        //then there is no match (test 1)
        if (!hasSsnMatch(newClient.getSsn(), client)) {
            return false;
        }

        //ssns are complete and equal
        //now check for at least two matches
        String[] newClientInfo = new String[]{newClient.getfName(), newClient.getlName(), newClient.getDobS()};
        String[] clientInfo = new String[]{client.getfName(), client.getlName(), client.getDobS()};

        int equalCount = countOfEqual(newClientInfo, clientInfo);
        if (equalCount < 2) {
            return false;
        }

        //
        //
        // TWIN CHECK for isMatch1
        // For test 1, we remove the requirement that SSNs are distinct,
        // as SSNs must match in order to reach the match 1 twin check
        //
        //
        // Same birthdays and last names
        if (newClientInfo[1].equals(client.getlName()) && newClient.getDob().equals(client.getDob())) {
            LocalDate entryDate = ENTRY_DATES.get(newClient.getPersonalId());
            // Age < 18
            if (newClient.getDob().isBefore(entryDate.minusYears(18))) {
                String client2FName = client.getfName();
                // Different first names
                if (!newClientInfo[0].isEmpty() && !client2FName.isEmpty()
                        && !newClientInfo[0].equals(client2FName)) {

                    /*
                     //twins have been found
                     //update count and twin status accordingly
                     multFreqs[client.numMultiples.val]--;
                     client.numMultiples.val++;
                     newClient.numMultiples = client.numMultiples;
                     multFreqs[client.numMultiples.val]++;

                     test1MultCount++;
                     //System.out.println("TWIN 1");
                     */
                    return false;
                }
            }
        }
        return true;

    }//END isMatch1

    private static boolean isMatch2(Client newClient, Client client) {
        //
        //
        // First set of criteria (look for matching)
        //
        //
        String fName = newClient.getfName();
        String lName = newClient.getlName();
        String dobS = newClient.getDobS();
        String ssn = newClient.getSsn();
        String personalId = newClient.getPersonalId();
        String ssnDataQuality = newClient.getSsnDataQuality();

        String[] client1Info = {fName, lName, dobS,};
        String[] client2Info = {client.getfName(), client.getlName(), client.getDobS()};

        //ssn equality test
        //stepA is true if ssns are complete and matching
        boolean stepA = false;
        if (hasSsnMatch(ssn, client)) {

            String clientSsnDataQuality = client.getSsnDataQuality();

            //if both ssns are complete
            if (isFullSsn(newClient) && isFullSsn(client)) {
                stepA = true;
            }

        }

        // How equal are they? Has potential if at least two fields match
        boolean stepB = countOfEqual(client1Info, client2Info) >= 2;

        //if first set fails to find a match
        if (!(stepA || stepB)) {
            return false;
        }

        //
        //
        // Second set of criteria (look for distinctions)
        //
        //
        String gender = newClient.getGender();
        String suffix = newClient.getSuffix();
        LocalDate dob = newClient.getDob();

        //success for stepA AND stepB indicates a HARD MATCH
        //only look for distinctions if we dont have a HARD MATCH
        if (!(stepA && stepB)) {

            client1Info = new String[]{
                gender,
                suffix,
                String.valueOf(dob.getDayOfMonth()),
                String.valueOf(dob.getMonthValue()),
                String.valueOf(dob.getYear())
            };

            LocalDate client2Dob = client.getDob();
            client2Info = new String[]{
                client.getGender(),
                client.getSuffix(),
                String.valueOf(client2Dob.getDayOfMonth()),
                String.valueOf(client2Dob.getMonthValue()),
                String.valueOf(client2Dob.getYear())
            };

            if (countOfInequal(client1Info, client2Info) >= 2) {
                return false;
            }

        }

        //
        //
        // Third set of criteria (TWIN CHECK)
        //
        //
        // Same birthdays and last names
        if (lName.equals(client.getlName()) && dob.equals(client.getDob())) {
            // Different SSNs
            if (!ssn.equals(client.getSsn())) {
                LocalDate entryDate = ENTRY_DATES.get(personalId);
                // Age < 18
                if (dob.isBefore(entryDate.minusYears(18))) {
                    String client2FName = client.getfName();
                    // Different first names
                    if (!fName.isEmpty() && !client2FName.isEmpty() && !fName.equals(client2FName)) {

                        /*
                         //twins have been found
                         //update count and twin status accordingly
                         multFreqs[client.numMultiples.val]--;
                         client.numMultiples.val++;
                         newClient.numMultiples = client.numMultiples;
                         multFreqs[client.numMultiples.val]++;

                         test2MultCount++;
                         //System.out.println("TWIN 2");
                         */
                        return false;
                    }
                }
            }
        }

        return true;
    }//END isMatch2

    /**
     * If SSNs are equal and SSN is not garbage based on spec
     */
    private static boolean hasSsnMatch(String ssn, Client client) {
        return client.getSsn().equals(ssn) && !"999999999".equals(ssn)
                && !"000000000".equals(ssn) && !ssn.isEmpty();
    }

    /**
     * If SsnDataQuality is good based on algorithm
     */
    private static boolean isFullSsn(Client client) {
        return "1".equals(client.getSsnDataQuality())
                && !"999999999".equals(client.getSsn())
                && !"000000000".equals(client.getSsn())
                && !client.getSsn().isEmpty();
    }

    /**
     * Count how many times items in array are equal client1Info[i] ==
     * client2Info[i]
     */
    private static int countOfEqual(String[] client1Info, String[] client2Info) {
        int count = 0;
        for (int i = 0; i < client1Info.length; i++) {
            if (client1Info[i].equals(client2Info[i]) && !client1Info[i].isEmpty()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Count how many times items in array are not equal client1Info[i] ==
     * client2Info[i]
     *
     * If one field is blank, then there are no conflicts
     */
    private static int countOfInequal(String[] client1Info, String[] client2Info) {
        int count = 0;
        for (int i = 0; i < client1Info.length; i++) {
            if (!client1Info[i].equals(client2Info[i])
                    && !client1Info[i].isEmpty()
                    && !client2Info[i].isEmpty()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Get entry dates for everyone mapping PID to the date of entry
     */
    private static void getEntriesDates() {
        //HashMap<String, LocalDate> map = new HashMap<>();

        boolean flag = true;

        Scanner sc = null;
        try {
            sc = new Scanner(new File(INPUT_PATH + "Enrollment.csv"), "UTF-8");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Algorithm1.class.getName()).log(Level.SEVERE, null, ex);
        }
        sc = sc.useDelimiter("[,\n]");
        sc.nextLine();

        int count = 0;

        while (sc.hasNextLine()) {

            String line = sc.nextLine();
            String[] array = line.split(",");

            if (array.length == 0) {
                break;
            }

            String projectEntry = array[0];

            String personalId = array[1];

            String entryDateS = array[3];

            LocalDate entryDate;
            try {
                entryDate = LocalDate.parse(entryDateS, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                entryDate = LocalDate.parse(entryDateS, NEW_DATE_FORMAT);
            }

            ENTRY_DATES.put(personalId, entryDate);

        }

        sc.close();

        //return map;
    }

}//end class
