package io.github.cardsandhuskers.tag.objects;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple Class that creates a String to be written
 * to a CSV file based on the header names and line 
 * entries given to the class.
 * 
 * @author J. Scotty Solomon
 * @version 22-Dec-2023
 */
public class Stats {
    private String headers;
    private List<String> entries;
    private int length;

    /**
     * Saves csv header and the expected number of 
     * commas per entry.
     * 
     * @param headers Comma separated list of header names
     * Do not include a newline character.
     */
    public Stats(String headers) {
        this.headers = headers;
        entries = new ArrayList<String>();

        String[] commas = headers.split(",");
        length = commas.length;
    }

    /**
     * Adds the line to private list of all csv entries.
     * Does not add line if the amount of commas is 
     * not equal to the amount of commas in the header.
     * 
     * @param line comma-separated list of data to be saved.
     * Do NOT include a newline character.
     * @return if new line was added
     */
    public boolean addEntry(String line) {
        String[] commas = line.split(",");

        if(commas.length == this.length) {
            entries.add(line);
            return true;
        }

        return false;
        
    }

    /**
     * Creates a String for a csv file that can be written
     * directly to a csv file.
     * 
     * @return generated String
     */
    public String getCSV() {
        if(entries.size() == 0) {
            System.out.println("null");
            return null;
        }

        String csv = headers + "\n";

        for(String entry: entries) {
            csv += entry + "\n";
        }

        return csv;
    }

    /**
     * Writes the generated csv file to the path specified.
     * 
     * @param filePath Path for file
     * @param fileName Filename, excluding .csv, .txt, et cetera
     */
    public void writeToFile(String filePath, String fileName) {
        fileName += ".csv";

        Path path = Paths.get(filePath,fileName);

        PrintWriter out = null;

        try{
            if(!Files.exists(path)) {
                Files.createFile(path);
            }
            out = new PrintWriter(path.toString());

            System.out.println(path.toString());

            out.print(getCSV());

            out.close();

        } catch(Exception e) {
            e.printStackTrace();
            if(out != null) {
                out.close();
            }
        }
    }

    public String toString() {
        return getCSV();
    }

    public static void main(String[] args) {
        Stats stat = new Stats("One,Two,Three");

        stat.addEntry("1,test,test");
        stat.addEntry("2,test,test");
        stat.addEntry("3,test,test");
        stat.addEntry("4,test,test");
        stat.addEntry("5,test,test");
        stat.addEntry("6,test,test");

        System.out.println(stat.getCSV());

        System.out.println("Writing to file");

        stat.writeToFile(".","test");
    }
}
