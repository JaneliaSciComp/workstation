package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 * Create any "Tiled Microscope Sample" items (buttons, menu items, etc.) based upon this.
 */
public class SaveTiledMicroscopeSampleAction extends AbstractAction {

    private TmSample sample;
    private String name, octreePath, ktxPath, rawPath;

    public SaveTiledMicroscopeSampleAction(TmSample sample) {
        this.sample = sample;
    }

    public SaveTiledMicroscopeSampleAction(TmSample sample, String name, String octreePath, String ktxPath, String rawPath) {
        super("Create Tiled Microscope Sample");
        this.sample = sample;
        this.name = name;
        this.octreePath = octreePath;
        this.ktxPath = ktxPath;
        this.rawPath = rawPath;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        
        final Component mainFrame = FrameworkAccess.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private TmSample newSample;

            @Override
            protected void doStuff() throws Exception {
                    if (sample != null) {
                        if (name != null) {
                            sample.setName(name);
                        }
                        if (octreePath != null) {
                            DomainUtils.setFilepath(sample, FileType.LargeVolumeOctree, octreePath);
                        }
                        if (ktxPath != null) {
                            DomainUtils.setFilepath(sample, FileType.LargeVolumeKTX, ktxPath);
                        }
                        if (rawPath != null) {
                            DomainUtils.setFilepath(sample, FileType.TwoPhotonAcquisition, rawPath);
                        }
                        newSample = TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                    }
                    else {
                        newSample = TiledMicroscopeDomainMgr.getDomainMgr().createSample(name, octreePath, ktxPath, rawPath);
                    }
            }
            
            @Override
            protected void hadSuccess() {
                if (null != newSample) {
                    JOptionPane.showMessageDialog(mainFrame, "Sample " + newSample.getName() + " added successfully.",
                            "Add New Tiled Microscope Sample", JOptionPane.PLAIN_MESSAGE, null);
                    DomainMgr.getDomainMgr().getModel().invalidateAll();
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Error adding sample " + name + " at " + octreePath +
                                    ". Check that path exists and is accessible. If you continue to experience problems please contact support.",
                            "Failed to Add Tiled Microscope Sample", JOptionPane.ERROR_MESSAGE, null);
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException("Error saving sample - make sure the provided sample paths are correct.", error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Saving sample...", ""));
        worker.execute();
    }
}
