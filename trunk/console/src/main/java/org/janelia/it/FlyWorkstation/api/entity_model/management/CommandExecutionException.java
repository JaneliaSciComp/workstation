package org.janelia.it.FlyWorkstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:13 PM
 * This exception is used to note that though a command's preconditions were
 * valid, the command failed during execution.
 * The exception should only be thrown AFTER the command "validate postconditions"
 * phase DURING the command "execution" phase, and after some changes may
 * have been made to the subject / target of the command.  The subject features
 * may be in an unrelaiable state.
 * @author Jay T. Schira
 */

public class CommandExecutionException extends CommandException {

    public CommandExecutionException(Command aSourceCommand, String executionString) {
        super(aSourceCommand, "Error in Command Execution! " + executionString +
                "\nData in unpredictable state.");
    }
}