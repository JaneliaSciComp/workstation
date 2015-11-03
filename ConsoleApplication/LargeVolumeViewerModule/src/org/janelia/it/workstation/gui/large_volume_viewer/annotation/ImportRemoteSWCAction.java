package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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

    public ImportRemoteSWCAction(AnnotationPanel annotationPanel, AnnotationModel annotationModel) {
        this.annotationPanel = annotationPanel;
        this.annotationModel = annotationModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // Simple dialog: just enter the path.  Should be a server-known path.
        final String userInput = JOptionPane.showInputDialog(annotationPanel, "Enter Full Path to Input Folder", "Input Folder", JOptionPane.PLAIN_MESSAGE);
        if (userInput != null) {
            BackgroundWorker importer = new BackgroundWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.importRemoteSWCFolder(userInput);
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
            importer.executeWithEvents();
            
        }        
    }

}
