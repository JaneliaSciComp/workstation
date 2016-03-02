/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.compression;

import org.janelia.it.jacs.integration.framework.compression.CompressionException;
import org.janelia.it.jacs.integration.framework.compression.CompressionAlgorithm;
import java.io.File;
import java.util.Date;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4UnknownSizeDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements compression algorithm with lz4-java
 * @deprecated compression algorithm not used, but code is decent setup example.
 * @author fosterl
 */
public class LZ4Compression implements CompressionAlgorithm {
    public static final String TARGET_EXTENSION = ".lz4";
    private Logger log = LoggerFactory.getLogger(LZ4Compression.class);

    @Override
    public boolean canDecompress(File infile) {
        return infile.getName().endsWith(TARGET_EXTENSION);
    }
    
    @Override
    public File getCompressedNameForFile(File infile) {
        return new File(infile.getParentFile(), infile.getName() + TARGET_EXTENSION);
    }
    
    /**
     * Checks to see if the "compressed file" has the target extension of this
     * LZ4 algorithm.  If it does, return the 'root' version of the file name.
     * Otherwise, return null--indicating this algorithm will not decompress.
     * 
     * @param compressedFile final result of having run LZ4
     * @return original file name, decompressAsBytesed by LZ4, or null
     */
    @Override
    public File getDecompressedNameForFile(File compressedFile) {
        File rtnVal = null;
        final String fileName = compressedFile.getName();
        if (fileName.endsWith(TARGET_EXTENSION)) {
            rtnVal = new File(compressedFile.getParentFile(), fileName.substring(0, fileName.length() - TARGET_EXTENSION.length()));
        }
        return rtnVal;
    }

    /**
     * Given the input file, of appropriate extension and contents, decompress
     * it into original version, and return file handle to that.
     * 
     * @param infile what to decompressAsBytes
     * @return decompressAsBytesed blob.
     * @throws CompressionException if invalid extension, or anything goes wrong
     */
    @Override
    public byte[] decompressAsBytes(File infile) throws CompressionException {
        if (canDecompress(infile)) {
            try {
                Date startTime = new Date();
                byte[] b = collectBytes(infile);
                Date endFileRead = new Date();
                
                log.info("Time required for file-read: {}s.", (endFileRead.getTime() - startTime.getTime()) / 1000);
                
                return decompressAsBytes(b);

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }
            
        }
        else {
            throw new CompressionException("Cannot uncompress file without extens "+TARGET_EXTENSION);
        }
    }

    @Override
    public File decompressAsFile(File infile) throws CompressionException {
        if (canDecompress(infile)) {
            try {
                byte[] b = decompressAsBytes(infile);

                // Not efficient at all, but satisfies the requirement.
                b = decompressAsBytes(b);
                
                File tempFile = File.createTempFile("LZ4_ConsoleApp", this.getDecompressedNameForFile(infile).getName());
                return tempFile;

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }

        } else {
            throw new CompressionException("Cannot uncompress file without extens " + TARGET_EXTENSION);
        }
    }

    /**
     * Given a pre-digested byte array, decompress it.
     * @param inbytes some compressed blob.
     * @return decompressAsBytesed blob.
     * @throws CompressionException 
     */
    @Override
    public byte[] decompressAsBytes(byte[] inbytes) throws CompressionException {
        
        Date startTime = new Date();
        final int fileLen = (int) inbytes.length;
        byte[] dest = new byte[fileLen * 3];
        
        LZ4Factory factory = LZ4Factory.fastestJavaInstance();
        LZ4UnknownSizeDecompressor decompressor = factory.unknwonSizeDecompressor();
        int decompressedSize = decompressor.decompress(inbytes, 0, fileLen, dest, 0, dest.length);
        Date endDecompress = new Date();
        log.info("Time required for decompress-in-memory: {}s.", (endDecompress.getTime() - startTime.getTime()) / 1000);

        byte[] trimmedDest = new byte[decompressedSize];
        System.arraycopy(dest, 0, trimmedDest, 0, decompressedSize);
        return trimmedDest;
    }

    @Override
    public byte[] decompressIntoByteBuf(File infile, byte[] outbytes) throws CompressionException {
        if (canDecompress(infile)) {
            try {
                Date startTime = new Date();
                byte[] b = collectBytes(infile);
                Date endFileRead = new Date();

                log.info("Time required for file-read: {}s.", (endFileRead.getTime() - startTime.getTime()) / 1000);

                return decompressIntoByteBuf(b, outbytes);

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }

        } else {
            throw new CompressionException("Cannot uncompress file without extens " + TARGET_EXTENSION);
        }
    }

    public byte[] collectBytes(File infile) throws Exception {
        FileCollector collector = new FileCollector();
        collector.collectFile(infile);
        byte[] b = collector.getData();
        return b;
    }

    @Override
    public byte[] decompressIntoByteBuf(byte[] inbytes, byte[] outbytes) throws CompressionException {
        Date startTime = new Date();
        LZ4Factory factory = LZ4Factory.fastestJavaInstance();
        LZ4UnknownSizeDecompressor decompressor = factory.unknwonSizeDecompressor();
        final int fileLen = (int) inbytes.length;

        decompressor.decompress(inbytes, 0, fileLen, outbytes, 0, outbytes.length);
        Date endDecompress = new Date();
        log.info("Time required for decompress-in-memory: {}s.", (endDecompress.getTime() - startTime.getTime()) / 1000);

        return outbytes;
    }

}
