package org.janelia.workstation.webdav.exception;

import java.io.Serializable;
/**
 * Created by schauderd on 6/29/15.
 */

public class PermissionsFailureException extends Exception implements Serializable
{
    public PermissionsFailureException() {
        super();
    }
    public PermissionsFailureException(String msg)   {
        super(msg);
    }
    public PermissionsFailureException(String msg, Exception e)  {
        super(msg, e);
    }
}
