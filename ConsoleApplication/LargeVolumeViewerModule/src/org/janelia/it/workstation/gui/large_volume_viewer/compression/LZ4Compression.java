/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.compression;

import java.io.File;
import java.util.Date;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4UnknownSizeDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements compression algorithm with lz4-java
 * @author fosterl
 */
public class LZ4Compression implements CompressionAlgorithm {
    public static final String TARGET_EXTENSION = ".lz4";
    private Logger log = LoggerFactory.getLogger(LZ4Compression.class);

    @Override
    public boolean canUncompress(File infile) {
        return infile.getName().endsWith(TARGET_EXTENSION);
    }

    /**
     * Given the input file, of appropriate extension and contents, uncompress
     * it into original version, and return file handle to that.
     * 
     * @param infile what to uncompress
     * @return uncompressed blob.
     * @throws CompressionException if invalid extension, or anything goes wrong
     */
    @Override
    public byte[] uncompress(File infile) throws CompressionException {
        if (canUncompress(infile)) {
            try {
                Date startTime = new Date();

                FileCollector collector = new FileCollector();
                collector.collectFile(infile);
                byte[] b = collector.getData();
                Date endFileRead = new Date();
                
                log.info("Time required for file-read: {}s.", (endFileRead.getTime() - startTime.getTime()) / 1000);
                
                return uncompress(b);

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }
            
        }
        else {
            throw new CompressionException("Cannot uncompress file without extens "+TARGET_EXTENSION);
        }
    }

    /**
     * Given a pre-digested byte array, uncompress it.
     * @param inbytes some compressed blob.
     * @return uncompressed blob.
     * @throws CompressionException 
     */
    @Override
    public byte[] uncompress(byte[] inbytes) throws CompressionException {
        Date startTime = new Date();
        LZ4Factory factory = LZ4Factory.fastestJavaInstance();
        LZ4UnknownSizeDecompressor decompressor = factory.unknwonSizeDecompressor();
        final int fileLen = (int) inbytes.length;

        byte[] dest = new byte[fileLen * 3];
        int decompressedSize = decompressor.decompress(inbytes, 0, fileLen, dest, 0, dest.length);
        Date endDecompress = new Date();
        log.info("Time required for decompress-in-memory: {}s.", (endDecompress.getTime() - startTime.getTime()) / 1000);

        byte[] trimmedDest = new byte[decompressedSize];
        System.arraycopy(dest, 0, trimmedDest, 0, decompressedSize);
        return trimmedDest;
    }

}
