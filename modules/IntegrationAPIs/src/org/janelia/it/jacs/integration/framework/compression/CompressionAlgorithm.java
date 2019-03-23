/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.jacs.integration.framework.compression;

import java.io.File;

/**
 * Implement this, to compress some file.
 * @author fosterl
 */
public interface CompressionAlgorithm {
    /** Chain-of-responsibility support. */
    boolean canDecompress(File infile);
    
    /**
     * Tells what to look for, to figure out if this algorithm will work.
     * 
     * @param decompressedFile name that the application requires.
     * @return name of the compressed file that exists.
     */
    File getCompressedNameForFile(File decompressedFile);
    
    /**
     * Given you have a compressed file name, that can be decompressed by
     * this algorithm, this method tells what the decompressed file will be
     * named.  Only TELLS the name. Un-compression is not done here.
     * 
     * @param compressedFile pre-decompress.
     * @return post-decompress.
     */
    File getDecompressedNameForFile(File compressedFile);
    
    /** Compression */
    byte[] decompressAsBytes(File infile) throws CompressionException;
    byte[] decompressAsBytes(byte[] inbytes) throws CompressionException;
    File decompressAsFile(File infile) throws CompressionException;

    /** These may be used, if the compressed size is known in advance.  */
    byte[] decompressIntoByteBuf(File infile, byte[] outbytes) throws CompressionException;
    byte[] decompressIntoByteBuf(byte[] inbytes, byte[] outbytes) throws CompressionException;
}
