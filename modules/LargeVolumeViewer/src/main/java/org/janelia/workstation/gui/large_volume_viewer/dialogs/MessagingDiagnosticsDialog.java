package org.janelia.workstation.gui.large_volume_viewer.dialogs;


import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingDiagnosticsDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(MessagingDiagnosticsDialog.class);

    private final JButton closeButton;
    private final JPanel infoPanel;

    private final AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
    private AnnotationModel annotationModel;
    private MessagingDiagnosticsDialog dialog;

    public MessagingDiagnosticsDialog(TmNeuronMetadata ownershipProblem) {
        super(FrameworkAccess.getMainFrame());
        dialog = this;
        setTitle("Messaging Diagnostics");
        // set to modeless
        setModalityType(Dialog.ModalityType.MODELESS);


        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove dialog being updated by refreshhandler
                setVisible(false);
            }
        });

        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        infoPanel.add(Box.createHorizontalGlue());
        JLabel instructions1 = new JLabel("Your client is unfortunately experiencing some network or processing issues communicating with the messaging server." +
                " Running diagnostics to determine how severe the problem is and assess why you are experiencing network delays.");

        infoPanel.add(instructions1);
        infoPanel.add(closeButton);

    }

    public void showDialog() {
        packAndShow();
    }
}

