package org.janelia.it.FlyWorkstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:09 PM
 */
public class CommandException extends Exception {
    /**
     * @label source command
     */
    public Command sourceCommand;

    public CommandException(Command aSourceCommand, String commandException) {
        super(commandException);
        sourceCommand = aSourceCommand;
    }
}
