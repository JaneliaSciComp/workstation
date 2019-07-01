package org.janelia.workstation.common.actions;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.workstation.core.actions.ContextualNodeAction;
import org.janelia.workstation.core.actions.ContextualNodeActionTracker;
import org.janelia.workstation.core.actions.NodeContext;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.SystemAction;

/**
 * A class which unifies normal Swing Actions (manipulating domain objects) with the NetBeans NodeAction model
 * of acting on selected Nodes. This action can then be returned from both getAction and getNodeAction of
 * a ContextualActionBuilder.
 *
 * When you extend this class, just implement getName(), isVisible(), and executeAction(). Note that
 * domainObjectNodeList will be only populated in the case of the action being invoked via a node context menu.
 *
 * You should not use the NodeAction API to do things like check isEnabled(). Instead, use ContextualActionUtils
 * where possible, as this will produce results that are compatible with both APIs.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BaseContextualNodeAction
        extends CallableSystemAction
        implements ViewerContextReceiver, PopupMenuGenerator, ContextualNodeAction {

    private String name;
    private boolean visible = true;
    private ViewerContext viewerContext;
    private NodeContext nodeContext;

    protected BaseContextualNodeAction(String name) {
        this.name = name;
        setEnabled(false); // default to disabled, only enable when there is a favorable context
        ContextualNodeActionTracker.getInstance().register(this);
    }

    /**
     * Provides a default name for the action, if ContextualActionUtils.getName returns null.
     * @return default name for this action
     */
    public String getName() {
        return name;
    }

    /**
     * Override this to hide the action in certain situations.
     * Specifically, this only works in context menus, not in the main menu.
     * @return true if the action should be visible in context menus
     */
    protected boolean isVisible() {
        return visible;
    }

    protected void setVisible(boolean visible) {
        this.visible = visible;
    }

    protected ViewerContext getViewerContext() {
        return viewerContext;
    }

    protected NodeContext getNodeContext() {
        return nodeContext;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (!ContextualActionUtils.isVisible(this)) {
            return null;
        }
        String name = ContextualActionUtils.getName(this);
        if (name == null) name = getName();
        JMenuItem item = ContextualActionUtils.getNamedActionItem(name, actionEvent -> {
            try {
                performAction();
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        });
        item.setEnabled(ContextualActionUtils.isEnabled(this));
        return item;
    }

    @Override
    public boolean enable(NodeContext nodeContext) {
        this.viewerContext = null;
        this.nodeContext = nodeContext;
        processContext();
        ContextualActionUtils.setVisible(this, isVisible());
        ContextualActionUtils.setEnabled(this, isEnabled());
        return isEnabled();
    }

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        this.viewerContext = viewerContext;
        this.nodeContext = viewerContext.getNodeContext();
        processContext();
        ContextualActionUtils.setVisible(this, isVisible());
        ContextualActionUtils.setEnabled(this, isEnabled());
    }

    /**
     * Override this method to process the current context (as returned by getNodeContext and getViewerContext) and
     * call setEnabled and setVisible to initialize the state of the action before it is displayed for the
     * current context.
     */
    protected void processContext() {
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
