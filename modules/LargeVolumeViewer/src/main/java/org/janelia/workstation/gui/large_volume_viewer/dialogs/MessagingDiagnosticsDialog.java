package org.janelia.workstation.gui.large_volume_viewer.dialogs;


import java.awt.*;
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

public class MessagingDiagnosticsDialog
{
    JPanel infoPanel;
    TmNeuronMetadata neuron;
    AnnotationModel annModel;
    JTextArea results;

    public MessagingDiagnosticsDialog(AnnotationModel annModel, TmNeuronMetadata targetNeuron) {
    }

    public JPanel getDiagnosticsPanel() {
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        infoPanel.add(Box.createHorizontalGlue());
        JLabel instructions1 = new JLabel("Your client is unfortunately experiencing some network or processing issues communicating with the messaging server." +
                " Running diagnostics to determine how severe the problem is and assess why you are experiencing network delays.");
        instructions1.setMaximumSize(new Dimension (600,300));
        infoPanel.add(instructions1);

        JLabel test1Text = new JLabel("Starting Roundtrip Ownership Tests...");
        infoPanel.add(test1Text);

        JTextArea results = new JTextArea(16, 58);
        results.setEditable(false); // set textArea non-editable
        JScrollPane scroll = new JScrollPane(results);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        infoPanel.add(scroll);
        return infoPanel;

    }

    public void kickOffTest () {
        try {
            StringBuffer testResults = new StringBuffer();
            Map<String, Map<String,Object>> resultsData = annModel.testMessagingRoundtrips(neuron);
            for (String key: resultsData.keySet()) {
                Map<String,Object> testRunData = resultsData.get(key);
                testRunData.keySet().stream().forEach( dataKey -> {
                    if (testRunData.get(dataKey) instanceof String) {
                         testResults.append(dataKey + ": " + testRunData.get(dataKey) + "\n");
                    }
                });
                testResults.append("\n");
            }
        } catch (Exception e) {
             e.printStackTrace();
             results.setText(e.getMessage());
             infoPanel.invalidate();
        }
    }
}

