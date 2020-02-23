package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.browser.actions.RemoveItemsActionListener;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "RemoveItemsAction"
)
@ActionRegistration(
        displayName = "#CTL_RemoveItemsAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions", position = 160, separatorAfter = 199)
})
@NbBundle.Messages("CTL_RemoveItemsAction=Remove Items")
public class RemoveItemsAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveItemsAction.class);

    private Node parentTreeNode;
    private Collection<DomainObject> toRemove = new ArrayList<>();

    @Override
    protected void processContext() {

        this.parentTreeNode = null;
        this.toRemove.clear();

        setEnabledAndVisible(false);

        for (Object object : getNodeContext().getObjects()) {
            if (object instanceof Ontology || object instanceof Workspace) {
                return;
            }
        }

        if (getNodeContext().isSingleNodeOfType(ChildObjectsNode.class)) {
            // Viewer selection
            ViewerContext viewerContext = getViewerContext();
            if (viewerContext!=null) {
                Object contextObject = viewerContext.getContextObject();
                if (contextObject instanceof Node) {
                    this.parentTreeNode = (Node)contextObject;
                    this.toRemove.addAll(DomainUIUtils.getSelectedDomainObjects(viewerContext));
                    setVisible(true);
                    setEnabled(ClientDomainUtils.hasWriteAccess(parentTreeNode));
                }
            }
        }
        else {
            // Node selection
            Collection<org.openide.nodes.Node> selected = new ArrayList<>();
            toRemove.clear();
            parentTreeNode = null;
            for(org.openide.nodes.Node node : getNodeContext().getNodes()) {
                selected.add(node);

                boolean included = true;

                org.openide.nodes.Node parentNode = node.getParentNode();

                if (parentNode instanceof TreeNodeNode) {
                    TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                    Node parentTreeNode = parentTreeNodeNode.getNode();
                    if (this.parentTreeNode==null) {
                        this.parentTreeNode = parentTreeNode;
                    }
                    else if (!this.parentTreeNode.getId().equals(parentTreeNode.getId())) {
                        // Wrong parent
                        included = false;
                    }
                    // Must have write access to parent
                    if (!ClientDomainUtils.hasWriteAccess(parentTreeNode)) {
                        included = false;
                    }
                }

                if (node instanceof AbstractDomainObjectNode) {
                    AbstractDomainObjectNode<?> domainObjectNode = (AbstractDomainObjectNode<?>)node;
                    DomainObject domainObject = domainObjectNode.getDomainObject();
                    if (included) {
                        toRemove.add(domainObject);
                    }
                }
            }

            setEnabledAndVisible(!toRemove.isEmpty() && toRemove.size()==selected.size());
        }
    }

    @Override
    public String getName() {
        if (parentTreeNode==null) {
            return "Delete " + toRemove.size() + " Items";
        }
        return "Remove "+toRemove.size()+" Items";
    }

    @Override
    public void performAction() {
        Node parentTreeNode = this.parentTreeNode;
        Collection<DomainObject> toRemove = new ArrayList<>(this.toRemove);
        RemoveItemsActionListener action = new RemoveItemsActionListener(parentTreeNode, toRemove);
        action.actionPerformed(null);
    }
}
