package org.janelia.workstation.integration.spi.domain;

import javax.swing.Action;

import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * A builder for simple named actions. Just implement getName and performAction.
 *
 * Takes care of action creation and exception handling.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SimpleActionBuilder implements ContextualActionBuilder {

    @Override
    public Action getAction(Object contextObject) {
        Action action = ContextualActionUtils.getNamedAction(getName(), actionEvent -> {
            try {
                performAction(contextObject);
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        });
        // Set enabled state
        action.setEnabled(isEnabled(contextObject));
        return action;
    }

    @Override
    public Action getNodeAction(Object contextObject) {
        return getAction(contextObject);
    }

    /**
     * Implement this to return a user-readable label for the action, which will be displayed in
     * e.g. in a popup menu.
     * @return a label
     */
    protected abstract String getName();

    /**
     * Perform the action on the given contextual object. Before this method is called, it's guaranteed
     * that isCompatible returned true for the same object.
     * @param contextObject an object which is compatible with this builder
     * @throws Exception any exceptions are caught and handled in the standard way
     */
    protected abstract void performAction(Object contextObject) throws Exception;
}
