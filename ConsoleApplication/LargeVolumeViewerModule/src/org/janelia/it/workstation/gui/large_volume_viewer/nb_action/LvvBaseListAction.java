package org.janelia.it.workstation.gui.large_volume_viewer.nb_action;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.components.PathCollectionEditor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

//Temporarily out of action.  The target values are unused, and changes
// are needed for the PathCollectionEditor.  see that class for todo list.

@ActionID(
		category = "File/New",
		id = "org.janelia.it.workstation.gui.large_volume_viewer.nb_action.LvvBaseListAction"
)
@ActionRegistration(
		displayName = "#CTL_LvvBaseListAction"
)
@ActionReference(path = "Menu/Edit", position = 5100, separatorBefore = 5050)
@Messages("CTL_LvvBaseListAction=Large Volume Viewer Base Directory List Editor")
public final class LvvBaseListAction implements ActionListener {

    public static final String PATHS_ATTRIBUTE = "sample paths";
    private static final String ENTITY_FOLDER_NAME = "3D Tile Microscope Samples";
    private static final String REQURED_OWNER = "group:mouselight";
    public static final String DIALOG_TITLE = "Edit Large Volume Base Paths";

    @Override
	public void actionPerformed(ActionEvent ae) {
        try {
            List<Entity> parentEntities = ModelMgr.getModelMgr().getEntitiesByName(ENTITY_FOLDER_NAME);
            if (parentEntities == null) {
                JOptionPane.showInputDialog("Failed to find any " + ENTITY_FOLDER_NAME);
                return;
            }
            
            for (Entity e: parentEntities) {
                if (e.getOwnerKey().equals(REQURED_OWNER)) {
                    JDialog lvvBaseDialog = new JDialog();
                    final PathCollectionEditor pathCollectionEditor = 
                            new PathCollectionEditor(e, PATHS_ATTRIBUTE, new PathCollectionCompletionListener(lvvBaseDialog));
                    lvvBaseDialog.setModal(true);
                    lvvBaseDialog.setLayout(new BorderLayout());
                    lvvBaseDialog.add(pathCollectionEditor, BorderLayout.CENTER);
                    lvvBaseDialog.setSize(pathCollectionEditor.getSize());
                    lvvBaseDialog.setVisible(true);
                    lvvBaseDialog.setTitle(DIALOG_TITLE);
                    break;
                }
            }
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
            ex.printStackTrace();
        }
	}
    
    /** Kill dialog after completion. */
    private static class PathCollectionCompletionListener implements PathCollectionEditor.CompletionListener {
        private JDialog dialog;
        public PathCollectionCompletionListener(JDialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void done() {
            dialog.setVisible(false);
        }
        
    }
    
}
