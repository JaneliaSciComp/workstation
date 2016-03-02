/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.compression;

import org.janelia.it.jacs.integration.framework.compression.CompressionException;
import org.janelia.it.jacs.integration.framework.compression.CompressionAlgorithm;
import java.io.File;

/**
 * Simplistic '1:1' compression algorithm. It just reads data off the disk.
 *
 * @author fosterl
 */
public class TrivialCompression implements CompressionAlgorithm {

    /** We can ALWAYS do nothing to an input file. */
    @Override
    public boolean canDecompress(File infile) {
        return true;
    }

    /** The compressed and decompressed files are one and the same. */
    @Override
    public File getCompressedNameForFile(File infile) {
        return infile;
    }

    /**
     * The compressed and decompressed files are one and the same.
     */
    @Override
    public File getDecompressedNameForFile(File compressedFile) {
        return compressedFile;
    }
    
    @Override
    public File decompressAsFile(File infile) throws CompressionException {
        return infile;
    }

    @Override
    public byte[] decompressAsBytes(File infile) throws CompressionException {
        try {
            FileCollector collector = new FileCollector();
            collector.collectFile(infile);
            return collector.getData();
        } catch ( Exception ex ) {
            throw new CompressionException(ex);
        }
    }

    @Override
    public byte[] decompressAsBytes(byte[] inbytes) throws CompressionException {
        return inbytes;
    }

    @Override
    public byte[] decompressIntoByteBuf(File infile, byte[] outbytes) throws CompressionException {        
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
    public byte[] decompressIntoByteBuf(byte[] inbytes, byte[] outbytes) throws CompressionException {
        return inbytes;
    }

}
