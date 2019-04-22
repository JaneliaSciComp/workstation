package org.janelia.workstation.common.actions;

import javax.swing.Action;

import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * A builder for simple named actions which only refer to the object that was selected by the user.
 *
 * Just implement getName and performAction, and this base class takes care of action creation and exception handling.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SimpleActionBuilder implements ContextualActionBuilder {

    @Override
    public Action getAction(Object contextObject) {
        return new SimpleAction(getName(), contextObject, supportsMultipleSelection());
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
     * Return false here if your action should not appear when the user has selected multiple items.
     * @return
     */
    protected boolean supportsMultipleSelection() {
        return false;
    }

    /**
     * Perform the action on the given contextual object. Before this method is called, it's guaranteed
     * that isCompatible returned true for the same object.
     * @param contextObject an object which is compatible with this builder
     * @throws Exception any exceptions are caught and handled in the standard way
     */
    protected abstract void performAction(Object contextObject) throws Exception;

    private class SimpleAction extends ViewerContextAction {

        private String name;
        private Object contextObject;
        private boolean supportsMultipleSelection;

        SimpleAction(String name, Object contextObject, boolean supportsMultipleSelection) {
            this.name = name;
            this.contextObject = contextObject;
            this.supportsMultipleSelection = supportsMultipleSelection;
            ContextualActionUtils.setName(this, name);
        }

        protected Boolean isVisible() {
            return getViewerContext()==null || getViewerContext().isMultiple()==supportsMultipleSelection;
        }

        @Override
        public String getName() {
            return name;
        }

        protected void executeAction() {
            try {
                SimpleActionBuilder.this.performAction(contextObject);
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        }
    }
}
