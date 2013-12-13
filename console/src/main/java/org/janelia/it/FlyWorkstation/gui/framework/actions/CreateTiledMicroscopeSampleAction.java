package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;
import org.janelia.it.jacs.shared.utils.StringUtils;

import javax.swing.*;

/**
 * Create any "Tiled Microscope Sample" items (buttons, menu items, etc.) based upon this.
 * 
 */
public class CreateTiledMicroscopeSampleAction implements Action {

    private String name;
    private RootedEntity sampleRootedEntity;

    public CreateTiledMicroscopeSampleAction(String name) {
        this.name = name;
    }

    public CreateTiledMicroscopeSampleAction(String name, RootedEntity sampleRootedEntity) {
        this.name = name;
        this.sampleRootedEntity = sampleRootedEntity;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public void doAction() {

        final Browser browser = SessionMgr.getBrowser();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            }
            
            @Override
            protected void hadSuccess() {

//                if (sample!=null && contexts.isEmpty()) {
//                    JOptionPane.showMessageDialog(browser,
//                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }



                // Pick a name for the new board
                final String boardName = (String) JOptionPane.showInputDialog(browser, "Board Name:\n",
                        "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if (StringUtils.isEmpty(boardName)) return;


                SimpleWorker worker = new SimpleWorker() {
                    
                    private TmSample newSample;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        newSample = ModelMgr.getModelMgr().createTiledMicroscopeSample(null, name);
                        if (newSample!=null) {
                            // Add the new sample to the Alignment Boards area.
//                            alignmentBoardContext.addNewAlignedEntity(sample);
                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
                        final EntityOutline entityOutline = browser.getEntityOutline();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                entityOutline.selectEntityByUniqueId(newSample.getId().toString());
                                SessionMgr.getBrowser().setPerspective(Perspective.SliceViewer);
                                SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(newSample.getId());
                            }
                        });
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(browser, "Preparing Slice Viewer...", ""));
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
