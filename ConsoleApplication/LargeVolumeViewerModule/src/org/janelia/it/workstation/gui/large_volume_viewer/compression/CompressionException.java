/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.compression;

/**
 * Throw this in event compression fails in either a predictable way,
 * or as the result of some other operation having thrown an exception.
 *
 * @author fosterl
 */
public class CompressionException extends Exception {
    /**
     * Failing due to some condition having not been met.
     * @param message tell why
     */
    public CompressionException(String message) {
        super(message);
    }
    
    /**
     * Failing due to caught exception.
     * @param parent caught
     */
    public CompressionException(Exception parent) {
        super(parent);
    }    
}
