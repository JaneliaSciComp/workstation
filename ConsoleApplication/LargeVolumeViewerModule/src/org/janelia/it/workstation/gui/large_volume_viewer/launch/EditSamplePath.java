package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.openide.util.lookup.ServiceProvider;

/**
 * Right-click context menu that allows user to edit a TmSample file path.
 */
@ServiceProvider(service = ObjectOpenAcceptor.class, path = ObjectOpenAcceptor.LOOKUP_PATH)
public class EditSamplePath implements ObjectOpenAcceptor  {
    
    private static final int MENU_ORDER = 400;
    
    public EditSamplePath() {
    }

    @Override
    public void acceptObject(Object obj) {

        final TmSample sample = (TmSample)obj;
        
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
    public boolean isCompatible(Object obj) {
        return obj != null && (obj instanceof TmSample);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return obj != null && ClientDomainUtils.hasWriteAccess((DomainObject)obj);
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
