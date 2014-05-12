package org.janelia.it.FlyWorkstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:12 PM
 */
public class CompositeCommandPreconditionException extends CommandPreconditionException{

    public static String COMMAND_EXCEPTION_PREFIX_STRING=" ";


    public CompositeCommandPreconditionException(Command aSourceCommand, String preconditionString) {
        //super(aSourceCommand,preconditionString);
        super(aSourceCommand,COMMAND_EXCEPTION_PREFIX_STRING, preconditionString);
    }
}