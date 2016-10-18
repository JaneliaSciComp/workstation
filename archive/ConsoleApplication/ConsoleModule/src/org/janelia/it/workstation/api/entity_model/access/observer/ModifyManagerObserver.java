package org.janelia.it.workstation.api.entity_model.access.observer;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/20/13
 * Time: 3:14 PM
 */
/**
 ModifyManagerObserver provides an interface for objects wishing to
 track the availability of undo, redo, and save operations.
 */
public interface ModifyManagerObserver {

    public void noteCanUndo(String undoCommandName);
    public void noteCanRedo(String redoCommandName);
    public void noteNoUndo();
    public void noteNoRedo();
    public void noteCommandDidStart(String commandName);
    public void noteCommandDidFinish(String commandName, int commandKind);
    public void noteCommandPreconditionException(String commandName);
    public void noteCommandExecutionException(String commandName);
    public void noteCommandPostconditionException(String commandName);
    public void noteCommandStringHistoryListNonEmpty();
    public void noteCommandStringHistoryListEmpty();
}
