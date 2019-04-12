package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.GenerateNeuronsDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.enums.SubjectRole;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Tools",
        id = "GenerateNeuronsAction"
)
@ActionRegistration(
        displayName = "#CTL_GenerateNeuronsAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 6000),
    @ActionReference(path = "Shortcuts", name = "A-g")
})
@Messages("CTL_GenerateNeuronsAction=Generate Neurons...")
public final class GenerateNeuronsAction extends AbstractAction implements Presenter.Menu {
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isAccessible()) {
            AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
            if (annotationMgr != null) {
                GenerateNeuronsDialog dialog = new GenerateNeuronsDialog();
                dialog.showDialog();
            } else {
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "LVV needs to be opened before this function can be used.",
                    "LVV must be open",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        if (isAccessible()) {
            JMenuItem menuItem = new JMenuItem("Generate Neurons...");
            menuItem.addActionListener(this);
            return menuItem;
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return isAccessible();
    }

    public static boolean isAccessible() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }
}
