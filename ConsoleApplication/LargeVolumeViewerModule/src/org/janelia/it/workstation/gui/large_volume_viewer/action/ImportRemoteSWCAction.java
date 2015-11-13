package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationPanel;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;

/**
 * Drag the SWCs into the workspace, and make neurons.  Carry out all
 * operations on the server.
 *
 * @author fosterl
 */
public class ImportRemoteSWCAction extends AbstractAction {

    private AnnotationPanel annotationPanel;
    private AnnotationModel annotationModel;
    private AnnotationManager annotationManager;

    public ImportRemoteSWCAction(AnnotationPanel annotationPanel, AnnotationModel annotationModel, AnnotationManager annotationManager) {
        this.annotationPanel = annotationPanel;
        this.annotationModel = annotationModel;
        this.annotationManager = annotationManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // Simple dialog: just enter the path.  Should be a server-known path.
        final String userInput = JOptionPane.showInputDialog(annotationPanel, "Enter Full Path to Input Folder", "Input Folder", JOptionPane.PLAIN_MESSAGE);
        if (userInput != null) {
            BackgroundWorker importer = new BackgroundWorker() {
                @Override
                protected void doStuff() throws Exception {
                    Long lvvEntityId = annotationManager.getInitialEntity().getId();
                    annotationModel.importRemoteSWCFolder(userInput, lvvEntityId);
                }

                @Override
                public String getName() {
                    return "import " + userInput;
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            importer.execute();
            
        }        
    }

}
