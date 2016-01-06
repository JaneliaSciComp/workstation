package org.janelia.it.workstation.gui.browser.actions;

import java.util.Collection;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Remove items from an object set.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveItemsFromObjectSetAction implements NamedAction {

    private final ObjectSet objectSet;
    private final Collection<DomainObject> domainObjects;

    public RemoveItemsFromObjectSetAction(ObjectSet objectSet, Collection<DomainObject> domainObjects) {
        this.objectSet = objectSet;
        this.domainObjects = domainObjects;
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Remove \"" + domainObjects.size() + "\" Items From Set '"+objectSet.getName()+"'" : "  Remove This Item From Set '"+objectSet.getName()+"'";
    }

    @Override
    public void doAction() {
    
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (!ClientDomainUtils.hasWriteAccess(objectSet)) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "You do not have write access to the set", "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to remove "+domainObjects.size()+" items from the set '"+objectSet.getName()+"'?", "Remove items", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                model.removeMembers(objectSet, domainObjects);        
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
