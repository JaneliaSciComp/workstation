/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.compression;

import java.io.File;

/**
 * Simplistic '1:1' compression algorithm. It just reads data off the disk.
 *
 * @author fosterl
 */
public class TrivialCompression implements CompressionAlgorithm {

    /** We can ALWAYS do nothing to an input file. */
    @Override
    public boolean canUncompress(File infile) {
        return true;
    }

    /** The compressed and uncompressed files are one and the same. */
    @Override
    public File compressedVersion(File infile) {
        return infile;
    }

    /**
     * The compressed and uncompressed files are one and the same.
     */
    @Override
    public File uncompressedVersion(File compressedFile) {
        return compressedFile;
    }

    @Override
    public byte[] uncompress(File infile) throws CompressionException {
        try {
            FileCollector collector = new FileCollector();
            collector.collectFile(infile);
            return collector.getData();
        } catch ( Exception ex ) {
            throw new CompressionException(ex);
        }
    }

    @Override
    public byte[] uncompress(byte[] inbytes) throws CompressionException {
        return inbytes;
    }

    @Override
    public byte[] uncompress(File infile, byte[] outbytes) throws CompressionException {        
        try {
            FileCollector collector = new FileCollector();
            collector.collectFile(infile, outbytes);
            return collector.getData();
        } catch (Exception ex) {
            throw new CompressionException(ex);
        }
    }

    /**
     * Ignoring the outbytes, here.  Truly a trivial operation.
     * 
     * @param inbytes data, which is returned.
     * @param outbytes unused
     * @return the inbytes
     * @throws CompressionException 
     */
    @Override
    public byte[] uncompress(byte[] inbytes, byte[] outbytes) throws CompressionException {
        return inbytes;
    }

}
