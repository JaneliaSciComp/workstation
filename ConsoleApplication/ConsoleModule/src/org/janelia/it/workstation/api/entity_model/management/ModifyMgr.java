package org.janelia.it.workstation.api.entity_model.management;

import org.janelia.it.workstation.api.entity_model.access.observer.ModifyManagerObserver;
import org.janelia.it.workstation.shared.util.ThreadQueue;

import java.util.*;

/**
 ModifyManager is a singleton class that mediates changes to the model
 so that they can be undone, redone, and commited to the remote database.
 @see ModifyManagerObserver
 @see org.janelia.it.workstation.api.entity_model.management.Command
 */
public class ModifyMgr {
/* ____________________________________________________________________

  In the following table,
    - exceptions have an asterix prefix for this documentation
    - editTokens are numbered
    - the inverse (undo) for a token is token' (a.k.a. token prime)
      NOTE: the design depends on the token and its inverse being
      different instances, otherwise a *BadCommandInverseException
      BadCommandInverseException will be thrown.
    - stacks are shown as arrays listed bottom to top.
    - save calls "executeRemote" on the tokens in the commit stack
      in order from bottom to top.

  action:   CommitStack:    UndoStack:    RedoStack:
  op 1      [1]             [1']          []
  op 2      [12]            [1'2']        []
  undo      [1]             [1']          [2]
  redo      [12]            [1'2']        []
  op 3      [123]           [1'2'3']      []
  redo      *AbscentUndoException
  undo      [12]            [1'2']        [3]
  op 4      [124]           [1'2'4']      []
  save      []              []            []
  undo      *AbscentUndoException

  CommitStack is no longer supported.

   ____________________________________________________________________
*/
    /**
     * Class debug flag... helps during debug.
     */
    private static boolean DEBUG = false;

    public static final int DO_COMMAND = 0;
    public static final int UNDO_COMMAND = 1;
    public static final int REDO_COMMAND = 2;

    /**
     * A handle to the shared notifciation queue of the model.
     */
    private static ThreadQueue notificationQueue = ModelMgr.getModelMgr().getNotificationQueue();

    // The types of notification.
    private static final int NOTE_CAN_UNDO = 90;
    private static final int NOTE_CAN_REDO = 91;
    private static final int NOTE_NO_UNDO = 92;
    private static final int NOTE_NO_REDO = 93;
    private static final int NOTE_COMMAND_START = 94;
    private static final int NOTE_COMMAND_FINISH = 95;
    private static final int NOTE_COMMAND_PRECONDITION_EXCEPTION = 96;
    private static final int NOTE_COMMAND_EXECUTION_EXCEPTION = 97;
    private static final int NOTE_COMMAND_POSTCONDITION_EXCEPTION = 98;
    private static final int NOTE_ANNOTATIONLOG_NONEMPTY= 99;
    private static final int NOTE_ANNOTATIONLOG_EMPTY= 100;

    /**
     * Singleton ModifyManager instance.
     * @label singleton instance
     * @supplierCardinality 1
     */
    private static ModifyMgr instance = new ModifyMgr();

    /**
     * undoCommandList is an OrderedCollection of CommandTokens.
     * @associates <{Command}>
     * @supplierCardinality 0..*
     * @label undo list
     */
    private List undoCommandList = null;

    /**
     * redoCommandList is an Ordered Collection of CommandTokens.
     * @associates <{Command}>
     * @supplierCardinality 0..*
     * @label redo list
     */
    private List redoCommandList = null;

    /**
     * executedCommandNameStack is an Stack of Strings.
     */
    private Stack executedCommandNameStack = null;

    /**
     *
     */
    private List commandHistoryStringList=null;

    /**
     * observerSet is an unordered Collection of unique ModifyManagerObservers.
     * @associates <{ModifyManagerObserver}>
     * @supplierCardinality 0..*
     * @label observers
     */
    private java.util.HashSet observerSet = new java.util.HashSet();

    /**
     * Stored to return the string for the UnDo message
     */
//    private String undoString;

    /**
     * This private constructor is defined so the compilier won't
     * generate a default constructor, this will prevent other classes from
     * createing instances of this class, instead using the getModifyMgr() method
     * to get at the Singleton instance.
     */
    private ModifyMgr()
    {
        undoCommandList = new ArrayList();
        redoCommandList = new ArrayList();
        executedCommandNameStack = new Stack();

        commandHistoryStringList= Collections.synchronizedList(new ArrayList());
    }



    /**
     * Return a reference to the only (ie Singleton) instance of this class.
     */
    public static ModifyMgr getModifyMgr()
    {
        return instance;
    }

    /**
     * Peek at the most recent command name
     */
    private String peekCommandName()
    {
        return (String)executedCommandNameStack.peek();
    }


    /**
     * Get a string representing the most recent undo command.
     * <JTS> 7/13/00 - This was using gCommitStack instead of gUndoStack.
     */
    private String getLatestUndoCommandText()
    {
        String undoText = null;
        org.janelia.it.workstation.api.entity_model.management.Command command = this.getLatestUndoCommand();
        if ( command != null ) undoText = command.toString();
        return undoText;
    }


    /**
     * Get a string representing the most recent redo command.
     */
    private String getLatestRedoCommandText()
    {
        String redoText = null;
        org.janelia.it.workstation.api.entity_model.management.Command command = this.getLatestRedoCommand();
        if ( command != null ) redoText = command.toString();
        return redoText;
    }


    /**
     * Get the whole Undo Stack (container)...
     * ... or the last undo command?
     * <JTS> Migrate this to getLatestUndoCommand()
     public CommandToken getUndoStack()
     {
     return gUndoStack;
     }
     */
    public org.janelia.it.workstation.api.entity_model.management.Command getLatestUndoCommand()
    {
        return (org.janelia.it.workstation.api.entity_model.management.Command)undoCommandList.get(0);
    }


    /**
     * Get the whole Redo Stack (container)...
     * ... or the last Redo command?
     * <JTS> Migrate this to getLatestRedoCommand()
     public CommandToken getRedoStack()
     {
     return gRedoStack;
     }
     */

    public org.janelia.it.workstation.api.entity_model.management.Command getLatestRedoCommand()
    {
        return (org.janelia.it.workstation.api.entity_model.management.Command)redoCommandList.get(0);
    }




    /**
     * Notify all ModifyManagerObservers of current state.
     * This notification is not sensitive to what has or has not changed.
     */
    private void doNotifyStatusChange()
    {
        // Notify of Undo status...
        if( !undoCommandList.isEmpty() )  {
            this.postNotification(peekCommandName(), NOTE_CAN_UNDO);
        }
        else {
            this.postNotification("", NOTE_NO_UNDO);
        }

        // Notify of Redo status...
        if( !redoCommandList.isEmpty() )  {
            this.postNotification(getLatestRedoCommandText(), NOTE_CAN_REDO);
        }
        else  {
            this.postNotification("", NOTE_NO_REDO);
        }
        //also check if the log list is empty and send notification.
        if(!getCommandHistoryStringList().isEmpty()){
            this.postNotification("", NOTE_ANNOTATIONLOG_NONEMPTY);
        }else{
            this.postNotification("", NOTE_ANNOTATIONLOG_EMPTY);
        }


    }

    /**
     * Notify all ModifyManagerObservers of start of a command.
     */
    private void doNotifyCommandStart(org.janelia.it.workstation.api.entity_model.management.Command command)
    {
        this.postNotification(command.toString(), NOTE_COMMAND_START);
   /*
    ModifyManagerObserver obs;
    for (Iterator ittr = observerSet.iterator(); ittr.hasNext(); ) {
        obs=(ModifyManagerObserver)ittr.next();
        obs.noteCommandDidStart(command.toString());
    }
  */
    }


    /**
     * Notify all ModifyManagerObservers of start of a command.
     */
    private void doNotifyCommandFinish(org.janelia.it.workstation.api.entity_model.management.Command command, int commandKind)
    {
        this.postNotification(command.toString(), NOTE_COMMAND_FINISH, commandKind);
    }

    /**
     * doCommand() executes the command to modify the local model, caches
     * the inverse to allow subsequent undo, and caches the command for
     * subsequent save operations that modify the remote database.
     * @param nextCommand is the command to be executed locally.
     */
    public void doCommand(org.janelia.it.workstation.api.entity_model.management.Command nextCommand)/* throws Exception*/ {
        try {
            nextCommand.validatePreconditions();
            doNotifyCommandStart(nextCommand);
            org.janelia.it.workstation.api.entity_model.management.Command inverseToken = nextCommand.execute();
            flushRedoStack();
            nextCommand.validatePostconditions();
            doNotifyCommandFinish(nextCommand, DO_COMMAND);
            pushCommandName(nextCommand.toString());
            addDoCommandToCommandHistoryList(nextCommand);
            if( inverseToken == nextCommand )
                throw new org.janelia.it.workstation.api.entity_model.management.BadCommandInverseException( nextCommand );
            if ( inverseToken != null )
                pushUndoToken( inverseToken );
            doNotifyStatusChange();

            if ( DEBUG ) dumpStacks();
        }
        catch (CommandPreconditionException preEx) {
            this.postNotification(nextCommand.toString(), NOTE_COMMAND_PRECONDITION_EXCEPTION);
            ModelMgr.getModelMgr().handleException(preEx);
        }
        catch (CommandExecutionException execEx) {
            // Unknown state! automatically roll-back?
            // We could if we already had the UNDO, even before execution!
            this.postNotification(nextCommand.toString(), NOTE_COMMAND_EXECUTION_EXCEPTION);
            ModelMgr.getModelMgr().handleException(execEx);
        }
        catch (org.janelia.it.workstation.api.entity_model.management.CommandPostconditionException postEx) {
            // @todo We should roll-back here as we do have the undo-command.
            this.postNotification(nextCommand.toString(), NOTE_COMMAND_POSTCONDITION_EXCEPTION);
            ModelMgr.getModelMgr().handleException(postEx);
        }
        catch (IllegalAccessError iaErr) {
            // Entity does not permit mutation.
            // Simply ignore the exception since no modification occurred
            // At it should not have been permitted in the first place.
            System.out.println("Modification operation denied. " + iaErr.getMessage());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }  // End of doCommand() method.



    /**
     * canUndoCommand() indicates if that the last Command can be undone.
     */
    public boolean canUndoCommand() {
        return !undoCommandList.isEmpty();
    }


    /**
     * undoCommand() undoes the last action to the local model, caches
     * the inverse to allow subsequent redo, and erases the undone command
     * from the list of actions to perform on a subsequent save request.
     */
    public void undoCommand() {
        try {
            org.janelia.it.workstation.api.entity_model.management.Command undoToken = popUndoToken();
            doNotifyCommandStart(undoToken);
            org.janelia.it.workstation.api.entity_model.management.Command redoToken = undoToken.execute();
            doNotifyCommandFinish(undoToken, UNDO_COMMAND);
            popCommandName();
            if( redoToken == undoToken )
                throw new org.janelia.it.workstation.api.entity_model.management.BadCommandInverseException( undoToken );
            pushRedoToken( redoToken );
            doNotifyStatusChange();

            if ( DEBUG ) dumpStacks();
        }
        catch (Exception ex) {
            try {
                ModelMgr.getModelMgr().handleException(ex);
            }
            catch (Exception ex1) {
                ex.printStackTrace();
            }
        }
    }



    /**
     * canRedoCommand() indicates if that the last undo can be undone.
     */
    public boolean canRedoCommand(){ return !redoCommandList.isEmpty(); }


    /**
     * redoCommand() logically revokes the last undo operation, and caches
     * the redo command for subsequent save operations that modify the
     * remote database.
     */
    public void redoCommand()  {
        try {
            org.janelia.it.workstation.api.entity_model.management.Command redoToken = popRedoToken();
            doNotifyCommandStart(redoToken);
            org.janelia.it.workstation.api.entity_model.management.Command undoToken = redoToken.execute();
            doNotifyCommandFinish(redoToken, REDO_COMMAND);
            pushRedoCommandName(redoToken.toString());
            if( undoToken == redoToken )
                throw new org.janelia.it.workstation.api.entity_model.management.BadCommandInverseException( redoToken );
            pushUndoToken( undoToken );
            doNotifyStatusChange();

            if ( DEBUG ) dumpStacks();
        }
        catch (Exception ex) {
            try {
                ModelMgr.getModelMgr().handleException(ex);
            }
            catch (Exception ex1) {
                ex.printStackTrace();
            }
        }
    }



    /*____________________________________________________________

    Observation mechanism
    ____________________________________________________________
    */

    /**
     * Add a ModifyManagerObserver.
     */
    public void addObserver( ModifyManagerObserver observer )
    {
        observerSet.add( observer );
    }


    /**
     * Remove a ModifyManagerObserver.
     */
    public void removeObserver( ModifyManagerObserver observer )
    {
        observerSet.remove( observer );
    }


    private Set getObserversToNotify() {
        return (HashSet)observerSet.clone();
    }


    /**
     * Post notification of new details on  the notification queue.
     */
    private void postNotification(String commandName, int notificationType) {
        //Run other notification in the notification queue
        notificationQueue.addQueue(new ModifyManagerNotificationObject(
                notificationType, commandName));
    }

    private void postNotification(String commandName, int notificationType,
                                  int commandKind) {
        //Run other notification in the notification queue
        notificationQueue.addQueue(new ModifyManagerNotificationObject(
                notificationType, commandName, commandKind));
    }


    /*____________________________________________________________

    private utilities
    ____________________________________________________________
    */



    /**
     * Pop the latest undo command off the stack & return it.
     * If the undo stack is now empty, notify the observers.
     */
    private org.janelia.it.workstation.api.entity_model.management.Command popUndoToken() throws org.janelia.it.workstation.api.entity_model.management.AbsentUndoException
    {
        org.janelia.it.workstation.api.entity_model.management.Command undoToken = (org.janelia.it.workstation.api.entity_model.management.Command)undoCommandList.get(0);
        // If we don't have any, throw an exception...
        if( undoToken == null )  throw new org.janelia.it.workstation.api.entity_model.management.AbsentUndoException();
            // Otherwise, remove it.
        else  {
            commandHistoryStringList.add("Undo "+undoToken.getCommandLogMessage());
            undoCommandList.remove(0);
        }
        // If I've run out of undo tokens, notify observers...
        if( undoCommandList.isEmpty() ) {
            this.postNotification("",NOTE_NO_UNDO);
  /*
                    ModifyManagerObserver Obs;
                    for (Iterator ittr = observerSet.iterator(); ittr.hasNext(); ) {
                            Obs=(ModifyManagerObserver)ittr.next();
                            Obs.noteNoUndo();
                    }
  */
        }
        return undoToken;
    }

    /**
     * Pop the latest commandName
     */
    private String popCommandName()
    {
        // commandHistoryList.add("DO:"+(String)executedCommandNameStack.peek());
        return (String)executedCommandNameStack.pop();
    }


    /**
     * Pop the latest redo command off the stack & return it.
     * If the redo stack is now empty, notify the observers.
     */
    private org.janelia.it.workstation.api.entity_model.management.Command popRedoToken() throws org.janelia.it.workstation.api.entity_model.management.AbsentRedoException
    {
        org.janelia.it.workstation.api.entity_model.management.Command redoToken = (org.janelia.it.workstation.api.entity_model.management.Command)redoCommandList.get(0);
        // If we don't have any, throw an exception...
        if( redoToken == null ) throw new org.janelia.it.workstation.api.entity_model.management.AbsentRedoException();
            // Otherwise, remove it.
        else {
            commandHistoryStringList.add("Redo "+redoToken.getCommandLogMessage());
            redoCommandList.remove(0);
        }
        // If I've run out of redo tokens, notify observers...
        if( redoCommandList.isEmpty() ) {
            this.postNotification("",NOTE_NO_REDO);
  /*
                    ModifyManagerObserver Obs;
                    for (Iterator ittr = observerSet.iterator(); ittr.hasNext(); ) {
                            Obs=(ModifyManagerObserver)ittr.next();
                            Obs.noteNoRedo();
                    }
  */
        }
        return redoToken;
    }



    /**
     * Push a command onto the front of the Undo stack.
     * If the command argument is null, flush the Undo stack.
     */
    private void pushUndoToken( org.janelia.it.workstation.api.entity_model.management.Command undoToken )
    {
        // if no undo token was returned (passed), flush the undo stack
        if( undoToken == null ) flushUndoStack();
            // otherwise push the token onto the top of the undo stack
        else undoCommandList.add(0,undoToken);
    }

    /**
     * Push the last executed command name.
     * If the command argument is null, flush the redo stack.
     */
    private void pushCommandName ( String commandName )
    {
        // if no commandName was returned, flush the commandName stack
        if( commandName == null ) flushCommandNameStack();
            // otherwise push the token onto the top of the undo stack
        else{
            executedCommandNameStack.push(commandName);
            // commandHistoryStringList.add("DO:"+commandName);
        }
    }



    private void addDoCommandToCommandHistoryList(org.janelia.it.workstation.api.entity_model.management.Command c){
        String commlogmessg=c.getCommandLogMessage();
        commandHistoryStringList.add("Do "+commlogmessg);
    }



    /**
     * Push the last executed command name.
     * If the command argument is null, flush the redo stack.
     */
    private void pushRedoCommandName ( String commandName )
    {
        // if no commandName was returned, flush the commandName stack
        if( commandName == null ) flushCommandNameStack();
            // otherwise push the token onto the top of the undo stack
        else{
            executedCommandNameStack.push(commandName);

        }
    }


    /**
     * Push a command onto front of the Redo stack.
     * If the command argument is null, flush the redo stack.
     */
    private void pushRedoToken( org.janelia.it.workstation.api.entity_model.management.Command redoToken )
    {
        // if no redo token was returned, flush the redo stack
        if( redoToken == null ) flushRedoStack();
            // otherwise push the token onto the top of the undo stack
        else redoCommandList.add(0, redoToken);
    }

    /**
     * Empty the commandName stack
     */
    private void flushCommandNameStack()
    {
        executedCommandNameStack.clear();
    }

    /**
     * Empty the undo stack
     * If the undo stack was previously non-empty, notify observers that NOW
     * you can undo.
     */
    private void flushUndoStack()
    {
        boolean DoNotify = !undoCommandList.isEmpty();
        undoCommandList.clear();
        // notify observers if I previously had undo tokens...
        if (DoNotify) {
            this.postNotification("",NOTE_NO_UNDO);
  /*
                    ModifyManagerObserver Obs;
                    for (Iterator ittr = observerSet.iterator(); ittr.hasNext(); ) {
                            Obs=(ModifyManagerObserver)ittr.next();
                            Obs.noteNoUndo();
                    }
  */
        }
    }



    /**
     * Empty the Redo stack
     * If the Redo stack was previously non-empty, notify observers that NOW
     * you can Redo.
     */
    private void flushRedoStack()
    {
        boolean DoNotify = !redoCommandList.isEmpty();
        redoCommandList.clear();
        // notify observers if I previously had redo tokens...
        if (DoNotify) {
            this.postNotification("",NOTE_NO_REDO);
  /*
                    ModifyManagerObserver Obs;
                    for (Iterator ittr = observerSet.iterator(); ittr.hasNext(); ) {
                            Obs=(ModifyManagerObserver)ittr.next();
                            Obs.noteNoRedo();
                    }
  */
        }
    }


    public synchronized void printExecutedCommandStack(){

        if(!commandHistoryStringList.isEmpty()){
            for(Iterator iter=commandHistoryStringList.iterator();iter.hasNext();){
                String commandStr=(String)iter.next();
                System.out.println(commandStr);
            }
        }
    }


    /**
     * To be used by AnnotationLogWriter and AnnotationLogViewer
     */

    public synchronized List getCommandHistoryStringList(){
        return commandHistoryStringList;


    }

    /**
     * Print a dump of all the stacks.
     */
    public void dumpStacks()
    {
        System.out.println("<---Begin DumpStacks--->");
        // Print the Undo Stack...
        if ( !undoCommandList.isEmpty() )  {
            System.out.print("UndoStack: ");
            for (Iterator i = undoCommandList.iterator(); i.hasNext(); )
                System.out.print(" ["+ i.next().toString()+"]");
            System.out.println();
        }
        else
            System.out.println("UndoStack: Is Empty");

        // Print the Redo Stack...
        if ( !redoCommandList.isEmpty() )  {
            System.out.print("RedoStack: ");
            for (Iterator i = redoCommandList.iterator(); i.hasNext(); )
                System.out.print(" ["+ i.next().toString()+"]");
            System.out.println();
        }
        else
            System.out.println("RedoStack: Is Empty");

        System.out.println("<---End DumpStacks--->");
        System.out.println();
    }

    /**
     * Will reset the ModifyManager
     */
    public void flushStacks() {
        flushCommandNameStack();
        flushRedoStack();
        flushUndoStack();
        commandHistoryStringList.clear();
    }


//****************************************
//*  Inner classes
//****************************************


    // notification class
    protected class ModifyManagerNotificationObject implements Runnable {
        int notedAction;
        private String commandName;
        private int commandKind;

        /**
         * Constructor.
         */
        ModifyManagerNotificationObject(int aNotedAction, String aCommandName) {
            this.notedAction = aNotedAction;
            this.commandName = aCommandName;
            this.commandKind = 0;
        }

        ModifyManagerNotificationObject(int aNotedAction, String aCommandName, int commandKind) {
            this.notedAction = aNotedAction;
            this.commandName = aCommandName;
            this.commandKind = commandKind;
        }

        public void run() {
            sendEntityChangedMessage();
        }

        private void sendEntityChangedMessage() {
            Set observersToNotify = getObserversToNotify();
            ModifyManagerObserver[] observers = (ModifyManagerObserver[])observersToNotify.toArray(
                    new ModifyManagerObserver[observersToNotify.size()]);
            ModifyManagerObserver observer;
            for( int i= 0; i< observers.length; i++ ) {
                if (observers[i] instanceof ModifyManagerObserver) {
                    observer=observers[i];
                    switch (notedAction) {
                        case NOTE_CAN_UNDO: {
                            observer.noteCanUndo(commandName);
                            break;
                        }
                        case NOTE_CAN_REDO: {
                            observer.noteCanRedo(commandName);
                            break;
                        }
                        case NOTE_NO_UNDO: {
                            observer.noteNoUndo();
                            break;
                        }
                        case NOTE_NO_REDO: {
                            observer.noteNoRedo();
                            break;
                        }
                        case NOTE_COMMAND_START: {
                            observer.noteCommandDidStart(commandName);
                            break;
                        }
                        case NOTE_COMMAND_FINISH: {
                            observer.noteCommandDidFinish(commandName, commandKind);
                            break;
                        }
                        case NOTE_COMMAND_PRECONDITION_EXCEPTION: {
                            observer.noteCommandPreconditionException(commandName);
                            break;
                        }
                        case NOTE_COMMAND_EXECUTION_EXCEPTION: {
                            observer.noteCommandExecutionException(commandName);
                            break;
                        }
                        case NOTE_COMMAND_POSTCONDITION_EXCEPTION: {
                            observer.noteCommandPostconditionException(commandName);
                            break;
                        }
                        case NOTE_ANNOTATIONLOG_NONEMPTY: {
                            observer.noteCommandStringHistoryListNonEmpty();
                            break;
                        }
                        case NOTE_ANNOTATIONLOG_EMPTY: {
                            observer.noteCommandStringHistoryListEmpty();
                            break;
                        }

                        default: {
                            throw new RuntimeException ("Message "+notedAction+
                                    " not defined in ModifyManager");
                        }
                    }  // End Switch
                }  // End If
            }  // End for
        }  // End Method
    }
}
