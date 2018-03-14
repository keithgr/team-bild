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

    private static final String INPUT_PATH = "input/";

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
    private static int[] multFreqs = new int[10];
    private static int test1MultCount = 0, test2MultCount = 0;
    private static int[] twinTests = new int[4];

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
    private static HashMap<String, HashSet<Client>> dayGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> monthGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> yearGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> genderGroupMap = new HashMap<>();
    private static HashMap<String, HashSet<Client>> raceGroupMap = new HashMap<>();

    /**
     * Arrays to count frequency of field matches for various groups
     */
    //array of inividual fields to be compared
    private static String[] matchTypes = {
        "SSN", "FName", "LName", "DoB", "Day", "Month", "Year", "Gender", "Race",
        "Pass1", "Pass2", "Twin1", "Twin2"
    };

    //array of groups to compare within
    private static String[] groupTypes = {
        "SSN", "FName", "LName", "DoB", "Day", "Month", "Year", "Gender", "Race",
        "Pass1", "Pass2", "Twin1", "Twin2"
    };

    //for each group
    //a frequency array for matches in each field
    private static long[][] matchFreqs = new long[groupTypes.length][matchTypes.length];

    //stores clients that have at least one match in a pair of fields
    private static HashSet[][] entryMatches = new HashSet[groupTypes.length][matchTypes.length];

    private static HashSet[] entryComps = new HashSet[groupTypes.length];

    static {
        for (int i = 0; i < groupTypes.length; i++) {
            for (int j = 0; j < matchTypes.length; j++) {
                entryMatches[i][j] = new HashSet();
            }
            entryComps[i] = new HashSet();
        }
    }

    //for each group
    //the total number of comparisons made per field
    private static long[] compFreqs = new long[groupTypes.length];

    //turning on DEBUG will cause data analysis to run through a small sample (1000)
    //of entries to get a quick result
    private static final boolean DEBUG = false;

///////////////////////////
// M A I N   M E T H O D //
///////////////////////////
//
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

        StringBuilder twinsOutput = new StringBuilder(Client.CLIENT_HEADER);

        //for each entry
        while (sc.hasNextLine() && !(DEBUG && scanCount >= 1_000)) {

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

                String race = array[10] + array[11] + array[12]
                        + array[13] + array[14];

                String raceDataQuality = array[15];

                String gender = array[17];

                Client client = new Client(
                        personalId, fName, lName, suffix, nameDataQuality,
                        ssn, ssnDataQuality,
                        dobS, dob, dobDataQuality, gender, race, raceDataQuality,
                        line
                );

                //make comparisons between current client
                //and each previous client
                //          gatherData(client);
                //map client's data to congruence classes
                if (isFullSsn(client)) {
                    mapSsn(client);
                }
                mapFName(client);
                mapLName(client);
                mapDob(client);
                mapDay(client);
                mapMonth(client);
                mapYear(client);
                mapGender(client);
                mapRace(client);

                //add entry to list
                entries.add(client);

                scanCount++;
            }//end special date condition

        }//END SCANNING
        sc.close();

        //GATHER DATA
        for (Client entry : entries) {
            gatherData(entry);
        }

        System.out.println("COUNTING TWINS ...");
        //COUNT TWINS

        for (int i = 0; i < entries.size(); i++) {

            int t1 = 0, t2 = 0;

            for (int j = 0; j < entries.size(); j++) {
                Client entry = entries.get(i);
                Client otherEntry = entries.get(j);
                if (entry != otherEntry && isTwin1(entry, otherEntry)) {
                    t1 = 1;
                    break;
                }
            }
            for (int j = 0; j < entries.size(); j++) {
                Client entry = entries.get(i);
                Client otherEntry = entries.get(j);
                if (entry != otherEntry && isTwin2(entry, otherEntry)) {
                    t2 = 2;
                    break;
                }
            }

            //[0] - Failed both tests
            //[1] - Passed test 1, failed test 2
            //[2] - Failed test 1, passed test 2
            //[3] - Passed both tests
            twinTests[t1 + t2]++;
            if (t1 + t2 > 0) {
                twinsOutput.append(entries.get(i)).append("\n");
            }
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
                System.out.print(entryMatches[g][m].size() + "\t");
            }
            System.out.println("|  " + entryComps[g].size());
        }

        System.out.println("");
        System.out.println("Clients who did not have a twin: " + twinTests[0]);
        System.out.println("Clients who had a twin (1): " + twinTests[1]);
        System.out.println("Clients who had a twin (2): " + twinTests[2]);
        System.out.println("Clients who had a twin (1, 2): " + twinTests[3]);

        printToFile("./output/TwinOutput.csv", twinsOutput.toString());
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

    private static void mapDay(Client entry) {
        String day = entry.getDob().getDayOfMonth() + "";
        HashSet<Client> group = dayGroupMap.get(day);
        if (group == null) {
            group = new HashSet<>();
            dayGroupMap.put(day, group);
        }
        group.add(entry);
    }

    private static void mapMonth(Client entry) {
        String month = entry.getDob().getMonthValue() + "";
        HashSet<Client> group = monthGroupMap.get(month);
        if (group == null) {
            group = new HashSet<>();
            monthGroupMap.put(month, group);
        }
        group.add(entry);
    }

    private static void mapYear(Client entry) {
        String year = entry.getDob().getYear() + "";
        HashSet<Client> group = yearGroupMap.get(year);
        if (group == null) {
            group = new HashSet<>();
            yearGroupMap.put(year, group);
        }
        group.add(entry);
    }

    private static void mapGender(Client entry) {
        String gender = entry.getGender();
        HashSet<Client> group = genderGroupMap.get(gender);
        if (group == null) {
            group = new HashSet<>();
            genderGroupMap.put(gender, group);
        }
        group.add(entry);
    }

    private static void mapRace(Client entry) {
        String race = entry.getRace();
        HashSet<Client> group = raceGroupMap.get(race);
        if (group == null) {
            group = new HashSet<>();
            raceGroupMap.put(race, group);
        }
        group.add(entry);
    }

    //
    //
    // DATA COLLECTING METHODS
    //
    //
    private static void gatherData(Client entry) {
        //
        // COMPARE ALL PAIRS OF THIS CLIENT AND A PREVIOUS CLIENT
        //
        try {
            //compare data fields for entries with matching SSNs
            for (Client otherEntry : ssnGroupMap.get(entry.getSsn())) {
                if (hasSsnMatch(entry.getSsn(), otherEntry)) {
                    compareFields(entry, otherEntry, 0);
                }
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching FirstNames
            for (Client otherEntry : fNameGroupMap.get(entry.getfName())) {
                compareFields(entry, otherEntry, 1);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching LastNames
            for (Client otherEntry : lNameGroupMap.get(entry.getlName())) {
                compareFields(entry, otherEntry, 2);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching DoBs
            for (Client otherEntry : dobGroupMap.get(entry.getDob())) {
                compareFields(entry, otherEntry, 3);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching days of birth
            for (Client otherEntry : dayGroupMap.get(entry.getDob().getDayOfMonth() + "")) {
                compareFields(entry, otherEntry, 4);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching months of birth
            for (Client otherEntry : monthGroupMap.get(entry.getDob().getMonthValue() + "")) {
                compareFields(entry, otherEntry, 5);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching years of birth
            for (Client otherEntry : yearGroupMap.get(entry.getDob().getYear() + "")) {
                compareFields(entry, otherEntry, 6);
            }
        } catch (NullPointerException e) {
            //System.out.println("YEEEEEEEE");
        }

        try {
            //compare data fields for entries with matching gender
            for (Client otherEntry : genderGroupMap.get(entry.getGender())) {
                compareFields(entry, otherEntry, 7);
            }
        } catch (NullPointerException e) {
        }

        try {
            //compare data fields for entries with matching gender
            for (Client otherEntry : raceGroupMap.get(entry.getRace())) {
                compareFields(entry, otherEntry, 8);
            }
        } catch (NullPointerException e) {
        }

        //compare data fields for all successive entries that pass test1 [9]
        for (Client otherEntry : entries) {
            if (isMatch1(entry, otherEntry)) {
                compareFields(entry, otherEntry, 9);
            }
        }

        //compare data fields for all successive entries that pass test2 [10]
        for (Client otherEntry : entries) {
            if (isMatch2(entry, otherEntry)) {
                compareFields(entry, otherEntry, 10);
            }
        }

        /*
        
        //compare data fields for all successive entries that pass twin check 1 [11]
        for (Client otherEntry : entries) {
            if (isTwin1(entry, otherEntry)) {
                compareFields(entry, otherEntry, 11);
            }
        }

        //compare data fields for all successive entries that pass twin check 2 [12]
        for (Client otherEntry : entries) {
            if (isTwin2(entry, otherEntry)) {
                compareFields(entry, otherEntry, 12);
            }
        }
        
        */
        
    }//end gatherData

    private static void compareFields(Client entry, Client otherEntry, int groupId) {

        //if an entry is compared to itself
        //then ignore
        if (entry == otherEntry) {
            return;
        }

        if (hasSsnMatch(entry.getSsn(), otherEntry)) {
            //matchFreqs[groupId][0]++;
            entryMatches[groupId][0].add(entry);
        }
        if (entry.getfName().equals(otherEntry.getfName()) && !entry.getfName().isEmpty()) {
            //matchFreqs[groupId][1]++;
            entryMatches[groupId][1].add(entry);
        }
        if (entry.getlName().equals(otherEntry.getlName()) && !entry.getlName().isEmpty()) {
            //matchFreqs[groupId][2]++;
            entryMatches[groupId][2].add(entry);
        }
        LocalDate dob = entry.getDob(), otherDob = otherEntry.getDob();
        if (dob.equals(otherDob)) {
            //matchFreqs[groupId][3]++;
            entryMatches[groupId][3].add(entry);
        }
        if (dob.getDayOfMonth() == otherDob.getDayOfMonth()) {
            //matchFreqs[groupId][4]++;
            entryMatches[groupId][4].add(entry);
        }
        if (dob.getMonthValue() == otherDob.getMonthValue()) {
            //matchFreqs[groupId][5]++;
            entryMatches[groupId][5].add(entry);
        }
        if (dob.getYear() == otherDob.getYear()) {
            //matchFreqs[groupId][6]++;
            entryMatches[groupId][6].add(entry);
        }
        if (entry.getGender().equals(otherEntry.getGender())) {
            //matchFreqs[groupId][7]++;
            entryMatches[groupId][7].add(entry);
        }
        if (entry.getRace().equals(otherEntry.getRace())) {
            //matchFreqs[groupId][8]++;
            entryMatches[groupId][8].add(entry);
        }
        if (isMatch1(entry, otherEntry)) {
            //matchFreqs[groupId][9]++;
            entryMatches[groupId][9].add(entry);
        }
        if (isMatch2(entry, otherEntry)) {
            //matchFreqs[groupId][10]++;
            entryMatches[groupId][10].add(entry);
        }

        //compFreqs[groupId]++;
        entryComps[groupId].add(entry);
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
        if (isTwin1(client, newClient)) {
            return false;
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
        if (isTwin2(client, newClient)) {
            return false;
        }

        return true;
    }//END isMatch2

    private static boolean isTwin1(Client newClient, Client client) {

        String[] newClientInfo = new String[]{newClient.getfName(), newClient.getlName(), newClient.getDobS()};

        if (newClientInfo[1].equals(client.getlName()) && newClient.getDob().equals(client.getDob())) {
            LocalDate entryDate = ENTRY_DATES.get(newClient.getPersonalId());
            // Age < 18
            if (!newClient.getDob().isBefore(entryDate.minusYears(18))) {
                String client2FName = client.getfName();
                // Different first names
                if (!newClientInfo[0].isEmpty() && !client2FName.isEmpty()
                        && !newClientInfo[0].equals(client2FName)) {
                    return true;
                }
            }
        }

        return false;
    }//end method

    private static boolean isTwin2(Client newClient, Client client) {

        String lName = newClient.getlName();
        LocalDate dob = newClient.getDob();
        String ssn = newClient.getSsn();
        String personalId = newClient.getPersonalId();
        String fName = newClient.getfName();

        if (lName.equals(client.getlName()) && dob.equals(client.getDob())) {
            // Different SSNs
            if (!ssn.equals(client.getSsn())) {
                LocalDate entryDate = ENTRY_DATES.get(personalId);
                // Age < 18
                if (!dob.isBefore(entryDate.minusYears(18))) {
                    String client2FName = client.getfName();
                    // Different first names
                    if (!fName.isEmpty() && !client2FName.isEmpty() && !fName.equals(client2FName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }//end method

    /**
     * If SSNs are equal and SSN is not garbage based on spec
     */
    private static boolean hasSsnMatch(String ssn, Client client) {
        return client.getSsn().equals(ssn) && !"999999999".equals(ssn)
                && !"000000000".equals(ssn) && isFullSsn(client);
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

    private static void printToFile(String filename, String contents)
            throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter pw = new PrintWriter(filename, "UTF-8")) {
            //System.out.println(contents);
            pw.println(contents);
        }
    }

}//end class
