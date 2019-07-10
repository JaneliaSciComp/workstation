package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
        category = "Actions",
        id = "RemoveFromFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_RemoveFromFolderAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 160, separatorAfter = 199)
})
@NbBundle.Messages("CTL_RemoveFromFolderAction=Remove Items From Folder")
public class RemoveFromFolderAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveFromFolderAction.class);

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
        actionPerformed(parentTreeNode, toRemove);
    }

    private void actionPerformed(Node node, Collection<DomainObject> domainObjects) {

        ActivityLogHelper.logUserAction("RemoveItemsFromFolderAction.doAction", node);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final Multimap<Node,DomainObject> removeFromFolders = ArrayListMultimap.create();
        final List<DomainObject> listToDelete = new ArrayList<>();

        for(DomainObject domainObject : domainObjects) {

            DomainObjectHandler provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
            if (provider!=null && provider.supportsRemoval(domainObject)) {
                if (node==null) {
                    listToDelete.add(domainObject);
                }
                else {
                    // first check to make sure this Object only has one ancestor references; if it does pop up a dialog before removal
                    List<Reference> refList = model.getContainerReferences(domainObject);
                    if (refList==null || refList.size()<=1) {
                        listToDelete.add(domainObject);
                    }
                    else {
                        log.info("{} has multiple references: {}", domainObject, refList);
                    }
                }
            }
            else {
                log.trace("Removal not supported for {}", domainObject);
            }

            if (node!=null) {
                log.info("Will remove {} from {}", domainObject, node);
                removeFromFolders.put(node,domainObject);
            }
        }

        if (!listToDelete.isEmpty()) {

            Set<String> allReaders = new HashSet<>();
            for (DomainObject domainObject : listToDelete) {
                allReaders.addAll(domainObject.getReaders());
                allReaders.remove(domainObject.getOwnerKey());
            }
            allReaders.remove(AccessManager.getSubjectKey());

            if (!allReaders.isEmpty()) {
                List<String> sharedWith = allReaders.stream().map(SubjectUtils::getSubjectName).collect(Collectors.toList());
                String sharedWithList = StringUtils.join(sharedWith, ", ");

                int deleteConfirmation = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                        "There are " + listToDelete.size() + " items in your remove list that will be deleted permanently. These items are shared with: "+sharedWithList+". Delete anyway?",
                        "Are you sure?", JOptionPane.YES_NO_OPTION);
                if (deleteConfirmation != 0) {
                    return;
                }
            }
            else {
                int deleteConfirmation = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                        "There are " + listToDelete.size() + " items in your remove list that will be deleted permanently.",
                        "Are you sure?", JOptionPane.YES_NO_OPTION);
                if (deleteConfirmation != 0) {
                    return;
                }
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                // Remove any actual objects that are no longer referenced
                if (!listToDelete.isEmpty()) {
                    log.info("Looking for provider to deleting object entirely: {}", listToDelete);
                    for(DomainObject domainObject : listToDelete) {
                        DomainObjectHandler provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
                        if (provider!=null) {
                            log.info("Using {} to delete object {}", provider.getClass().getName(), domainObject);
                            provider.remove(domainObject);
                        }
                        else {
                            log.warn("No DomainObjectHandler found for {}, cannot delete.",domainObject);
                        }
                    }
                }

                // Delete references
                for (Node node : removeFromFolders.keySet()) {
                    Collection<DomainObject> items = removeFromFolders.get(node);
                    log.info("Removing {} items from {}", items.size(), node);
                    model.removeChildren(node, items);
                }
            }

            @Override
            protected void hadSuccess() {
                // Handled by the event system
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Removing items", "", 0, 100));
        worker.execute();
    }
}
