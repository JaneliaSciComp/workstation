package org.janelia.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Right-click context menu that allows user to edit a TmSample file path.
 */
@ActionID(
        category = "Actions",
        id = "EditSamplePathAction"
)
@ActionRegistration(
        displayName = "#CTL_EditSamplePathAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Large Volume", position = 1520)
})
@NbBundle.Messages("CTL_EditSamplePathAction=Edit Sample Path")
public class EditSamplePathAction extends BaseContextualNodeAction {

    private TmSample sample;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmSample.class)) {
            sample = getNodeContext().getSingleObjectOfType(TmSample.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(sample));
        }
        else {
            sample = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        final String editedPath = (String) JOptionPane.showInputDialog(
                FrameworkAccess.getMainFrame(),
                "New Linux path to sample:",
                "Edit sample path",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list
                sample.getFilepath()
        );
        if (editedPath == null || editedPath.length() == 0) {
            // canceled
            return;
        }
        else {
            SimpleWorker saver = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    sample.setFilepath(editedPath);
                    TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                }
                @Override
                protected void hadSuccess() {
                    // Handled by event system
                }
                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            saver.execute();
        }
    }
}
