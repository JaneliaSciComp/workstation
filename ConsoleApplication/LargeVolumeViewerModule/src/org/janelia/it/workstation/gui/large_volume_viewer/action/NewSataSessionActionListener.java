package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSession;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NewSataSessionActionListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NewSataSessionActionListener.class);
    
    private TmSample sample;

    public NewSataSessionActionListener(TmSample sample) {
        this.sample = sample;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewSataSessionActionListener.actionPerformed");
        
        if (sample==null) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "No sample selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        final AnnotationModel annotationModel = annotationMgr.getAnnotationModel();
        
        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog("Session Name");
        final String sessionName = dialog.showForSample(annotationModel.getCurrentSample());
        
        if (sessionName==null) {
            log.info("Aborting session creation: no valid name was provided by the user");
            return;
        }

        // create it in another thread
        // there is no doubt a better way to get these parameters in:
        final Long finalSampleId = sample.getId();
        SimpleWorker creator = new SimpleWorker() {
            
            private TmSession session;
            
            @Override
            protected void doStuff() throws Exception {
                
                log.info("Creating new workspace with name '{}' for {}",sessionName,finalSampleId);
                
                // now we can create the workspace
                this.session = annotationModel.createSession(finalSampleId, sessionName);
                log.info("Created workspace with id={}",session.getId());

            }

            @Override
            protected void hadSuccess() {
                LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(session);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        creator.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Creating new session...", ""));
        creator.execute();
    }
}
