package org.janelia.it.workstation.gui.large_volume_viewer.compression;

import java.io.File;
import java.io.FileInputStream;

/**
 * Pulls the bytes of the file into memory, and makes any required metadata
 * available to the caller.
 *
 * @author fosterl
 */
public class FileCollector {
    private byte[] data;
    private int readCount = 0;
    
    public byte[] getData() {
        return data;
    }
    
    public int getFileSize() {
        return readCount;
    }
    
    /**
     * "Read Fully" the bytes of the input file, to memory.  Keep
     * metadata found during the process.
     * 
     * @param infile
     * @throws Exception 
     */
    public void collectFile(File infile) throws Exception {
        final int fileLen = (int) infile.length();        
        byte[] b = new byte[fileLen];
        
        collectFile(infile, b);
    }

    /**
     * "Read Fully" the bytes of the input file, to memory. Keep metadata found
     * during the process.
     *
     * @param infile
     * @throws Exception
     */
    public void collectFile(File infile, byte[] b) throws Exception {
        final int fileLen = (int) infile.length();
        final FileInputStream fis = new FileInputStream(infile);
        readCount = fis.read(b);
        fis.close();
        if (readCount != fileLen) {
            throw new Exception("Failed to fully read infile " + infile);
        }
        data = b;
    }
}
