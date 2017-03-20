package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.api.ModelTranslation;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NewWorkspaceActionListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NewWorkspaceActionListener.class);
    
    private TmSample sample;

    public NewWorkspaceActionListener(TmSample sample) {
        this.sample = sample;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewWorkspaceActionListener.actionPerformed");
        
        if (sample==null) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "No sample selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        final AnnotationModel annotationModel = annotationMgr.getAnnotationModel();
        final boolean existingWorkspace = annotationModel.getCurrentWorkspace() != null;
        
        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog("Workspace Name");
        final String workspaceName = dialog.showForSample(annotationModel.getCurrentSample());
        
        if (workspaceName==null) {
            log.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        final Long finalSampleId = sample.getId();
        SimpleWorker creator = new SimpleWorker() {
            
            private TmWorkspace workspace;
            
            @Override
            protected void doStuff() throws Exception {
                
                log.info("Creating new workspace with name '{}' for {}",workspaceName,finalSampleId);
                
                // now we can create the workspace
                this.workspace = annotationModel.createWorkspace(finalSampleId, workspaceName);
                log.info("Created workspace with id={}",workspace.getId());

                // Reuse the existing color model 
                if (existingWorkspace) {
                    QuadViewUi quadViewUi = LargeVolumeViewerTopComponent.getInstance().getLvvv().getQuadViewUi();
                    workspace.setColorModel(ModelTranslation.translateColorModel(quadViewUi.getImageColorModel()));
                    annotationModel.saveWorkspace(workspace);
                    log.info("Copied existing color model");
                }
            }

            @Override
            protected void hadSuccess() {
                LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(workspace);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        creator.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Creating new workspace...", ""));
        creator.execute();
    }
}
