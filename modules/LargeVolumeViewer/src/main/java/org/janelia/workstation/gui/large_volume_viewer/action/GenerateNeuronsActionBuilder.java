package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.dialogs.GenerateNeuronsDialog;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.workstation.integration.spi.actions.AdminActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AdminActionBuilder.class, position=1000)
public final class GenerateNeuronsActionBuilder implements AdminActionBuilder {

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction() {
        return new AbstractAction("Generate Neurons...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                if (annotationMgr != null) {
                    GenerateNeuronsDialog dialog = new GenerateNeuronsDialog();
                    dialog.showDialog();
                } else {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            "LVV needs to be opened before this function can be used.",
                            "LVV must be open",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
    }
}
