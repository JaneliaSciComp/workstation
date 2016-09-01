package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.gui.browser.actions.NamedAction;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Create any "Tiled Microscope Sample" items (buttons, menu items, etc.) based upon this.
 */
public class CreateTiledMicroscopeSampleAction implements NamedAction {

    private String name, pathToRenderFolder;

    public CreateTiledMicroscopeSampleAction(String name, String pathToRenderFolder) {
        this.name = name;
        this.pathToRenderFolder = pathToRenderFolder;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public void doAction() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            }
            
            @Override
            protected void hadSuccess() {

                SimpleWorker worker = new SimpleWorker() {
                    
                    private TmSample newSample;

                    @Override
                    protected void doStuff() throws Exception {
                    	newSample = TiledMicroscopeDomainMgr.getDomainMgr().createTiledMicroscopeSample(name, pathToRenderFolder);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        if (null!=newSample) {
                            JOptionPane.showMessageDialog(mainFrame, "Sample " + newSample.getName() + " added successfully.",
                                    "Add New Tiled Microscope Sample", JOptionPane.PLAIN_MESSAGE, null);
                            DomainMgr.getDomainMgr().getModel().invalidateAll();
                        }
                        else {
                            JOptionPane.showMessageDialog(mainFrame, "Error adding sample " + name + ". Please contact support.",
                                    "Failed to Add Tiled Microscope Sample", JOptionPane.ERROR_MESSAGE, null);
                        }
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating sample...", ""));
                worker.execute();
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }
}
