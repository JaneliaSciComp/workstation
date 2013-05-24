package org.janelia.it.FlyWorkstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:12 PM
 */
/**
 BadCommandInverseException is raised when a command token is executed
 and returns itself as its inverse.
 */
public class BadCommandInverseException extends Exception
{
    public BadCommandInverseException( Command cause )
    {
        super("Inverse of a command token was itself");
        mCause = cause;
    }
    public Command mCause;
}


