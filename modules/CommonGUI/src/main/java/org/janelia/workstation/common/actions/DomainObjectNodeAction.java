package org.janelia.workstation.common.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * A class which unifies normal Swing Actions (manipulating domain objects) with the NetBeans NodeAction model
 * of acting on selected Nodes. This action can then be returned from both getAction and getNodeAction of
 * a ContextualActionBuilder.
 *
 * When you extend this class, just implement getName(), setViewerContext(), and executeAction(). Note that
 * only the domain object list will be populated in the case of the action being invoked via a node context menu.
 *
 * You should not use the NodeAction API to do things like check isEnabled(). Instead, use ContextualActionUtils
 * where possible, as this will produce results that are compatible with both APIs.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectNodeAction extends NodeAction implements ViewerContextReceiver, PopupMenuGenerator {

    protected List<DomainObjectNode> domainObjectNodeList;
    protected List<DomainObject> domainObjectList;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        executeAction();
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
                executeAction();
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        });
        item.setEnabled(ContextualActionUtils.isEnabled(this));
        return item;
    }

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        this.domainObjectList = viewerContext.getDomainObjectList();
    }

    protected void executeAction() {
    }

    @Override
    protected boolean asynchronous() {
        // We do our own background processing
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        List<DomainObject> domainObjectList = new ArrayList<>();
        this.domainObjectNodeList = new ArrayList<>();
        for(Node node : activatedNodes) {
            if (node instanceof DomainObjectNode) {
                DomainObjectNode<DomainObject> domainObjectNode = (DomainObjectNode<DomainObject>)node;
                domainObjectNodeList.add(domainObjectNode);
                domainObjectList.add(domainObjectNode.getDomainObject());
            }
        }
        ViewerContext viewerContext = new ViewerContext(null, domainObjectList, null, null, null);
        setViewerContext(viewerContext);
        // Enable state is determined by the popup presenter
        return true;
    }
}
