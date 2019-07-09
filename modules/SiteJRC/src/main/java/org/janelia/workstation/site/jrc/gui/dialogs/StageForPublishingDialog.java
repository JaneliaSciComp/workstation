package org.janelia.workstation.site.jrc.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.LineRelease;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.context.ApplyAnnotationAction;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.site.jrc.action.context.ApplyPublishingNamesActionListener;
import org.janelia.workstation.site.jrc.nodes.FlyLineReleasesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for staging samples for publishing.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StageForPublishingDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(StageForPublishingDialog.class);

    private static final String PUBLICATION_OWNER = "group:workstation_users";
    private static final String PUBLICATION_ONTOLOGY_NAME = "Publication";
    private static final String ANNOTATION_PUBLISH_OBJECTIVE = "Publish%sToWeb";

    private final JLabel loadingLabel = new JLabel(Icons.getLoadingIcon());
    private final JPanel mainPanel;
    private final GroupedKeyValuePanel attrPanel;
    private final JPanel addPanel;
    private final JRadioButton existingReleaseRadioButton;
    private final JLabel existingReleaseLabel;
    private final JPanel releaseComboPanel;
    private final JComboBox<ReleaseWrapper> releaseComboBox;
    private final JRadioButton newReleaseRadioButton;
    private final JLabel newReleaseLabel;
    private final JTextField releaseNameField;
    private final JPanel objectivesPanel;
    private final Map<String, JCheckBox> objectiveCheckboxMap = new HashMap<>();
    private Collection<Sample> samples;

    public StageForPublishingDialog() {
        setLayout(new BorderLayout());

        this.attrPanel = new GroupedKeyValuePanel();

        ButtonGroup group = new ButtonGroup();

        this.existingReleaseRadioButton = new JRadioButton("Add to existing release:");
        group.add(existingReleaseRadioButton);
        existingReleaseRadioButton.addActionListener((ActionEvent e) -> {
            updateState();
        });

        this.releaseComboBox = new JComboBox<>();
        releaseComboBox.setEditable(false);
        releaseComboBox.addActionListener((ActionEvent e) -> {
            updateState();
        });
        existingReleaseLabel = new JLabel("Existing release");

        this.releaseComboPanel = new JPanel(new BorderLayout());
        releaseComboPanel.add(existingReleaseLabel, BorderLayout.WEST);
        releaseComboPanel.add(loadingLabel, BorderLayout.CENTER);

        this.newReleaseRadioButton = new JRadioButton("Create a new release:");
        newReleaseRadioButton.setSelected(true);
        group.add(newReleaseRadioButton);
        newReleaseRadioButton.addActionListener((ActionEvent e) -> {
            updateState();
        });

        this.newReleaseLabel = new JLabel("Release name");
        this.releaseNameField = new JTextField(25);

        this.addPanel = new JPanel();
        addPanel.setLayout(new MigLayout("wrap 2, ins 10, fillx", "[grow 0]20[grow 1]"));
        addPanel.add(existingReleaseRadioButton, "");
        addPanel.add(newReleaseRadioButton, "");
        addPanel.add(releaseComboPanel, "gapleft 10");
        addPanel.add(releaseNameField, "gapleft 10");

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(attrPanel);
        mainPanel.add(addPanel);

        objectivesPanel = new JPanel();
        objectivesPanel.setLayout(new BoxLayout(objectivesPanel, BoxLayout.PAGE_AXIS));

        mainPanel.add(new JLabel("Stage images for these microscope objectives:"));
        mainPanel.add(objectivesPanel);

        add(mainPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton saveButton = new JButton("Stage");
        saveButton.setToolTipText("Stage sample(s) to the selected release");
        saveButton.addActionListener(e -> processSave());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(saveButton);
        add(buttonPane, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
    }

    private void updateState() {

        if (existingReleaseRadioButton.isSelected()) {
            existingReleaseLabel.setEnabled(true);
            releaseComboBox.setEnabled(true);
            newReleaseLabel.setEnabled(false);
            releaseNameField.setEnabled(false);
        }
        else if (newReleaseRadioButton.isSelected()) {
            existingReleaseLabel.setEnabled(false);
            releaseComboBox.setEnabled(false);
            newReleaseLabel.setEnabled(true);
            releaseNameField.setEnabled(true);
        }
        else {
            log.warn("None of the radio buttons is selected. This should never happen.");
        }
    }

    public void showForSamples(Collection<Sample> samples) {

        this.samples = samples;
        setTitle("Stage "+samples.size()+" Samples for Publishing");
        Component mainFrame = FrameworkAccess.getMainFrame();
        //setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.6)));

        ActivityLogHelper.logUserAction("StageForPublishingDialog.showForSamples");

        objectivesPanel.removeAll();
        objectiveCheckboxMap.clear();

        Set<String> objectiveSet = new TreeSet<>();
        for (Sample sample : samples) {
            objectiveSet.addAll(sample.getObjectives());
        }

        for (String objective : objectiveSet) {
            JCheckBox checkbox = new JCheckBox(objective, true);
            objectiveCheckboxMap.put(objective, checkbox);
            objectivesPanel.add(checkbox);
        }

        load();

        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        Events.getInstance().unregisterOnEventBus(this);
    }

    private void load() {

        SimpleWorker worker = new SimpleWorker() {

            private List<LineRelease> releases = new ArrayList<>();

            @Override
            protected void doStuff() throws Exception {
                for (LineRelease lineRelease : DomainMgr.getDomainMgr().getModel().getLineReleases()) {
                    if (ClientDomainUtils.hasWriteAccess(lineRelease)) {
                        releases.add(lineRelease);
                    }
                }
            }

            @Override
            protected void hadSuccess() {

                releaseComboPanel.removeAll();
                releaseComboPanel.add(releaseComboBox);

                if (releases.isEmpty()) {
                    newReleaseRadioButton.setSelected(true);
                    existingReleaseRadioButton.setEnabled(false);
                    releaseComboBox.setEnabled(false);
                }
                else {
                    existingReleaseRadioButton.setSelected(true);
                    DefaultComboBoxModel<ReleaseWrapper> model =
                            (DefaultComboBoxModel<ReleaseWrapper>) releaseComboBox.getModel();
                    ReleaseWrapper wrapper = null;
                    for(LineRelease release : releases) {
                        wrapper = new ReleaseWrapper(release);
                        model.addElement(wrapper);
                    }
                    if (wrapper!=null) {
                        model.setSelectedItem(wrapper); // select the last item by default
                    }
                }

                revalidate();
                pack();
            }

            @Override
            protected void hadError(Throwable error) {
                setVisible(false);
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();

    }

    private LineRelease getSelectedRelease() {
        ReleaseWrapper wrapper = (ReleaseWrapper)releaseComboBox.getSelectedItem();
        if (wrapper == null) return null;
        return wrapper.release;
    }

    private void processSave() {

        if (existingReleaseRadioButton.isSelected()) {
            if (getSelectedRelease()==null) {
                JOptionPane.showMessageDialog(this,
                        "You must select a release to add the mask to",
                        "No release selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Collection<String> objectives = new TreeSet<>();
        for (String objective : objectiveCheckboxMap.keySet()) {
            if (objectiveCheckboxMap.get(objective).isSelected()) {
                objectives.add(objective);
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            private LineRelease lineRelease;
            private int numAnnotations;
            private int numPublishingNames;
            private boolean hadProblems = false;

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();

                if (newReleaseRadioButton.isSelected()) {
                    lineRelease = model.createLineRelease(
                            releaseNameField.getText(), null, null, new ArrayList<>());
                    log.info("Created release {}", lineRelease);
                }
                else {
                    lineRelease = getSelectedRelease();
                    log.info("Using existing release {}", lineRelease);
                }

                List<Reference> oldChildren = lineRelease.getChildren();
                Set<Reference> children = new LinkedHashSet<>(oldChildren);
                for (Sample sample : samples) {
                    children.add(Reference.createFor(sample));
                }
                lineRelease.setChildren(new ArrayList<>(children));
                lineRelease.setSageSync(true);
                model.save(lineRelease);

                // TODO: use this method instead once the server-side has been generified to work with all Nodes
                //lineRelease = model.addChildren(this.lineRelease, samples);
                log.info("Added {} samples to release", lineRelease.getChildren().size()-oldChildren.size());

                numAnnotations = annotatePublishObjective(samples, objectives);
                hadProblems |= numAnnotations != samples.size()*objectives.size();
                log.info("Annotated samples for release");

                numPublishingNames = annotatePublishNames(samples);
                hadProblems |= numPublishingNames != samples.size();
                log.info("Added {} line publishing names", numPublishingNames);
            }

            @Override
            protected void hadSuccess() {
                setVisible(false);
                ActivityLogHelper.logUserAction("StageForPublishingDialog.processSave", lineRelease.getId());

                if (hadProblems) {
                    log.warn("Some problems were encountered");
                    // TODO: complete this when we have synchronous releasing
                    // show all problems in a dialog popup
                }

                DomainExplorerTopComponent.getInstance().refresh(() -> {
                    DomainExplorerTopComponent.getInstance().expandNodeById(FlyLineReleasesNode.NODE_ID);
                    DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(lineRelease.getId());
                    return null;
                });
            }

            @Override
            protected void hadError(Throwable error) {
                setVisible(false);
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(),
                "Staging samples for publishing", ""));
        worker.execute();
    }

    private int annotatePublishObjective(Collection<Sample> samples, Collection<String> objectives) throws Exception {
        int numAnnotations = 0;
        Ontology publicationOntology = getPublicationOntology();
        ApplyAnnotationAction action = ApplyAnnotationAction.get();
        for (String objective : objectives) {
            OntologyTerm publishingNameTerm = getPublishedTerm(publicationOntology, String.format(ANNOTATION_PUBLISH_OBJECTIVE, objective));
            List<Annotation> annotations = action.setObjectAnnotations(new ArrayList<>(samples), publishingNameTerm, null, null);
            if (annotations != null) {
                numAnnotations += annotations.size();
            }
        }
        return numAnnotations;
    }

    private int annotatePublishNames(Collection<Sample> samples) {
        ApplyPublishingNamesActionListener a = new ApplyPublishingNamesActionListener(samples, false,true);
        a.actionPerformed(null);
        return a.getNumPublishingNamesApplied();
    }

    private Ontology getPublicationOntology() throws Exception {
        Ontology publicationOntology = null;
        for(Ontology ontology : DomainMgr.getDomainMgr().getModel().getOntologies()) {
            if (ontology.getOwnerKey().equals(PUBLICATION_OWNER) && ontology.getName().equals(PUBLICATION_ONTOLOGY_NAME)) {
                if (publicationOntology==null) {
                    publicationOntology = ontology;
                }
                else {
                    log.warn("More than one ontology found! Make sure that "+PUBLICATION_OWNER+" only has a single Ontology named "+PUBLICATION_ONTOLOGY_NAME);
                }
            }
        }

        if (publicationOntology!=null) {
            return publicationOntology;
        }
        else {
            throw new IllegalStateException("No publication ontology found. Make sure that "+PUBLICATION_OWNER+" has an Ontology named "+PUBLICATION_ONTOLOGY_NAME);
        }
    }

    private OntologyTerm getPublishedTerm(Ontology publicationOntology, String termName) {
        OntologyTerm publishedTerm = publicationOntology.findTerm(termName);

        if (publishedTerm==null) {
            throw new IllegalStateException("No ontology term owned by "+PUBLICATION_OWNER+" was found with name '"+termName+"'");
        }

        return publishedTerm;
    }

    /**
     * Wrapper to present ColorDepthSearch with names in a combo box.
     */
    private class ReleaseWrapper {

        LineRelease release;

        ReleaseWrapper(LineRelease search) {
            this.release = search;
        }

        @Override
        public String toString() {
            if (release ==null) {
                return "";
            }
            return release.getName();
        }
    }

}
