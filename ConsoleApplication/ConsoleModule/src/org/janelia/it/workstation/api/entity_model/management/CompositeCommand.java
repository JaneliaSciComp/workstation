package org.janelia.it.workstation.api.entity_model.management;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:09 PM
 */

import java.util.ArrayList;
import java.util.Iterator;

/**
 * CompositeEditToken provides a mechanism for bundling together several
 * individual edit tokens into a single action.
 */
public class CompositeCommand extends org.janelia.it.workstation.api.entity_model.management.Command
{
    private String commandName;
    /**
     * An ordered collection of (sub) CommandTokens that are part of this CompositeCommandToken.
     * Order is significant.
     * The collection of (sub)CommandTokens should at all times be maintained such
     * that the order of the collection is the order the sub-commands should be executed.
     * @associates <{Command}>
     * @supplierCardinality 0..*
     * @label sub commands
     * @link aggregationByValue
     */
    private java.util.List subCommandList;


    /**
     * Constructor for the CompositeToken.
     */
    public CompositeCommand(String aName) {
        commandName = aName;
        subCommandList = new ArrayList();
    }


    public void validatePreconditions()throws CommandPreconditionException {
        for (Iterator i = subCommandList.iterator(); i.hasNext(); ) {
            org.janelia.it.workstation.api.entity_model.management.Command subCommand = (org.janelia.it.workstation.api.entity_model.management.Command)i.next();
            try{
                subCommand.validatePreconditions();
            }catch(CommandPreconditionException ex){
                throw(new org.janelia.it.workstation.api.entity_model.management.CompositeCommandPreconditionException(this, "One of the subcommands failed with this message : \n"+ex.getMessage()));
            }
        }

    }

    /**
     * CompositeCommandTokens are populated by adding CommandTokens via
     * addNextCommand().  Command actions MUST be
     * added in the order in which they should be executed!
     */
    public void addNextCommand( org.janelia.it.workstation.api.entity_model.management.Command newSubCommand )
    {
        // The ArrayList adds to the end.
        subCommandList.add(newSubCommand);
    }  // End of addNextCommand() method.


    /**
     * CompositeCommandTokens can be populated by adding entire lists of CommandTokens.
     * The list of commands should be in the order in which it should be executed.
     * The list should be added in the order in which it should be executed.
     * The CommandTokens in the list are added to this CompositeCommand individually,
     * not as a CompositeCommand
     */
    public void addNextCommandList( java.util.List newSubCommands ) {
        for (Iterator i = newSubCommands.iterator(); i.hasNext(); ) {
            this.addNextCommand((org.janelia.it.workstation.api.entity_model.management.Command)i.next());
        }
    }



    /**
     * Modifies the model, returning a command token
     * capable of undoing the resultant change. The components of the
     * returned token will be the inverses of the components of this
     * token, in reverse order.
     */
    public org.janelia.it.workstation.api.entity_model.management.Command execute() throws Exception
    {
        // Execute my components in order
        // and gather their inverses in reverse order...
        boolean isInvertable = true;
        java.util.List reverseOrderedInvserseSubCommands = new ArrayList();

        // Iterate through this CompositeCommandToken's sub-commandTokens,
        // Collecting the inverse commands and making sure the command is invertable.
        for (Iterator i = subCommandList.iterator(); i.hasNext(); ) {
            org.janelia.it.workstation.api.entity_model.management.Command subCommand = (org.janelia.it.workstation.api.entity_model.management.Command)i.next();
            org.janelia.it.workstation.api.entity_model.management.Command inverseSubCommand = subCommand.execute();
            // We are reversing the order of the inverse commands, so add to the front of the list.
            if ( inverseSubCommand != null ) reverseOrderedInvserseSubCommands.add(0, inverseSubCommand);
                // If any sub-commands are not invertable, then this composite is not invertable.
            else isInvertable = false;
        }

        // If I am invertable, build my inverse Composite Command out of the
        // reverse-ordered collection of inverse sub-commands...
        if( isInvertable ) {
            CompositeCommand inverseComposite = new CompositeCommand(commandName);
            inverseComposite.addNextCommandList(reverseOrderedInvserseSubCommands);
            return inverseComposite;
        }
        return null;
    }  // End of executeLocal() method.



    /**
     * toString() will return the name of the command.
     */
    public String toString() {
        return this.commandName;
    }


    public String getCommandLogMessage() {
        String resultStr="";
        for(Iterator iter=subCommandList.iterator();iter.hasNext();){
            org.janelia.it.workstation.api.entity_model.management.Command c=(org.janelia.it.workstation.api.entity_model.management.Command)iter.next();
            resultStr=resultStr+c.getCommandLogMessage()+"\n";

        }
        return resultStr;
    }

}  // End of CompositeCommandToken class.

