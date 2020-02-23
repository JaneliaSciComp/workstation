package org.janelia.workstation.common.actions;

import java.util.MissingResourceException;

import javax.swing.JMenuItem;

import org.janelia.workstation.core.actions.ContextualNodeAction;
import org.janelia.workstation.core.actions.ContextualNodeActionTracker;
import org.janelia.workstation.core.actions.NodeContext;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class which unifies normal Swing actions (manipulating domain objects) with the NetBeans NodeAction model
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
        implements PopupMenuGenerator, ContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(BaseContextualNodeAction.class);

    private String name;
    private boolean visible = true;
    private ViewerContext viewerContext;
    private NodeContext nodeContext;

    protected BaseContextualNodeAction() {
        setEnabledAndVisible(false); // default to false, only enable when there is a favorable context
        ContextualNodeActionTracker.getInstance().register(this);
    }

    /**
     * Provides a default name for the action, if ContextualActionUtils.getName returns null.
     * @return default name for this action
     */
    @Override
    public String getName() {
        try {
            return NbBundle.getBundle(getClass()).getString("CTL_"+getClass().getSimpleName());
        }
        catch (MissingResourceException e) {
            log.warn("Problem loading display name", e);
            return getClass().getSimpleName();
        }
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

    protected void setEnabledAndVisible(boolean visible) {
        setEnabled(visible);
        setVisible(visible);
    }

    protected ViewerContext getViewerContext() {
        return viewerContext;
    }

    protected NodeContext getNodeContext() {
        return nodeContext;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (!isVisible()) return null;
        JMenuItem item = new JMenuItem(this);
        item.setEnabled(isEnabled());
        return item;
    }

    @Override
    public boolean enable(NodeContext nodeContext, ViewerContext viewerContext) {
        this.viewerContext = viewerContext;
        this.nodeContext = nodeContext;
        processContext();
        return isEnabled();
    }

    /**
     * Override this method to process the current context (as returned by getNodeContext and getViewerContext) and
     * call setEnabled and setVisible to initialize the state of the action before it is displayed for the
     * current context.
     */
    protected void processContext() {
    }

    protected boolean asynchronous() {
        return false;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
