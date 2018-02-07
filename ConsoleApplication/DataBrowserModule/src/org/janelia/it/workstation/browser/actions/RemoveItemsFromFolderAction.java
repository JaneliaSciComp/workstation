package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Remove items from a node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveItemsFromFolderAction extends AbstractAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveItemsFromFolderAction.class);

    private final Node node;
    private final Collection<DomainObject> domainObjects;

    public RemoveItemsFromFolderAction(Node node, Collection<DomainObject> domainObjects) {
        super(getName(node, domainObjects));
        this.node = node;
        this.domainObjects = domainObjects;
    }

    public static final String getName(Node node, Collection<DomainObject> domainObjects) {
        if (node==null) {
            return domainObjects.size() > 1 ? "Delete " + domainObjects.size() + " Items" : "Delete This Item";
        }
        return domainObjects.size() > 1 ? "Remove " + domainObjects.size() + " Items From Folder '"+node.getName()+"'" : "Remove This Item From Folder '"+node.getName()+"'";
    }

    @Override
    public void actionPerformed(ActionEvent event) {
    	
        ActivityLogHelper.logUserAction("RemoveItemsFromFolderAction.doAction", node);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final Multimap<Node,DomainObject> removeFromFolders = ArrayListMultimap.create();
        final List<DomainObject> listToDelete = new ArrayList<>();

        for(DomainObject domainObject : domainObjects) {

            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
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
                log.info("Will removing {} from {}", domainObject, node);
                removeFromFolders.put(node,domainObject);
            }
        }
        
        if (!listToDelete.isEmpty()) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(),
                    "There are " + listToDelete.size() + " items in your remove list that will be deleted permanently.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                // Remove any actual objects that are no longer referenced
                if (!listToDelete.isEmpty()) {
                    log.info("Looking for provider to deleting object entirely: {}", listToDelete);
                    for(DomainObject domainObject : listToDelete) {
                        DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
                        if (provider!=null) {
                            log.info("Using DomainObjectHelper {} to delete object {}", provider, domainObject);
                            provider.remove(domainObject);
                        }
                        else {
                            log.warn("No DomainObjectHelper found for {}, cannot delete.",domainObject);
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
                ConsoleApp.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(ConsoleApp.getMainFrame(), "Removing items", "", 0, 100));
        worker.execute();
    }
}
