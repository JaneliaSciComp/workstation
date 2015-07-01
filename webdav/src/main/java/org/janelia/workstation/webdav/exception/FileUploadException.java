package org.janelia.workstation.webdav.exception;

import java.io.Serializable;
/**
 * Created by schauderd on 6/29/15.
 */

public class FileUploadException extends Exception implements Serializable
{
    public FileUploadException() {
        super();
    }
    public FileUploadException(String msg)   {
        super(msg);
    }
    public FileUploadException(String msg, Exception e)  {
        super(msg, e);
    }
}
