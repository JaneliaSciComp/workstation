/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.compression;

import java.io.File;

/**
 * Implement this, to compress some file.
 * @author fosterl
 */
public interface CompressionAlgorithm {
    /** Chain-of-responsibility support. */
    boolean canUncompress(File infile);
    
    /** Compression */
    byte[] uncompress(File infile) throws CompressionException;
    byte[] uncompress(byte[] inbutes) throws CompressionException;
}
