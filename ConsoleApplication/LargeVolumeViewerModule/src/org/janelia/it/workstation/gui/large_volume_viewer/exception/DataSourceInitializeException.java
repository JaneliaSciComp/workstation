/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.exception;

/**
 * Exception to be thrown when a data source fails to initialized.  Capable
 * of wrapping exceptions of various types.
 * 
 * @author fosterl
 */
public class DataSourceInitializeException extends Exception {
    public DataSourceInitializeException( Exception wrapped ) {
        super(wrapped);
    }
    public DataSourceInitializeException( String message ) {
        super(message);
    }
    public DataSourceInitializeException( String msg, Exception ex ) {
        super(msg, ex);
    }
}
