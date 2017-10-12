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
 * Main program
 *
 * @author Hamza Memon, Keith Grable
 * @version 2017-10-04
 */
public class Algorithm1 {

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
     * a map of string representations of dates, to date objects
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
     * Reads the client.csv file De-duplicates entries Writes the new list of
     * entries to an output file
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
        StringBuilder sb = new StringBuilder(200 * 100_000);
        sb = sb.append(headers).append('\n');

        System.out.println("DEDUPLICATING ...");

        //for each entry
        while (sc.hasNextLine()) {

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
                        dobS, dob, dobDataQuality, gender, line
                );
                boolean isNew = isNewClient(client);
                if (isNew) {
                    clients.add(client);
                }

            }//end special date condition

        }//END WHILE

        sc.close();

        System.out.println("WRITING OUTPUT FILES ...");

        // Get all other files
        File[] files = new File(INPUT_PATH).listFiles();
        for (File file : files) {
            String filename = file.getName();
            // Get all CSVs
            if (filename.endsWith(".csv")) {
                String noExtension = filename.substring(0, filename.indexOf(".csv"));
                // A CSV provided by HUD
                if (!noExtension.endsWith("Output")) {
                    System.out.println("Writing to " + filename + "Output ...");
                    changePersonalIds(filename);
                    System.out.println("Wrote to " + filename + "Output");
                }
            }
        }

        System.out.println("COUNT OF UNIQUE CLIENTS = " + clients.size());

    }//end main

    /**
     * Run the algorithm
     *
     * @param newClient the current line of the file
     *
     * @return if newClient is new (true) or not (false)
     */
    private static boolean isNewClient(Client newClient) {

        //for each previously entered client
        for (Client otherClient : clients) {

            //if the clients match
            if (isMatch1(newClient, otherClient)) {
                // Add newClient to an ArrayList for the main client
                try {
                    //add new client to list of duplicates of old client
                    map.get(otherClient).add(newClient);
                } catch (NullPointerException e) {
                    ArrayList<Client> list = new ArrayList<>();
                    list.add(newClient);
                    map.put(otherClient, list);
                }
                return false;
            }

            if (isMatch2(newClient, otherClient)) {
                // Same thing as before
                try {
                    map.get(otherClient).add(newClient);
                } catch (NullPointerException e) {
                    ArrayList<Client> list = new ArrayList<>();
                    list.add(newClient);
                    map.put(otherClient, list);
                }
                return false;
            }

        }

        return true;
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

        //if SSNs are NOT, equal and valid values
        //then there is no match (test 1)
        if (!hasSsnMatch(newClient.getSsn(), client)) {
            return false;
        }

        //ssns must be full-reports
        String newClientSsnDataQuality = newClient.getSsnDataQuality();
        String clientSsnDataQuality = client.getSsnDataQuality();

        //if either client has incomplete ssn
        //then there is no match (test 1)
        if (!isFullSsn(newClientSsnDataQuality) || !isFullSsn(clientSsnDataQuality)) {
            return false;
        }

        //ssns are complete and equal
        //now check for at least two matches
        String[] client1Info = {newClient.getfName(), newClient.getlName(), newClient.getDobS()};
        String[] client2Info = {client.getfName(), client.getlName(), client.getDobS()};

        int equalCount = countOfEqual(client1Info, client2Info);
        if (equalCount >= 2) {
            matchingClients.put(newClient.getPersonalId(), client.getPersonalId());
            return true;
        }

        // no match
        return false;

    }//END isMatch1

    private static boolean isMatch2(Client newClient, Client client) {
        //
        //
        // First set of criteria
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
            return false;
        }

        //
        //
        // Second set of criteria
        //
        //
        String gender = newClient.getGender();
        String suffix = newClient.getSuffix();
        LocalDate dob = newClient.getDob();

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

        int inequalCount = countOfInequal(client1Info, client2Info);

        // if Second set of criteria fails
        if (inequalCount >= 2) {
            return false;
        }

        //
        //
        // Third set of criteria
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
                        return false;
                    }
                }
            }
        }

        // Link the main client's PID to the Client
        matchingClients.put(personalId, client.getPersonalId());
        return true;
    }

    /**
     * If SSNs are equal and SSN is not garbage based on spec
     */
    private static boolean hasSsnMatch(String ssn, Client client) {
        return client.getSsn().equals(ssn) && !"999999999".equals(ssn) && !"000000000".equals(ssn) && !ssn.isEmpty();
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

    /**
     * Duh
     */
    private static void printToFile(String filename,
            String contents) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter pw = new PrintWriter(filename, "UTF-8")) {
            pw.println(contents);
        }
    }

    private static void changePersonalIds(String filename) throws FileNotFoundException, UnsupportedEncodingException {
        Scanner sc = new Scanner(new File(filename), "UTF-8");
        sc = sc.useDelimiter("[,\n]");

        String headers = sc.nextLine();
        String[] headersArray = headers.split(",");
        headers = "NewPersonalID," + headers;

        int personalIdCol = -1;
        for (int i = 0; i < headersArray.length; i++) {
            // Is there even a PersonalID column (lowercase because it's spelled differently)
            if ("personalid".equals(headersArray[i].toLowerCase())) {
                personalIdCol = i;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb = sb.append(headers).append('\n');

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            String[] array = line.split(",");
            String personalId;
            try {
                // Get the Client's personalId
                personalId = array[personalIdCol];
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }

            // Change it to the right PersonalId if it is a match
            if (matchingClients.containsKey(personalId)) {
                personalId = matchingClients.get(personalId);
            }

            line = personalId + "," + line;
            sb = sb.append(line).append('\n');
        }

        String noExtensionFileName = filename.substring(0, filename.indexOf(".csv"));

        // Output
        if (!sb.toString().equals(headers + "\n")) {
            printToFile("output/" + noExtensionFileName + "Output.csv", sb.toString());
        }

    }//end main

}//end class
