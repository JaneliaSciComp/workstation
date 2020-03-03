package org.janelia.workstation.gui.large_volume_viewer.dialogs;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.security.Subject;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
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
import java.util.ArrayList;
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

    // results
    JTextArea resultsText;

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
        JPanel addPointsPanel = new JPanel();
        addPointsPanel.setLayout(new BoxLayout(addPointsPanel, BoxLayout.LINE_AXIS));
        addPointsPanel.add(new JLabel("Create neuron and add points: "));
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


        // change owner
        JPanel changeOwnerPanel = new JPanel();
        changeOwnerPanel.setLayout(new BoxLayout(changeOwnerPanel, BoxLayout.LINE_AXIS));
        changeOwnerPanel.add(new JLabel("Create neuron and repeatedly change owner: "));
        changeOwnerPanel.add(new JLabel("ntimes:"));
        JTextField ntimesField = new JTextField(4);
        changeOwnerPanel.add(ntimesField);
        changeOwnerPanel.add(new JLabel("dt (ms):"));
        JTextField dtField2 = new JTextField(5);
        changeOwnerPanel.add(dtField2);
        JButton changeOwnerButton = new JButton("Go");
        changeOwnerButton.addActionListener(e->{
            changeNeuronOwner(Integer.parseInt(ntimesField.getText()),
                    Integer.parseInt(dtField2.getText()));
        });
        changeOwnerPanel.add(changeOwnerButton);
        testPanel.add(changeOwnerPanel);


        // merge fragments
        JPanel mergePanel = new JPanel();
        mergePanel.setLayout(new BoxLayout(mergePanel, BoxLayout.LINE_AXIS));
        mergePanel.add(new JLabel("Create fragments and merge them: "));
        mergePanel.add(new JLabel("nfrags:"));
        JTextField nfragsField = new JTextField(4);
        mergePanel.add(nfragsField);
        mergePanel.add(new JLabel("dt (ms):"));
        JTextField dtField3 = new JTextField(5);
        mergePanel.add(dtField3);
        JButton mergeButton = new JButton("Go");
        mergeButton.addActionListener(e->{
            createMergeFragments(Integer.parseInt(nfragsField.getText()),
                    Integer.parseInt(dtField3.getText()));
        });
        mergePanel.add(mergeButton);
        testPanel.add(mergePanel);




        add(testPanel);




        // results
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
        resultsPanel.add(new JLabel("RESULTS"));

        resultsText = new JTextArea(20, 40);
        JScrollPane resultsScrollPane = new JScrollPane(resultsText);
        resultsPanel.add(resultsScrollPane);



        add(resultsPanel);






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
            addResults("sample ID " + sampleIDstring + " has name " + obj.getName());
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
            addResults("workspace ID " + workspaceIDstring + " has name " + obj.getName());
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
    private void addNeuronPoints(int nPoints, int dt) {
        // yes, I'm doing this in one massive exception handler; it's for testing...
        try {
            addResults("addNeuronPoints():");

            Long neuronID = createNeuronRoot("test add points");

            TmNeuronMetadata neuron = annModel.getNeuronFromNeuronID(neuronID);
            TmGeoAnnotation root = neuron.getFirstRoot();
            // these are voxel coordinates
            double x0 = root.getX();
            double y0 = root.getY();
            double z0 = root.getZ();

            // loop and place points; make a nice spiral:
            double radius = 500.0;
            double dTheta = Math.PI / 4.0;
            double dz = 50.0;
            TmGeoAnnotation parent = root;
            for (int n = 0; n < nPoints; n++) {
                Vec3 location = new Vec3(
                        x0 + radius * Math.sin(n * dTheta),
                        y0 + radius * Math.cos(n * dTheta),
                        z0 + dz);
                TmGeoAnnotation next = annModel.addChildAnnotation(parent, location);
                if (next == null) {
                    addResults("child annotation failed at n = " + n);
                }
                parent = next;
                TimeUnit.MILLISECONDS.sleep(dt);
            }
            addResults(nPoints + " points added");
        } catch (InterruptedException e) {
            presentError("Sleep interrupted", "Error");
        } catch (Exception e) {
            log.error("Exception while adding neuron points", e);
            presentError("Exception while adding neuron points", "Error");
        }
    }

    /**
     * create a neuron, add points; then change owner repeatedly
     *
     * ntimes = number of times to change from owner to group and back
     *      (eg 2 * ntimes total changes)
     * dt = time in milliseconds between each ownership change
     *
     * returns neuron ID or -1 if fail
     */
    private void changeNeuronOwner(int ntimes, int dt) {
        try {
            addResults("changeNeuronOwner():");

            Long neuronID = createDimer("test change owner", getRandomSampleLocation(), 0.0, 500.0);

            // the owners we're going to swap between:
            Subject owner = AccessManager.getAccessManager().getActualSubject();
            String tracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
            Subject tracersSubject = DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(tracersGroup);

            for (int i=0; i<ntimes; i++) {
                // we're just flip-flopping between the two
                annModel.changeNeuronOwner(neuronID, tracersSubject);
                TimeUnit.MILLISECONDS.sleep(dt);

                annModel.changeNeuronOwner(neuronID, owner);
                TimeUnit.MILLISECONDS.sleep(dt);
            }
            // change one last time to demonstrate that it has been changed
            //  (at least this once)
            annModel.changeNeuronOwner(neuronID, tracersSubject);
            addResults("changed owner " + (ntimes + 1) + " times");
        } catch (InterruptedException e) {
            presentError("Sleep interrupted", "Error");
        } catch (Exception e) {
            log.error("Exception while changing owner", e);
            presentError("Exception while changing owner", "Error");
        }
    }

    /**
     *  create nfrags fragments, then merge them together, at
     *  intervals of dt milliseconds
     */
    private void createMergeFragments(int nfrags, int dt) {
        try {
            addResults("createMergeFragments():");

            // these are all micron coordinates
            // length of frag:
            double fragx = 0.0;
            double fragy = 50.0;

            // distance between roots of two frags:
            Vec3 dist = new Vec3(0.0, 75.0, 0.0);

            Vec3 nextRootLocation = getRandomSampleLocation();
            String name = "test merge neuron ";

            ArrayList<TmNeuronMetadata> frags = new ArrayList<>();
            for (int i=0; i<nfrags; i++) {
                Long neuronID = createDimer(name + i, nextRootLocation, fragx, fragy);
                TmNeuronMetadata neuron = annModel.getNeuronFromNeuronID(neuronID);
                frags.add(neuron);
                nextRootLocation.plusEquals(dist);
                // I saw the select loop bug here once but it didn't recur
            }

            // merge fragments successively into the first frag at the endpoint
            TmNeuronMetadata targetNeuron = frags.get(0);
            Long targetNeuronID = targetNeuron.getId();
            Long targetAnnID = targetNeuron.getFirstRoot().getChildIds().get(0);

            for (int i=1; i<nfrags; i++) {
                TmNeuronMetadata sourceNeuron = frags.get(i);
                Long sourceNeuronID = sourceNeuron.getId();
                Long sourceAnnID = sourceNeuron.getFirstRoot().getId();
                Long nextTargetAnnID = sourceNeuron.getFirstRoot().getChildIds().get(0);
                annModel.mergeNeurite(sourceNeuronID, sourceAnnID, targetNeuronID, targetAnnID);

                TimeUnit.MILLISECONDS.sleep(dt);
                // target neuron remains the same, but the target annotation moves to the
                //  child annotation of the root of the neuron we just merged
                targetAnnID = nextTargetAnnID;
            }
        } catch (InterruptedException e) {
            presentError("Sleep interrupted", "Error");
        } catch (Exception e) {
            log.error("Exception while merging fragments", e);
            presentError("Exception while merging fragments", "Error");
        }
    }

    /**
     * create a neuron with the input name; returns neuron ID
     */
    private Long createNeuronRoot(String name) throws Exception {
        return createNeuronRoot(name, getRandomSampleLocation());
    }

    /**
     * create neuron with input name and root location (µm);
     * returns neuron ID
     */
    private Long createNeuronRoot(String name, Vec3 rootLocation) throws Exception {
        TmNeuronMetadata neuron = annModel.createNeuron(name);
        // minimal testing...
        if (neuron == null) {
            addResults("neuron creation failed");
            return -1L;
        }

        addResults("neuron root (µm): " + rootLocation);
        TmGeoAnnotation root = annModel.addRootAnnotation(neuron, toVoxelCoords(rootLocation));
        if (root == null) {
            addResults("root creation failed");
            return -1L;
        }

        return neuron.getId();
    }

    /**
     * create a two-point neuron with given name; root is at location, and
     * second point is at location plus (dx, dy) (thus has the same z)
     * (all in µm); returns neuron ID
     */
    private Long createDimer(String name, Vec3 location, double dx, double dy) throws Exception {
        Long neuronID = createNeuronRoot(name, location);
        TmNeuronMetadata neuron = annModel.getNeuronFromNeuronID(neuronID);
        TmGeoAnnotation root = neuron.getFirstRoot();

        Vec3 location2 = new Vec3(
                location.getX() + dx,
                location.getY() + dy,
                location.getZ());
        TmGeoAnnotation next = annModel.addChildAnnotation(root, toVoxelCoords(location2));
        if (next == null) {
            addResults("child annotation failed while creating dimer");
        }
        return neuronID;
    }

    /**
     * return a random point within the sample, near the middle-ish;
     * result is in micron coordinates
     */
    private Vec3 getRandomSampleLocation() {
        // need to find the bounds of the image and pick a spot for the root
        // origin is in nm and is the corner of the volume; add some typical
        //  offsets to move it to the middle-ish, with a substantial random spread
        // NOTE: we're working in micron coordinates right now
        TmSample sample = annModel.getCurrentSample();

        Random r = new Random();
        double x0 = sample.getOrigin().get(0) / 1000.0 + 5000.0 + 3000.0 * (r.nextDouble() * 2 - 1);
        double y0 = sample.getOrigin().get(1) / 1000.0 + 4000.0 + 3000.0 * (r.nextDouble() * 2 - 1);
        double z0 = sample.getOrigin().get(2) / 1000.0 + 6000.0 + 1000.0 * (r.nextDouble() * 2 - 1);

        return new Vec3(x0, y0, z0);
    }

    private void addResults(String result) {
        // mostly this method is because I keep forgetting to add the carriage return!
        resultsText.append(result + "\n");
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

