package org.janelia.workstation.gui.large_volume_viewer.dialogs;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class LVVDebugTestDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(GenerateNeuronsDialog.class);

    private final AnnotationManager annMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
    private AnnotationModel annModel;

    // UI things
    private Frame parent;

    JTextField sampleIDField;
    JTextField sampleNameField;
    JTextField workspaceIDField;
    JTextField workspaceNameField;


    public LVVDebugTestDialog(Frame parent) {
        super(parent, "LVV/Horta testing and debug dialog");


        this.parent = parent;




        setupUI();


    }

    private void setupUI() {

        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

        // info area; it's useful to be able to map IDs to workspaces and samples on demand:
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
        infoPanel.add(new JLabel("INFO"));

        // get sample name for sample id:
        JPanel sampleIDPanel = new JPanel();
        sampleIDPanel.setLayout(new BoxLayout(sampleIDPanel, BoxLayout.LINE_AXIS));
        sampleIDPanel.add(new JLabel("Sample ID:"));
        sampleIDField = new JTextField(20);
        sampleIDPanel.add(sampleIDField);
        JButton getSampleNameButton = new JButton("Get sample name");
        getSampleNameButton.addActionListener(event->doGetSampleName());
        sampleIDPanel.add(getSampleNameButton);
        sampleNameField = new JTextField(20);
        sampleIDPanel.add(sampleNameField);
        infoPanel.add(sampleIDPanel);

        // get workspace name for workspace id:
        JPanel workspaceIDPanel = new JPanel();
        workspaceIDPanel.setLayout(new BoxLayout(workspaceIDPanel, BoxLayout.LINE_AXIS));
        workspaceIDPanel.add(new JLabel("Workspace ID:"));
        workspaceIDField = new JTextField(20);
        workspaceIDPanel.add(workspaceIDField);
        JButton getWorkspaceNameButton = new JButton("Get workspace name");
        getWorkspaceNameButton.addActionListener(event->doGetWorkspaceName());
        workspaceIDPanel.add(getWorkspaceNameButton);
        workspaceNameField = new JTextField(20);
        workspaceIDPanel.add(workspaceNameField);
        infoPanel.add(workspaceIDPanel);





        add(infoPanel);




        // testing area
        add(new JSeparator());
        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.LINE_AXIS));
        testPanel.add(new JLabel("TESTING"));


        add(testPanel);




        // the bottom
        add(new JSeparator());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event->doCancel());
        add(closeButton);

        pack();
        setLocationRelativeTo(parent);

        // hook up actions
        getRootPane().registerKeyboardAction(escapeListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void doGetSampleName() {
        String sampleIDstring = sampleIDField.getText();
        Long sampleID = Long.valueOf(Long.parseLong(sampleIDstring));

        DomainObject obj = null;
        try {
            obj = DomainMgr.getDomainMgr().getModel().getDomainObject(TmSample.class.getSimpleName(), sampleID);
        } catch (Exception e) {
            sampleNameField.setText("exception occurred");
            return;
        }
        if (obj != null) {
            sampleNameField.setText(obj.getName());
        } else {
            sampleNameField.setText("domain object is null");
        }
    }

    private void doGetWorkspaceName() {
        String workspaceIDstring = workspaceIDField.getText();
        Long workspaceID = Long.valueOf(Long.parseLong(workspaceIDstring));

        DomainObject obj = null;
        try {
            obj = DomainMgr.getDomainMgr().getModel().getDomainObject(TmWorkspace.class.getSimpleName(), workspaceID);
        } catch (Exception e) {
            workspaceNameField.setText("exception occurred");
            return;
        }
        if (obj != null) {
            workspaceNameField.setText(obj.getName());
        } else {
            workspaceNameField.setText("domain object is null");
        }
    }

    private void doCancel() {
        dispose();
    }

    private ActionListener escapeListener = new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
            doCancel();
        }
    };


}

