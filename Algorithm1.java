package research;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main program
 *
 * @author Hamza Memon, Keith Grable, Ting Liu
 * @version 2017-10-04
 * @version 2018-02-28
 */
public class Algorithm1 {

    private static final String INPUT_PATH = "input/";
    private static final String OUTPUT_PATH = "output-D(stay-hh-conflict)/";

    //
    //CONSTANTS THAT CONCERN DATES
    //
    /**
     * the standard date format for our data
     */
    private static final DateTimeFormatter DATE_FORMAT
            = DateTimeFormatter.ofPattern("MM/dd/yyyy"); //02/26/2018 by TL

    /**
     * an atypical date format for our data
     */
    private static final DateTimeFormatter NEW_DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //02/26/2018 by TL

    /**
     * All enrollment dates: needed for twin checking
     */
    private static final HashMap<String, ArrayList<LocalDate>> ENTRY_DATES = new HashMap<>();

    /**
     * maps personal ids to reliable dates of entry and exit
     */
    private static final HashMap<String, ArrayList<LocalDate>> EXIT_DATES = new HashMap<>();

    /**
     * The set of all reliable project exit ids
     */
    private static final HashMap<String, LocalDate> EXIT_IDS = new HashMap<>();

    private static HashSet<String> stayConflictClientsOne = new HashSet();

    private static HashSet<String> houseHoldIDConflictClientsOne = new HashSet();
    /**
     * maps personal ids to destination code of exit
     */
    private static final HashMap<String, ArrayList<LocalDate>> EXIT_CODES = new HashMap<>();

    /**
     * maps personal ids to household ids
     */
    private static final HashMap<String, String> HOUSEHOLD_IDS = new HashMap<>();

    /**
     * indicates that DoB was not given
     */
    private static final String SPECIAL_DATE = "1/1/1900";

    //
    //CLIENT MATCHING VARS
    //
    /**
     * dynamic list of unique clients
     */
    private static ArrayList<Client> clients = new ArrayList<>();

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
     * Maps SSN to a list of clients with that SSN Lists the groups of clients
     */
    private static HashMap<String, ArrayList<Client>> ssnGroupMap = new HashMap<>();
    private static ArrayList<ArrayList<Client>> ssnGroupList = new ArrayList<>();

    /**
     * frequency array for groups of singles(0), twins(1), triplets(2), ...
     */
    private static int[] multFreqs = new int[1000];
    private static int test1MultCount = 0, test2MultCount = 0;

    /**
     * if false, then skip writing to output
     */
    private static final boolean WRITE = true;

    //
    // VARS FOR FEATURE ACCURACY
    //
    /**
     * isMatch1 is assumed to be accurate
     *
     * [0] both matches fail [1] match1 succeeds, match2 fails (false negative)
     * [2] match1 fails, match2 succeeds (false positive) [3] both matches
     * succeed
     */
    private static long[] test2Acc = new long[4];
    private static long totalMatchCount = 0;
    private static long[] twinTests = new long[4];

    /**
     * Counts the number of matches that were rejected due to stay conflict
     */
    private static long stayConflictCountOne = 0; //02/26/18 by TL
    private static long stayConflictCountTwo = 0; //02/26/18 by TL
    /**
     * Counts the number of matches that were rejected due to same household id
     */
    private static long sameHouseHoldOne = 0; //02/27/18 by TL
    private static long sameHouseHoldTwo = 0; //02/27/18 by TL

    private static long twinCheckOne = 0; //02/26/18 by TL

    private static long twinCheckTwo = 0; //02/26/18 by TL

    private static long countOfUnique = 0;

    /**
     * Reads the client.csv file De-duplicates entries Writes the new list of
     * entries to an output file
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.UnsupportedEncodingException
     * @main
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        getEnrollmentData();

        //reads the file
        Scanner sc = new Scanner(new File(INPUT_PATH + "Client.csv"), "UTF-8");

        System.out.println("READING FROM: " + INPUT_PATH + "Client.csv ...");

        // split it up by commas (considering it is a CSV)
        sc = sc.useDelimiter("[,\n]");

        // column header
        String headers = sc.nextLine();
        StringBuilder sb = new StringBuilder();
        sb = sb.append(headers).append('\n');

        System.out.println("DEDUPLICATING ...");

        int scanCount = 0;

        StringBuilder twinsOutput = new StringBuilder(Client.CLIENT_HEADER);

        //for each entry
        while (sc.hasNextLine()) {

            if (scanCount % 1000 == 0) {
                System.out.println(scanCount);
            }

            String line = sc.nextLine();
            String[] array = line.split(",");

            if (array.length == 0) {
                break;
            }

            String dobS = array[8];

            //if a client has the special
            if (!SPECIAL_DATE.equals(dobS)) {

                String personalId = array[0];

                ArrayList<LocalDate> clientEntryDates = ENTRY_DATES.get(personalId),
                        clientExitDates = EXIT_DATES.get(personalId);

                /*
                 System.out.println(personalId);
                 System.out.println(clientEntryDates);
                 System.out.println(clientExitDates);
                
                
                 if (clientEntryDates != null) {
                 for (int i = 0; scanCount < 5000 && i < clientEntryDates.size(); i++) {
                 System.out.println(ChronoUnit.DAYS.between(clientEntryDates.get(i), clientExitDates.get(i)));
                 //System.out.print(ChronoUnit.DAYS.between(clientEntryDates.get(i), clientExitDates.get(i)) + "           ");
                 scanCount++;
                 }
                 }

                
                 */
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

                //if client is new, then add client to dynamic list
                boolean isNew = isNewClient(client);
                //if (isNew) {
                clients.add(client);
                //}

                if (isNew) {
                    countOfUnique++;
                }

                scanCount++;

            }//end special date condition

        }//END WHILE

        sc.close();

        System.out.println("Count of unique clients = " + countOfUnique);
        System.out.println("Number of stay conflicts found in T1 = " + stayConflictCountOne); //02/26/18 by TL
        System.out.println("Number of stay conflicts found in T2 = " + stayConflictCountTwo); //02/26/18 by TL
        System.out.println("Number of Household conflicts found in T1 = " + sameHouseHoldOne); //02/27/18 by TL
        System.out.println("Number of Household conflicts found in T2 = " + sameHouseHoldTwo); //02/27/18 by TL
        System.out.println("twins found in test one: " + twinCheckOne); //02/26/18 by TL
        System.out.println("twins found in test two: " + twinCheckTwo); //02/26/18 by TL


        //an arraylist to store accurate entries that will replace inaccurate entries
        ArrayList<Client> replacements = new ArrayList<>();
        ArrayList<ArrayList<Client>> groups = new ArrayList<>();

        //among a duplicate group
        //we should select an entry with the most common DoB
        for (Client firstClient : map.keySet()) {

            //get duplicate group
            ArrayList<Client> group = map.get(firstClient);

            //determine first entry with majority DoB
            Client majDob = getClientWithMajDob(group);

            replacements.add(firstClient);
            replacements.add(majDob);
            groups.add(group);
        }

        for (int i = 0; i < groups.size(); i++) {

            //remove first client
            map.put(replacements.get(2 * i), null);

            //map majDob client
            map.put(replacements.get(2 * i + 1), groups.get(i));
        }

        if (WRITE) {

            System.out.println("WRITING OUTPUT FILES ...");
            // A CSV provided by HUD

            // Get all other files
            File[] files = new File(INPUT_PATH).listFiles();
            for (File file : files) {
                String filename = file.getName();
                // Get all CSVs
                if (filename.endsWith(".csv")) {
                    String noExtension = filename.substring(0, filename.indexOf(".csv"));
                    // A CSV provided by HUD

                    if (!noExtension.endsWith("Output")) {
                        System.out.print("Writing to " + OUTPUT_PATH + noExtension + "Output.csv ... ");
                        changePersonalIds(filename);
                        System.out.println("DONE");
                    }

                }
            }//end file loop

        }

        /*
        
        System.out.println("");
        System.out.println("Clients who did not have a twin: " + twinTests[0]);
        System.out.println("Clients who had a twin (1): " + twinTests[1]);
        System.out.println("Clients who had a twin (2): " + twinTests[2]);
        System.out.println("Clients who had a twin (1, 2): " + twinTests[3]);

         */
    }//end main

    /**
     * Determine if two clients have any conflicting stay time in distinct
     * projects
     */
    private static boolean hasStayConflict(
            ArrayList<LocalDate> enrollments1, ArrayList<LocalDate> exits1,
            ArrayList<LocalDate> enrollments2, ArrayList<LocalDate> exits2
    ) {
        if (enrollments1 == null || enrollments2 == null) {
            return false;
        }
        //02/26/18 by TL
        if (exits1 == null) {
            exits1 = new ArrayList();
            LocalDate exit = LocalDate.of(0000, 01, 01);
            exits1.add(exit);
        }
        if (exits2 == null) {
            exits2 = new ArrayList();
            LocalDate exit = LocalDate.of(0000, 01, 01);
            exits2.add(exit);
        }
        //end adding
        for (int i = 0; i < enrollments1.size(); i++) {
            LocalDate en1 = enrollments1.get(i), ex1 = exits1.get(i);
            for (int j = 0; j < enrollments2.size(); j++) {
                LocalDate en2 = enrollments2.get(j), ex2 = exits2.get(j);

                if (en2.isBefore(en1)) {
                    if (ex2.isAfter(en1)) {
                        return true;       // en2 and ex2 surround en1
                    }
                } else if (en2.isBefore(ex1)) {
                    return true;           // en2 is between en1 and ex1
                }
                /*
                 //02/26/18 by TL
                 if (ex1.isBefore(ex2)) {
                 if (ex1.isAfter(en2)) {
                 return true;
                 }
                 //en1 and ex1 suround ex2    
                 } else if (en1.isBefore(ex2)) {
                 return true;
                 }
                 */
                //end adding
            }
        }

        return false;

    }

    /**
     * Determine, from a list of clients, the client with the mode DoB
     *
     * @param grp A list of clients, representing a duplicate group
     *
     * @return The first client with the most common DoB
     */
    private static Client getClientWithMajDob(List<Client> grp) {
        //frequency map of the dobs
        HashMap<LocalDate, Integer> dobFreqs = new HashMap<>();

        //count dob freqs
        for (Client c : grp) {
            dobFreqs.putIfAbsent(c.getDob(), 0);
            Integer f = dobFreqs.get(c.getDob());
            f++;
        }

        //find most frequent dob
        int maxF = -1;
        LocalDate majDob = null;
        for (LocalDate ld : dobFreqs.keySet()) {
            if (dobFreqs.get(ld) > maxF) {
                majDob = ld;
                maxF = dobFreqs.get(ld);
            }
        }

        //find first client with
        for (Client c : grp) {
            if (c.getDob().equals(majDob)) {
                return c;
            }
        }

        //this case should not be reached
        return null;
    }

    /**
     * Run the algorithm
     *
     * @param newClient the current line of the file
     *
     * @return if newClient is new (true) or not (false)
     */
    private static boolean isNewClient(Client newClient) {

        // KG 2018-03-13
        // Assume that the client is new until a match is found
        // Both tests will execute to completion
        boolean duplicateFound = false;

        //for each previously entered client
        for (Client otherClient : clients) {

            //if the clients match
            if (isMatch1(newClient, otherClient)) {

                //clients did not fail any of the match criteria
                matchingClients.put(newClient.getPersonalId(), otherClient.getPersonalId());

                // Add newClient to an ArrayList for the main client
                ArrayList<Client> temp = map.get(otherClient);
                if (temp == null) {
                    temp = new ArrayList<>();
                    map.put(otherClient, temp);
                }
                temp.add(newClient);

                duplicateFound = true;
            }

            if (isMatch2(newClient, otherClient)) {

                // Link the main client's PID to the Client
                matchingClients.put(newClient.getPersonalId(), otherClient.getPersonalId());

                // Same thing as before
                ArrayList<Client> temp = map.get(otherClient);
                if (temp == null) {
                    temp = new ArrayList<>();
                    map.put(otherClient, temp);
                }
                temp.add(newClient);

                duplicateFound = true;

            }

        }//end prev client loop

        return !duplicateFound;
    }

    /**
     * Run test 1
     *
     *
     *
     * @return true if a match is detected false if not
     *
     */
    static boolean isMatch1(Client newClient, Client client) {

        // KG 2018-03-14
        // Assume that the client is a match, until it is rejected by a filter
        boolean matchFailed = false;

        //reject match if ssns are different
        if (!hasSsnMatch(newClient.getSsn(), client)) {
            matchFailed = true;
        }

        /*
        String[] newClientInfo = {newClient.getfName(), newClient.getlName(),
            newClient.getDob().toString()};
        String[] clientInfo = {client.getfName(), client.getlName(),
            client.getGender(), client.getDob().toString()};
         */
        String[] newClientInfo = {newClient.getfName(), newClient.getlName(),
            newClient.getGender(), newClient.getDob().getMonth() + ""};
        String[] clientInfo = {client.getfName(), client.getlName(),
            client.getGender(), client.getDob().getMonth() + ""};

        //number of matches for individual fields
        int fieldMatchCount = countOfEqual(newClientInfo, clientInfo);

        if (fieldMatchCount < 2) {
            matchFailed = true;
        }

        //
        //
        // TWIN CHECK for isMatch1
        //
        //
        if (isTwin1(newClient, client)) {
            twinCheckOne++; //02/26/18 by TL
            matchFailed = true;
        }

        
        //moved from the top of the method to the bottom 02/26/18 by TL
        // Stay conflict filter
        //
        // If two clients have a conflict in stay times at different projects,
        // then they cannot be duplicates
        //
        //        System.out.println("personal ID: " + newClient.getPersonalId());
        if (hasStayConflict(
                ENTRY_DATES.get(newClient.getPersonalId()),
                EXIT_DATES.get(newClient.getPersonalId()),
                ENTRY_DATES.get(client.getPersonalId()),
                EXIT_DATES.get(client.getPersonalId())
        )) {
            stayConflictClientsOne.add(client.getPersonalId() + "_" + newClient.getPersonalId());
            stayConflictCountOne++;
            matchFailed = true;
        }

        
        
        String newHouseholdId = HOUSEHOLD_IDS.get(newClient.getPersonalId());
        String houseHoldId = HOUSEHOLD_IDS.get(client.getPersonalId());

        //if both entries have non-empty equal HHIDS, then they are not
        //the same client
        if (!newHouseholdId.equals("\"\"") && newHouseholdId.equals(houseHoldId)) {
            //System.out.println("same household ID: \n" + newClient.toString()
            //        + "\n" + client.toString());
            houseHoldIDConflictClientsOne.add(client.getPersonalId() + "_" + newClient.getPersonalId());
            sameHouseHoldOne++;
            matchFailed = true;
        }
/*
         */
        return !matchFailed;

    }//END isMatch1

    static boolean isMatch2(Client newClient, Client client) {

        // KG 2018-03-11
        // Assume that the client is a match, until it is rejected by a filter
        boolean matchFailed = false;

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
            if (isFullSsn(ssnDataQuality) && isFullSsn(clientSsnDataQuality)) {
                stepA = true;
            }

        }

        // How equal are they? Has potential if at least two fields match
        boolean stepB = countOfEqual(client1Info, client2Info) >= 2;

        //if first set fails to find a match
        if (!(stepA || stepB)) {
            matchFailed = true;
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
                matchFailed = true;
            }

        }

        //
        //
        // Third set of criteria (TWIN CHECK)
        //
        //
        if (isTwin2(client, newClient)) {
            twinCheckTwo++; //02/26/18 by TL
            matchFailed = true;
        }
        
        //02/26/18 by TL
        // Stay conflict filter
        //
        // If two clients have a conflict in stay times at different projects,
        // then they cannot be duplicates
        //
//        System.out.println("personal ID: " + newClient.getPersonalId());
        if (hasStayConflict(
                ENTRY_DATES.get(newClient.getPersonalId()),
                EXIT_DATES.get(newClient.getPersonalId()),
                ENTRY_DATES.get(client.getPersonalId()),
                EXIT_DATES.get(client.getPersonalId())
        )) {
            if (!stayConflictClientsOne.contains(client.getPersonalId() + "_" + newClient.getPersonalId())) {
                stayConflictCountTwo++;
            }
            matchFailed = true;
        }
        
        
        
        String newHouseholdId = HOUSEHOLD_IDS.get(newClient.getPersonalId());
        String houseHoldId = HOUSEHOLD_IDS.get(client.getPersonalId());

        //if both entries have non-empty equal HHIDS, then they are not
        //the same client
        if (!newHouseholdId.equals("\"\"") && newHouseholdId.equals(houseHoldId)) {
            if (houseHoldIDConflictClientsOne.contains(client.getPersonalId() + "_" + newClient.getPersonalId())) {
                sameHouseHoldTwo++;
            }
            matchFailed = true;
        }

         
        return !matchFailed;

    }//END isMatch2

    /**
     * Determines whether two clients are considered to be twins, based on match
     * test 1
     */
    private static boolean isTwin1(Client newClient, Client client) {

        String lName = newClient.getlName();
        LocalDate dob = newClient.getDob();
        String ssn = newClient.getSsn();
        String personalId = newClient.getPersonalId();
        String fName = newClient.getfName();

        if (lName.equals(client.getlName()) && dob.equals(client.getDob())) {
            // Matching SSNs
            if (ssn.equals(client.getSsn())) {
                //get the oldest entry
                LocalDate entryDate = ENTRY_DATES.get(personalId).get(0);
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
     * Determines whether two clients are considered to be twins, based on match
     * test 2
     */
    private static boolean isTwin2(Client newClient, Client client) {

        String lName = newClient.getlName();
        LocalDate dob = newClient.getDob();
        String ssn = newClient.getSsn();
        String personalId = newClient.getPersonalId();
        String fName = newClient.getfName();

        if (lName.equals(client.getlName()) && dob.equals(client.getDob())) {
            // Different SSNs
            if (!ssn.equals(client.getSsn())) {
                //get the oldest entry
                LocalDate entryDate = ENTRY_DATES.get(personalId).get(0);
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
        return client.getSsn().equals(ssn) //codes are equal
                && "1".equals(client.getSsnDataQuality()) //ssns are full //02/26/18 modified by TL by relacing "ssn" with current method
                && !"999999999".equals(ssn) //ssns are valid numbers
                && !"000000000".equals(ssn);
    }

    /**
     * If SsnDataQuality is good based on algorithm
     */
    private static boolean isFullSsn(String ssnDataQuality) {
        return "1".equals(ssnDataQuality);
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
     *
     * Gets household ids
     */
    private static void getEnrollmentData() {

        //
        // Read exit dates from
        // Exit.csv
        // 
        // Validate the exit data
        //
        Scanner sc = null;
        try {
            sc = new Scanner(new File(INPUT_PATH + "Exit.csv"), "UTF-8");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Algorithm1.class.getName()).log(Level.SEVERE, null, ex);
        }
        sc = sc.useDelimiter("[,\n]");
        sc.nextLine();

        while (sc.hasNextLine()) {

            String line = sc.nextLine();
            String[] array = line.split(",");

            if (array.length == 0) {
                break;
            }

            String exitId = array[0];
            String personalId = array[2];
            String exitDateS = array[3].trim();
            String destination = array[4].trim();
            LocalDate exitDate = null;
            //02/27/18 added by TL
            if (destination.equals("30")) {
//                System.out.println("find a wrong exit date: " + exitDateS);
                exitDate = LocalDate.of(0000, 01, 01);
            } else {
                try {
                    exitDate = LocalDate.parse(exitDateS, DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    exitDate = LocalDate.parse(exitDateS, NEW_DATE_FORMAT);
                }
            }
            //done adding

            /* Map exit id to exit date to be referenced by enrollment entries */
            EXIT_IDS.put(exitId, exitDate);

        }

        //
        // Read enrollment dates and household ids from
        // Enrollment.csv
        //
        sc = null;
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

            String entryId = array[0];
            String personalId = array[1].trim();
            String entryDateS = array[3].trim();
            String houseHoldId = array[4];

            LocalDate entryDate = null;
            try {
                entryDate = LocalDate.parse(entryDateS, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                entryDate = LocalDate.parse(entryDateS, NEW_DATE_FORMAT);
            }

            ArrayList<LocalDate> eds = ENTRY_DATES.get(personalId);
            if (eds == null) {
                eds = new ArrayList<>();
                ENTRY_DATES.put(personalId, eds);
            }
            eds.add(entryDate);

            /* Associate entry date with corresponding exit date, if an exit date exists */
            LocalDate exitDate = EXIT_IDS.get(entryId);
            if (exitDate != null) {

                /* Map personal id to exit date with matching entry/exit id */
                ArrayList<LocalDate> clientExitDates = EXIT_DATES.get(personalId);
                if (clientExitDates == null) {
                    clientExitDates = new ArrayList<>();
                    EXIT_DATES.put(personalId, clientExitDates);
                }
                clientExitDates.add(entryDate);

            } else {

                /* Map personal id to exit date that equals entry date */
                ArrayList<LocalDate> clientExitDates = EXIT_DATES.get(personalId);
                if (clientExitDates == null) {
                    clientExitDates = new ArrayList<>();
                    EXIT_DATES.put(personalId, clientExitDates);
                }
                clientExitDates.add(entryDate);

            }

            HOUSEHOLD_IDS.put(personalId, houseHoldId);

        }

        sc.close();

    }

    /**
     * Duh
     */
    private static void printToFile(String filename, String contents)
            throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter pw = new PrintWriter(filename, "UTF-8")) {
            //System.out.println(contents);
            pw.println(contents);
        }
    }

    private static void changePersonalIds(String filename)
            throws FileNotFoundException, UnsupportedEncodingException {

        String noExtensionFileName = filename.substring(0, filename.indexOf(".csv"));
        String outputFilename = OUTPUT_PATH + noExtensionFileName + "Output.csv";

        /* Add output directory if it does not exist */
        File outputDir = new File(OUTPUT_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        /* Output results */
        try (PrintWriter pw = new PrintWriter(outputFilename, "UTF-8")) {

            Scanner sc = new Scanner(new File(INPUT_PATH + filename), "UTF-8");
            sc = sc.useDelimiter("[,\n]");

            String headers = sc.nextLine();
            String[] headersArray = headers.split(",");

            int personalIdCol = -1;
            for (int i = 0; i < headersArray.length; i++) {
                // Is there even a PersonalID column (lowercase because it's spelled differently)
                if ("personalid".equals(headersArray[i].toLowerCase())) {
                    personalIdCol = i;
                    break;
                }
            }

            pw.print("NewPersonalId,");
            pw.println(headers);

            int scanCount = 0;

            while (sc.hasNextLine()) {

                String line = sc.nextLine();

                String[] array = line.split(",");
                try {

                    // Get the Client's personalId
                    String personalId = array[personalIdCol];

                    // Change it to the right PersonalId if it is a match
                    if (matchingClients.containsKey(personalId)) {
                        personalId = matchingClients.get(personalId);
                    }

                    pw.print(personalId + ",");

                } catch (ArrayIndexOutOfBoundsException e) {
                }

                pw.println(line);

                //scanCount++;
            }

        }//end try

    }//end changePersonalIds

}//end class
