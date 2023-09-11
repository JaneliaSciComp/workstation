package org.janelia.workstation.controller.action;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 * Create any "Horta Sample" items (buttons, menu items, etc.) based upon this.
 */
public class SaveTiledMicroscopeSampleAction extends AbstractAction {

    private TmSample sample;
    private String name, octreePath, ktxPath, altPath;
    private boolean rawCompressed;

    public SaveTiledMicroscopeSampleAction(TmSample sample) {
        this.sample = sample;
    }

    public SaveTiledMicroscopeSampleAction(TmSample sample, String name, String octreePath, String ktxPath, String altPath,
                                           boolean rawCompressed) {
        super("Create Horta Sample");
        this.sample = sample;
        this.name = name;
        this.octreePath = octreePath;
        this.ktxPath = ktxPath;
        this.altPath = altPath;
        this.rawCompressed = rawCompressed;
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
                        if (altPath != null) {
                            DomainUtils.setFilepath(sample, FileType.LargeVolumeZarr, altPath);
                        }
                        newSample = TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                    }
                    else {
                        newSample = TiledMicroscopeDomainMgr.getDomainMgr().createSample(name, octreePath, ktxPath, altPath);
                    }
            }
            
            @Override
            protected void hadSuccess() {
                if (null != newSample) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Sample ").append(newSample.getName()).append(" saved successfully.");
                    if (!newSample.isExistsInStorage()) {
                        sb.append(" Some of the specified paths could not be accessed. Check that paths exist and try again.");
                    }
                    JOptionPane.showMessageDialog(mainFrame, sb.toString(),
                            "Add New Horta Sample", JOptionPane.PLAIN_MESSAGE, null);
                    DomainMgr.getDomainMgr().getModel().invalidateAll();
                }
                else {
                    JOptionPane.showMessageDialog(mainFrame, "Error saving sample " + name +
                                    ". Check that paths exist. If you continue to experience problems please contact support.",
                            "Failed to Add Horta Sample", JOptionPane.ERROR_MESSAGE, null);
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException("Error saving sample; name = " + name + "; octree = " + octreePath +
                        "; ktx = "+ ktxPath + ".", error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Saving sample...", ""));
        worker.execute();
    }
}
