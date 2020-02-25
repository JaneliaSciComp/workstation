package org.janelia.workstation.gui.large_volume_viewer.dialogs;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.workstation.gui.large_volume_viewer.TileFormat;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LVVDebugTestDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(GenerateNeuronsDialog.class);

    private AnnotationManager annMgr;
    private AnnotationModel annModel;

    // UI things
    private Frame parent;

    // for info area
    JTextField sampleIDField;
    JTextField sampleNameField;
    JTextField workspaceIDField;
    JTextField workspaceNameField;

    public LVVDebugTestDialog(Frame parent) {
        super(parent, "LVV/Horta testing and debug dialog");

        this.parent = parent;

        annMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annModel = annMgr.getAnnotationModel();

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
        testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.PAGE_AXIS));
        testPanel.add(new JLabel("TESTING"));

        // add points
        testPanel.add(new JLabel("Create neuron and add points:"));
        JPanel addPointsPanel = new JPanel();
        addPointsPanel.setLayout(new BoxLayout(addPointsPanel, BoxLayout.LINE_AXIS));
        addPointsPanel.add(new JLabel("npoints:"));
        JTextField nptsField = new JTextField(4);
        addPointsPanel.add(nptsField);
        addPointsPanel.add(new JLabel("dt (ms):"));
        JTextField dtField = new JTextField(5);
        addPointsPanel.add(dtField);
        JButton addPointsButton = new JButton("Go");
        addPointsButton.addActionListener(e->{
            addNeuronPoints(Integer.parseInt(nptsField.getText()),
                Integer.parseInt(dtField.getText()));
        });
        addPointsPanel.add(addPointsButton);


        testPanel.add(addPointsPanel);


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

    /**
     * create a neuron and add points to it at some time interval
     *
     * npoints = number of points
     * dt = time in milliseconds between each point addition
     *
     * returns neuron ID or -1 if fail
     */
    private Long addNeuronPoints(int nPoints, int dt) {
        // yes, I'm doing this in one massive exception handler; it's for testing...
        try {
            log.info("addNeuronPoints():");

            TmNeuronMetadata neuron = annModel.createNeuron("test neuron");
            // minimal testing...
            if (neuron == null) {
                log.info("neuron creation failed");
                return -1L;
            }
            log.info("neuron created");

            // need to find the bounds of the image and pick a spot for the root
            // origin is in nm and is the corner of the volume; add some typical
            //  offsets to move it to the middle-ish, with a substantial random spread
            // NOTE: we're working in micron coordinates right now
            TmSample sample = annModel.getCurrentSample();

            Random r = new Random();
            double x0 = sample.getOrigin().get(0) / 1000.0 + 5000.0 + 3000.0 * (r.nextDouble() * 2 - 1);
            double y0 = sample.getOrigin().get(1) / 1000.0 + 4000.0 + 3000.0 * (r.nextDouble() * 2 - 1);
            double z0 = sample.getOrigin().get(2) / 1000.0 + 6000.0 + 1000.0 * (r.nextDouble() * 2 - 1);

            log.info("neuron root: " + x0 + ", " + y0 + ", " + z0);

            Vec3 micronCoords = new Vec3(x0, y0, z0);
            TmGeoAnnotation root = annModel.addRootAnnotation(neuron, toVoxelCoords(micronCoords));
            if (root == null) {
                log.info("root creation failed");
                return -1L;
            }

            // loop and place points; make a nice spiral:
            double radius = 500.0;
            double dTheta = Math.PI / 4.0;
            double dz = 50.0;
            TmGeoAnnotation parent = root;
            for (int n = 0; n < nPoints; n++) {
                Vec3 micronLocation = new Vec3(
                        x0 + radius * Math.sin(n * dTheta),
                        y0 + radius * Math.cos(n * dTheta),
                        z0 + dz);
                TmGeoAnnotation next = annModel.addChildAnnotation(parent, toVoxelCoords(micronLocation));
                if (next == null) {
                    log.info("child annotation failed at n = " + n);
                }
                parent = next;
                TimeUnit.MILLISECONDS.sleep(dt);
            }
            return neuron.getId();
        } catch (InterruptedException e) {
            presentError("Sleep interrupted", "Error");
            return -1L;
        } catch (Exception e) {
            log.error("Exception while adding neuron points", e);
            presentError("Exception while adding neuron points", "Error");
            return -1L;
        }
    }

    private Vec3 toVoxelCoords(Vec3 micronCoords) {
        TileFormat.VoxelXyz tempLocation = annMgr.getTileFormat().voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(micronCoords.getX(),
                        micronCoords.getY(), micronCoords.getZ()));
        return new Vec3(tempLocation.getX(),
                tempLocation.getY(), tempLocation.getZ());
    }

    private void presentError(String message, String title) throws HeadlessException {
        JOptionPane.showMessageDialog(
            ComponentUtil.getLVVMainWindow(),
            message,
            title,
            JOptionPane.ERROR_MESSAGE);
    }

    private ActionListener escapeListener = new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
            doCancel();
        }
    };


}

