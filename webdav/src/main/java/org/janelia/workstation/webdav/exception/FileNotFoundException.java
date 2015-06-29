package org.janelia.workstation.webdav.exception;

import java.io.Serializable;
/**
 * Created by schauderd on 6/29/15.
 */

public class FileNotFoundException extends Exception implements Serializable
{
    public FileNotFoundException() {
        super();
    }
    public FileNotFoundException(String msg)   {
        super(msg);
    }
    public FileNotFoundException(String msg, Exception e)  {
        super(msg, e);
    }
}
