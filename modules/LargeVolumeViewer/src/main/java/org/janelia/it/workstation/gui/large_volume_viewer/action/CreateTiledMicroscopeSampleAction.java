package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 * Create any "Tiled Microscope Sample" items (buttons, menu items, etc.) based upon this.
 */
public class CreateTiledMicroscopeSampleAction extends AbstractAction {

    private String name, pathToRenderFolder;

    public CreateTiledMicroscopeSampleAction(String name, String pathToRenderFolder) {
        super("Create Tiled Microscope Sample");
        this.name = name;
        this.pathToRenderFolder = pathToRenderFolder;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        
        final Component mainFrame = FrameworkAccess.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private TmSample newSample;

            @Override
            protected void doStuff() throws Exception {
                newSample = TiledMicroscopeDomainMgr.getDomainMgr().createSample(name, pathToRenderFolder);
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
                FrameworkAccess.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating sample...", ""));
        worker.execute();
    }
}
