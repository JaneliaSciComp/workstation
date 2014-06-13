package org.janelia.it.workstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:16 PM
 */
/**
 AbsentUndoException is raised when an undo is requested but not available
 */
public class AbsentUndoException extends Exception
{
    public AbsentUndoException(){ super("Undo requested with none available");}
}

