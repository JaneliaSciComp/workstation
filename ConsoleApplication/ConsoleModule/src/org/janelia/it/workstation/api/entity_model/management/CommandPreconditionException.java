package org.janelia.it.workstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:10 PM
 * This exception is used to note that a command cannot execute because the
 * the preconditions are invalid.
 * The exception should only be thrown in the command "validate preconditions"
 * phase which is BEFORE the command "execution" phase, and before any changes
 * have been made to the subject / target of the command.
 * @author Jay T. Schira
 */

public class CommandPreconditionException extends CommandException {

    public static final String COMMAND_EXCEPTION_PREFIX_STRING="Invalid Precondition! ";

    public CommandPreconditionException(Command aSourceCommand,String preconditionString) {
        super(aSourceCommand,COMMAND_EXCEPTION_PREFIX_STRING + preconditionString /*+ "\nCommand will NOT execute."*/);
    }

    public CommandPreconditionException(Command aSourceCommand,String exceptiontype, String preconditionString) {
        super(aSourceCommand,exceptiontype+ preconditionString /*+ "\nCommand will NOT execute."*/);
    }
}