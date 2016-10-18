package org.janelia.it.workstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:11 PM
 * This exception is used to note that though a command executed an invalid
 * postcondition was detected.
 * The exception should only be thrown in the command "validate postconditions"
 * phase which is AFTER the command "execution" phase, and after any changes
 * have been made to the subject / target of the command.  The subject features
 * may be in an unrelaiable state.
 * @author Jay T. Schira
 * @version $Id: CommandPostconditionException.java,v 1.1 2006/11/09 21:36:08 rjturner Exp $
 */

public class CommandPostconditionException extends CommandException {

    public CommandPostconditionException(Command aSourceCommand, String postconditionString) {
        super(aSourceCommand, "Invalid Postcondition! " + postconditionString +
                "\nData in unpredictable state.");
    }
}