package org.janelia.it.workstation.gui.browser.actions;

import java.util.Collection;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Remove items from a tree node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveItemsFromFolderAction implements NamedAction {

    private final TreeNode treeNode;
    private final Collection<DomainObject> domainObjects;

    public RemoveItemsFromFolderAction(TreeNode treeNode, Collection<DomainObject> domainObjects) {
        this.treeNode = treeNode;
        this.domainObjects = domainObjects;
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Remove " + domainObjects.size() + " Items From Folder '"+treeNode.getName()+"'" : "Remove This Item From Folder '"+treeNode.getName()+"'";
    }

    @Override
    public void doAction() {

        ActivityLogHelper.logUserAction("RemoveItemsFromFolderAction.doAction", treeNode);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (!ClientDomainUtils.hasWriteAccess(treeNode)) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "You do not have write access to the folder", "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to remove "+domainObjects.size()+" items from the folder '"+treeNode.getName()+"'?", "Remove items", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                model.removeChildren(treeNode, domainObjects);
            }

            @Override
            protected void hadSuccess() {
                // No need to do anything
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Removing items", "", 0, 100));
        worker.execute();
    }
}
