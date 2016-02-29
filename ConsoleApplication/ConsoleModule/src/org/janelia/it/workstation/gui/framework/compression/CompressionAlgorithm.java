/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.compression;

import java.io.File;

/**
 * Implement this, to compress some file.
 * @author fosterl
 */
public interface CompressionAlgorithm {
    /** Chain-of-responsibility support. */
    boolean canUncompress(File infile);
    
    /** Tells what to look for, to figure out if this algorithm will work. */
    File compressedVersion(File infile);
    
    /**
     * Given you have a compressed file name, that can be decompressed by
     * this algorithm, this method tells what the uncompressed file will be
     * named.  Only TELLS the name. Decompression is not done here.
     * 
     * @param compressedFile pre-uncompress.
     * @return post-uncompress.
     */
    File uncompressedVersion(File compressedFile);
    
    /** Compression */
    byte[] uncompress(File infile) throws CompressionException;
    byte[] uncompress(byte[] inbytes) throws CompressionException;

    /** These may be used, if the uncompressed size is known in advance.  */
    byte[] uncompress(File infile, byte[] outbytes) throws CompressionException;
    byte[] uncompress(byte[] inbytes, byte[] outbytes) throws CompressionException;
}
