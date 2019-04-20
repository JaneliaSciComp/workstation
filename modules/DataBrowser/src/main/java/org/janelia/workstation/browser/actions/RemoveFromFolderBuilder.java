package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=160)
public class RemoveFromFolderBuilder implements ContextualActionBuilder {

    private final static Logger log = LoggerFactory.getLogger(RemoveFromFolderBuilder.class);

    private static final RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction();
    private static final RemoveAction nodeAction = new RemoveAction();

    @Override
    public boolean isCompatible(Object obj) {
        // TODO: unify deletion of ontologies, currently handled in RemoveOntologyTermBuilder
        return obj instanceof DomainObject && !(obj instanceof Ontology);
    }

    @Override
    public boolean isSucceededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    private static class RemoveItemsFromFolderAction extends AbstractAction implements ViewerContextReceiver {

        private org.janelia.model.domain.workspace.Node parentTreeNode;
        private Collection<DomainObject> toRemove;

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            Object contextObject = viewerContext.getContextObject();
            log.info("RemoveItemsFromFolderAction contextObject="+contextObject);
            if (contextObject instanceof org.janelia.model.domain.workspace.Node) {
                this.parentTreeNode = (org.janelia.model.domain.workspace.Node) contextObject;
                this.toRemove = viewerContext.getDomainObjectList();
                ContextualActionUtils.setVisible(this, true);
                ContextualActionUtils.setEnabled(this, ClientDomainUtils.hasWriteAccess(parentTreeNode));
                ContextualActionUtils.setName(this, getName(parentTreeNode, toRemove));
            }
            else {
                ContextualActionUtils.setVisible(this, false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            RemoveFromFolderBuilder.actionPerformed(parentTreeNode, toRemove);
        }
    }

    public static final class RemoveAction extends NodeAction {

        private final List<Node> selected = new ArrayList<>();
        private List<DomainObject> toRemove = new ArrayList<>();
        private org.janelia.model.domain.workspace.Node parentTreeNode;

        @Override
        public String getName() {
            return "Remove "+toRemove.size()+" items";
        }

        @Override
        public HelpCtx getHelpCtx() {
            return new HelpCtx("RemoveAction");
        }

        @Override
        protected boolean asynchronous() {
            return false;
        }

        @Override
        protected boolean enable(org.openide.nodes.Node[] activatedNodes) {
            selected.clear();
            toRemove.clear();
            parentTreeNode = null;
            for(org.openide.nodes.Node node : activatedNodes) {
                selected.add(node);

                boolean included = true;

                org.openide.nodes.Node parentNode = node.getParentNode();

                if (parentNode instanceof TreeNodeNode) {
                    TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                    org.janelia.model.domain.workspace.Node parentTreeNode = parentTreeNodeNode.getNode();
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
            return toRemove.size()==selected.size();
        }

        @Override
        protected void performAction(org.openide.nodes.Node[] activatedNodes) {
            RemoveFromFolderBuilder.actionPerformed(parentTreeNode, toRemove);
        }
    }

    private static String getName(org.janelia.model.domain.workspace.Node node, Collection<DomainObject> domainObjects) {
        if (node==null) {
            return domainObjects.size() > 1 ? "Delete " + domainObjects.size() + " Items" : "Delete This Item";
        }
        if (node.getName()==null) {
            return domainObjects.size() > 1 ? "Remove " + domainObjects.size() + " Items From Folder" : "Remove This Item From Folder";
        }
        return domainObjects.size() > 1 ? "Remove " + domainObjects.size() + " Items From Folder '"+node.getName()+"'" : "Remove This Item From Folder '"+node.getName()+"'";
    }

    private static void actionPerformed(org.janelia.model.domain.workspace.Node node, Collection<DomainObject> domainObjects) {

        ActivityLogHelper.logUserAction("RemoveItemsFromFolderAction.doAction", node);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final Multimap<org.janelia.model.domain.workspace.Node,DomainObject> removeFromFolders = ArrayListMultimap.create();
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
                for (org.janelia.model.domain.workspace.Node node : removeFromFolders.keySet()) {
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
