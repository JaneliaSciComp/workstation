package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;

/**
 * let the user edit the path attached to an LVV sample
 */
public class EditLVVSamplePathActionListener implements ActionListener {
    private RootedEntity rootedEntity;

    public EditLVVSamplePathActionListener(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final String oldPath = rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) // input value
                ;
        final String editedPath = (String) JOptionPane.showInputDialog(
                SessionMgr.getMainFrame(),
                "New Linux path to sample:",
                "Edit sample path",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, oldPath);
        if (editedPath == null || editedPath.length() == 0) {
            // canceled
            return;
        } else {
            final Entity sampleEntity = rootedEntity.getEntity();
            SimpleWorker saver = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    SessionMgr.getSessionMgr().logToolEvent(
                            new ToolString("Lvv"),
                            new CategoryString("ChangeSamplePath"),
                            new ActionString(oldPath + " to " + editedPath.toString()), 
                            true);
                    sampleEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, editedPath);
                    ModelMgr.getModelMgr().saveOrUpdateEntity(sampleEntity);
                }

                @Override
                protected void hadSuccess() {
                    // blah
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            saver.execute();
        }
    }
}
