package org.janelia.it.workstation.gui.framework.actions;

import java.awt.Component;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;

/**
 * Create any "Tiled Microscope Sample" items (buttons, menu items, etc.) based upon this.
 * 
 */
public class CreateTiledMicroscopeSampleAction implements Action {

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
                        newSample = ModelMgr.getModelMgr().createTiledMicroscopeSample(SessionMgr.getUsername(), name, pathToRenderFolder);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
//                        final EntityOutline entityOutline = browser.getEntityOutline();
//                        SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                entityOutline.selectEntityByUniqueId(newSample.getId().toString());
//                                SessionMgr.getBrowser().setPerspective(Perspective.SliceViewer);
//                                SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(newSample.getId());
//                            }
//                        });
                        if (null!=newSample) {
                            JOptionPane.showMessageDialog(mainFrame, "Sample " + newSample.getName() + " added successfully.",
                                    "Add New Tiled Microscope Sample", JOptionPane.PLAIN_MESSAGE, null);
                            SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline().refresh();
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
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Preparing Slice Viewer...", ""));
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
