package org.janelia.it.workstation.browser.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Goes through NetBeans artifacts looking for version numbers in specific types of files, and with very specific
 * formats, and increases those number.
 *
 * Created by fosterl on 9/26/2016.
 */
public class VersionNumberIncrementer {
    public static final String PROJECT_XML = "project.xml";
    public static final String MANIFEST_MF = "manifest.mf";

    private String[] oldVersion;
    private String[] newVersion;
    private FileFilter directoryFilter;
    private FileFilter replaceFileFilter;

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: java " + VersionNumberIncrementer.class.getName() + " <old versions comma-sep> <new versions comma-sep>\n" +
                                               "    Run this in base directory of all code to modify.");
        }
        try {
            String cwd = System.getProperty("user.dir");
            new VersionNumberIncrementer(args[0].split(","), args[1].split(",")).execute(cwd);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public VersionNumberIncrementer(String[] oldVersion, String[] newVersion) {
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        replaceFileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.getName().equals(PROJECT_XML)) || (pathname.getName().equals(MANIFEST_MF));
            }
        };
    }

    public void execute(String baseDirectory) throws IOException {
        File baseDirectoryFile = new File(baseDirectory);
        if (! baseDirectoryFile.isDirectory()) {
            throw new IllegalArgumentException("File " + baseDirectory + " is not a directory.");
        }

        int count = walkDirs(baseDirectoryFile);
        System.out.println("Carried out replacement on " + count + " files.");
    }

    private int walkDirs(File baseDirectory) throws IOException {
        // Carry out replacement on this directory.
        int rtnVal = replaceAll(baseDirectory);

        for (File directory: baseDirectory.listFiles(directoryFilter)) {
            rtnVal += walkDirs(directory);
        }
        return rtnVal;
    }

    /**
     * Iterate over directory, recursively, replacing all patterns.
     *
     * @param directory contains replace candidates, and/or more directories.
     * @return number of replacements carried out.
     * @throws IOException
     */
    private int replaceAll(File directory) throws IOException {
        int count = 0;
        File[] files = directory.listFiles(replaceFileFilter);
        for (File file: files) {
            File temp = updateFileVersion(file, oldVersion, newVersion);
            if (temp != null) {
                count ++;
            }
        }
        return count;
    }

    /**
     * Bump version numbers in well-known patterns, within the file given.  Does not detect whether the file is
     * different.
     *
     * @param infile file likely to contain these patterns.
     * @param oldVersion version number format N.NN
     * @param newVersion version number format N.NN (likely higher than old version).
     * @return file with converted with version number updates.
     * @throws IOException thrown by IO operations.
     */
    public File updateFileVersion(File infile, String[] oldVersion, String[] newVersion) throws IOException {
        System.out.println("Testing: " + infile.toString() );
        File tempFile = File.createTempFile("_release_",".temp", infile.getParentFile());

        try (BufferedReader br = new BufferedReader( new FileReader(infile)); PrintWriter pw = new PrintWriter(new FileWriter(tempFile )) ) {
            String inline = null;
            while (null != (inline = br.readLine() ) ) {
                for (int i = 0; i < oldVersion.length; i++) {
                    inline = inline.replace("<specification-version>"+oldVersion[i]+"</specification-version>", "<specification-version>"+newVersion[i]+"</specification-version>");
                    final String inPattern = "OpenIDE-Module-Specification-Version: " + oldVersion[i];
                    if (inline.contains(inPattern + ".")) {
                        System.out.println("Warning: extra tuple after expected version number. " + inline);
                    }
                    else {
                        inline = inline.replace(inPattern, "OpenIDE-Module-Specification-Version: "+newVersion[i]);
                    }
                }
                pw.print(inline + "\n");
            }
        } catch (IOException ex) {
            throw ex;
        }

        infile.delete();
        tempFile.renameTo(infile);
        return tempFile;
    }

}
