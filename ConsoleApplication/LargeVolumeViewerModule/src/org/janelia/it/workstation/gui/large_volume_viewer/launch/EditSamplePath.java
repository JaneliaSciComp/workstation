package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectAcceptor;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.browser.workers.SimpleWorker;
import org.openide.util.lookup.ServiceProvider;

/**
 * Right-click context menu that allows user to edit a TmSample file path.
 */
@ServiceProvider(service = DomainObjectAcceptor.class, path = DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH)
public class EditSamplePath implements DomainObjectAcceptor  {
    
    private static final int MENU_ORDER = 400;
    
    public EditSamplePath() {
    }

    @Override
    public void acceptDomainObject(DomainObject domainObject) {

        final TmSample sample = (TmSample)domainObject;
        
        final String editedPath = (String) JOptionPane.showInputDialog(
                ConsoleApp.getMainFrame(),
                "New Linux path to sample:",
                "Edit sample path",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list
                sample.getFilepath()
        );
        if (editedPath == null || editedPath.length() == 0) {
            // canceled
            return;
        } 
        else {
            SimpleWorker saver = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    sample.setFilepath(editedPath);
                    TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                }
                @Override
                protected void hadSuccess() {
                	// Handled by event system
                }
                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };
            saver.execute();
        }
    }

    @Override
    public String getActionLabel() {
        return "  Edit Sample Path";
    }

    @Override
    public boolean isCompatible(DomainObject e) {
        return e != null && (e instanceof TmSample);
    }

    @Override
    public boolean isEnabled(DomainObject e) {
        return e != null && ClientDomainUtils.hasWriteAccess(e);
    }
    
    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }
}
