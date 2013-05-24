package org.janelia.it.FlyWorkstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:16 PM
 */
/**
 AbsentRedoException is raised when a redo is requested but not available
 */
public class AbsentRedoException extends Exception
{
    public AbsentRedoException(){ super("Redo requested with none available");}
}

