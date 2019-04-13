package org.janelia.console.viewerapi.commands;

/**
 *
 * @author brunsc
 */
public interface Notifier {
    // Top level semantic Commands, i.e. those that make it into the Undo/Redo
    //  menu, should notify listeners of completed model changes.
    // On the other hand, sub-Commands should maybe wait until the top level 
    //  Command has completed. For performance reasons.
    void setNotify(boolean doNotify);
    boolean doesNotify();
}
