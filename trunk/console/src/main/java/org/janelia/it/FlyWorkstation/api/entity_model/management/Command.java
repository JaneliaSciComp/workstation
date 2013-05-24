package org.janelia.it.FlyWorkstation.api.entity_model.management;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:06 PM
 */
public abstract class Command {
    /**
     * Command provides an interface for functors that modify the model,
     * allowing undo and redo of actions. See @ModifyManager for additional
     * details.
     *
     * In an interactive application, it is adequate for the UI to focus on the result of the
     * execution of a command. We call it the "focusEntity".
     * The public method getFocusEntity() can be used by the UI, after the execution of
     * a command, to retrieve the entity that should be selected.
     * Each command, in its execute() method, is responsible to set the focus entity
     * by calling setFocusEntity(entity).
     */
    private static Entity focusEntity;

    protected boolean isActingAsUndo = false;
    protected String undoCommandName = null;
    protected String timeofCommandExecution;
    protected String actionStr;

    /**
     * Set if this command is acting as an undo for some other command.  Pass in the
     * Undo command string, or null.
     */
    public void setIsActingAsUndoAndUndoName(boolean isActingAsUndoFlag, String undoCommandName) {
        this.isActingAsUndo = isActingAsUndoFlag;
        if (!isActingAsUndoFlag) this.undoCommandName = null;
        else this.undoCommandName = undoCommandName;
    }

    /**
     * Modifies the local (in-memory) model, returning a command
     * capable of undoing the resultant change. The returned command may
     * be NULL, but must NOT be a reference to this command
     */
    public abstract Command execute() throws Exception;

    /**
     * Set the entity that the UI should focus on after the execution of this command.
     */
    protected void setFocusEntity(Entity focusEntity) {
        this.focusEntity = focusEntity;
    }

    /**
     * Return the entity that the UI should focus on after the execution of this command.
     */
    public static Entity getFocusEntity() {
        return focusEntity;
    }


    /**
     * Performs precondition validation, executes, and postcondition validation.
     * The returned command may
     * be NULL, but must NOT be a reference to this command
     */
    public final Command executeWithValidation() throws Exception {
        // Validate the preconditions...
        this.validatePreconditions();
        // Execute the command (without validation)...
        Command returnCommand = this.execute();
        // Validate the postconditions...
        this.validatePostconditions();

        return returnCommand;
    }

    /**
     * Checks the for valid preconditions, if the precoditions are not met,
     * throws a CommandPreconditionException.
     */
    public void validatePreconditions() throws CommandPreconditionException {
    }

    /**
     * Checks the for valid preconditions, if the precoditions are not met,
     * throws a CommandPreconditionException.
     */
    public void validatePostconditions() throws CommandPostconditionException {
    }


    /**
     * @return the boolean if this command is acting as an undo for some previously
     * executed command.  This affect which string to use to represent the command
     * as well as how the command should transition the Scratch Modified states
     * of the PIs affected by the command (either call transition methods or simply
     * goto previous states for undo).
     */
    public boolean isActingAsUndo() {  return isActingAsUndo;  }

    /**
     * Return the undo command string...
     */
    public String getUndoCommandName() {  return this.undoCommandName;  }

    /**
     * useful for printing command log
     * sub classes give the real implementation
     */
    public /*abstract*/ String getCommandLogMessage(){

        return (this.toString()+"--"+timeofCommandExecution+"--"+actionStr+"\n");
    }

}  // End Command Class.
