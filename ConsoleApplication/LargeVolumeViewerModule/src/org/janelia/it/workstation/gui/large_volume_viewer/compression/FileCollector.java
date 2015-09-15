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
    private static final int READ_CHUNK_SIZE = 10240;
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
        try (FileInputStream fis = new FileInputStream(infile)) {
            readCount = 0;
            int readSize = READ_CHUNK_SIZE;
            int amountRead = 0;
            // Reading in chunks.
            boolean breakTime = false;
            final int iterCount = new Double(Math.ceil((double)fileLen / (double)readSize)).intValue();
            for (int i = 0; i < iterCount; i++) {
                if (readCount + readSize > fileLen) {
                    readSize = fileLen - readCount;
                    breakTime = true;
                }
                amountRead = fis.read(b, readCount, readSize);
                if (amountRead > -1) {
                    readCount += amountRead;
                }
                if (breakTime  ||  amountRead == -1) {
                    break;
                }
            }
            if (readCount != fileLen) {
                throw new Exception("Failed to fully read infile " + infile);
            }
            data = b;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
