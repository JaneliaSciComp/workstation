package org.janelia.workstation.core.actions;

/**
 * Similar to NetBeans' NodeAction, this interface allows an action to hear about the current node selection.
 * The twist is that it comes in aggregated form, as the NodeContext object.
 * @see NodeContext
 * @see ContextualNodeActionTracker
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ContextualNodeAction {

    /**
     * Given the current node selection, enable or disable the action.
     * @param nodeContext
     * @return
     */
    boolean enable(NodeContext nodeContext);
}
