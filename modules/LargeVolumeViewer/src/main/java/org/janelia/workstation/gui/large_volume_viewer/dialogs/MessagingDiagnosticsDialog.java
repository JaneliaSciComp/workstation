package org.janelia.workstation.gui.large_volume_viewer.dialogs;


import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.table.AbstractTableModel;

import org.janelia.console.viewerapi.commands.UpdateNeuronAnchorRadiusCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingDiagnosticsDialog extends JOptionPane
{
    JPanel infoPanel;

    public MessagingDiagnosticsDialog(TmNeuronMetadata targetNeuron)
    {

        setOptionType(JOptionPane.OK_CANCEL_OPTION);

        JDialog dialog = createDialog("Messaging Diagnostics");
        dialog.setVisible(true);

        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        infoPanel.add(Box.createHorizontalGlue());
        JLabel instructions1 = new JLabel("Your client is unfortunately experiencing some network or processing issues communicating with the messaging server." +
                " Running diagnostics to determine how severe the problem is and assess why you are experiencing network delays.");

        infoPanel.add(instructions1);
        add (infoPanel);

    }
}

