package org.janelia.workstation.site.jrc.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.AbstractDomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.LineRelease;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.context.ApplyAnnotationActionListener;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.model.PreferenceConstants;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.site.jrc.action.context.ApplyPublishingNamesActionListener;
import org.janelia.workstation.site.jrc.nodes.FlyLineReleasesNode;
import org.janelia.workstation.site.jrc.util.SiteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        this.newReleaseLabel = new JLabel("Release name");
        this.releaseNameField = new JTextField(25);

        this.newReleaseRadioButton = new JRadioButton("Create a new release:");
        newReleaseRadioButton.setSelected(true);
        group.add(newReleaseRadioButton);
        newReleaseRadioButton.addActionListener((ActionEvent e) -> {
            releaseNameField.requestFocus();
            updateState();
        });

        objectivesPanel = new JPanel();
        objectivesPanel.setLayout(new BoxLayout(objectivesPanel, BoxLayout.PAGE_AXIS));

        this.addPanel = new JPanel();
        addPanel.setLayout(new MigLayout("wrap 2, ins 20, fillx", "[grow 0]20[grow 1]"));
        addPanel.add(existingReleaseRadioButton, "");
        addPanel.add(newReleaseRadioButton, "");
        addPanel.add(releaseComboPanel, "");
        addPanel.add(releaseNameField, "");
        addPanel.add(new JLabel("Stage images for these microscope objectives:"), "span 2, gaptop 10px");
        addPanel.add(objectivesPanel, "span 2");

        add(addPanel, BorderLayout.CENTER);

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

        List<LineRelease> sampleLineReleases = samples.stream().filter(Sample::isSamplePublishedToStaging).flatMap(s -> {
            try {
                return DomainMgr.getDomainMgr().getModel().getLineReleases(s).stream();
            } catch (Exception e) {
                log.error("Error getting line releases for sample "+s, e);
                return Stream.empty();
            }
        }).distinct().collect(Collectors.toList());

        if (!sampleLineReleases.isEmpty()) {
            String sampleCsv = StringUtils.joinWith(", ", sampleLineReleases.stream().map(AbstractDomainObject::getName).toArray());
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "One of the selected samples is already published as part of a Fly Line Release ("+sampleCsv+"). Samples cannot be added to more than one release.",
                    "Cannot stage samples for publishing", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setTitle("Stage "+samples.size()+" Samples for Publishing");

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
            private String lastSelectedRelease;
            private Collection<String> lastSelectedObjectives;

            @Override
            protected void doStuff() throws Exception {
                lastSelectedRelease = getLastSelectedRelease();
                lastSelectedObjectives = getLastSelectedObjectives();
                for (LineRelease lineRelease : DomainMgr.getDomainMgr().getModel().getLineReleases()) {
                    if (ClientDomainUtils.hasWriteAccess(lineRelease)) {
                        releases.add(lineRelease);
                    }
                }
            }

            @Override
            protected void hadSuccess() {

                log.info("lastSelectedRelease: {}", lastSelectedRelease);
                log.info("lastSelectedObjectives: {}", lastSelectedObjectives);

                releaseComboPanel.removeAll();
                releaseComboPanel.add(releaseComboBox);

                // By default, select "New Release"
                newReleaseRadioButton.setSelected(true);
                existingReleaseRadioButton.setSelected(false);

                if (releases.isEmpty()) {
                    existingReleaseRadioButton.setEnabled(false);
                    releaseComboBox.setEnabled(false);
                }
                else {
                    existingReleaseRadioButton.setEnabled(true);
                    releaseComboBox.setEnabled(true);
                    DefaultComboBoxModel<ReleaseWrapper> model =
                            (DefaultComboBoxModel<ReleaseWrapper>) releaseComboBox.getModel();
                    ReleaseWrapper last = null;
                    ReleaseWrapper toSelect = null;
                    for(LineRelease release : releases) {
                        last = new ReleaseWrapper(release);
                        model.addElement(last);
                        if (release.getName().equals(lastSelectedRelease)) {
                            toSelect = last;
                        }
                    }
                    if (toSelect!=null) {
                        model.setSelectedItem(toSelect);
                        // Select "Existing Release"
                        newReleaseRadioButton.setSelected(false);
                        existingReleaseRadioButton.setSelected(true);
                    }
                    else if (last!=null) {
                        model.setSelectedItem(last); // select the last item by default
                    }
                }

                for (String s : objectiveCheckboxMap.keySet()) {
                    JCheckBox checkBox = objectiveCheckboxMap.get(s);
                    if (checkBox!=null) {
                        checkBox.setSelected(lastSelectedObjectives.contains(s));
                    }
                }

                updateState();
                revalidate();
                pack();

                SwingUtilities.invokeLater(() -> {
                    // Focus the name field so the user can start typing
                    if (newReleaseRadioButton.isSelected()) {
                        releaseNameField.requestFocus();
                    }
                });

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

        if (objectives.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "You must select at least one objective",
                    "No objectives selected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ActivityLogHelper.logUserAction("StageForPublishingDialog.processSave");
        setVisible(false);

        SimpleWorker worker = new SimpleWorker() {

            private LineRelease lineRelease;
            private int numExpectedAnnotations;
            private int numExpectedPublishingNames;
            private int numAnnotations;
            private int numPublishingNames;
            private boolean hadProblems = false;

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();

                if (newReleaseRadioButton.isSelected()) {
                    lineRelease = model.createLineRelease(releaseNameField.getText());
                    log.info("Created release {}", lineRelease);
                }
                else {
                    lineRelease = getSelectedRelease();
                    log.info("Using existing release {}", lineRelease);
                }

                log.info("Setting SAGE Sync to true for release {}", lineRelease.getName());
                lineRelease.setSageSync(true);
                model.save(lineRelease);

                setStatus("Adding samples to release");
                log.info("Adding {} samples to release {}", samples.size(), lineRelease.getName());
                List<Reference> oldChildren = lineRelease.getChildren();
                lineRelease = model.addChildren(this.lineRelease, samples);
                log.info("Added {} new samples to release", lineRelease.getChildren().size()-oldChildren.size());

                setStatus("Adding release annotations");
                numAnnotations = annotatePublishObjective(samples, objectives);
                log.info("Adding release annotations for {} objectives in {} samples", objectives.size(), samples.size());
                numExpectedAnnotations = samples.size() * objectives.size();
                hadProblems |= numAnnotations != numExpectedAnnotations;
                log.info("Added {} release annotations", numAnnotations);

                setStatus("Adding publishing names");
                log.info("Adding publishing names for {} samples", samples.size());
                numPublishingNames = annotatePublishNames(samples);
                numExpectedPublishingNames = samples.size();
                hadProblems |= numPublishingNames != numExpectedPublishingNames;
                log.info("Added {} publishing names", numPublishingNames);
            }

            @Override
            protected void hadSuccess() {

                setLastSelectedRelease(lineRelease.getName());
                setLastSelectedObjectives(objectives);

                if (hadProblems) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("Some problems were encountered while staging samples for publishing: ");
                    if (numAnnotations != numExpectedAnnotations) {
                        sb.append("Expected ").append(numExpectedAnnotations).append(" annotations, but created ").append(numAnnotations).append(". ");
                    }
                    if (numPublishingNames != numExpectedPublishingNames) {
                        sb.append("Expected ").append(numExpectedPublishingNames).append(" publishing names, but set ").append(numPublishingNames).append(". ");
                    }

                    log.warn(sb.toString());
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            sb.toString(),
                            "Problems Staging Samples for Publishing",
                            JOptionPane.WARNING_MESSAGE);
                }

                SiteUtils.navigateToLineRelease(lineRelease.getId());
            }

            @Override
            protected void hadError(Throwable error) {
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
        final ApplyAnnotationActionListener action = new ApplyAnnotationActionListener();
        action.setBatchMode(true);
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
        ApplyPublishingNamesActionListener a = new ApplyPublishingNamesActionListener(samples, false,false,true, this);
        a.actionPerformed(null);
        return a.getNumPublishingNamesApplied() + a.getNumPublishingNamesExisting();
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
    private static class ReleaseWrapper {

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

    private String getLastSelectedRelease() {
        try {
            String lastRelease = FrameworkAccess.getRemotePreferenceValue(PreferenceConstants.CATEGORY_PREVIOUS_VALUE, PreferenceConstants.KEY_PREVIOUS_VALUE_RELEASE_NAME, null);
            log.info("Got last release: {}", lastRelease);
            return lastRelease;
        } catch (Exception e) {
            log.error("Error getting last selected release", e);
            return null;
        }
    }

    private void setLastSelectedRelease(String selectedRelease) {
        try {
            log.info("Saving last selected release as {}", selectedRelease);
            FrameworkAccess.setRemotePreferenceValue(PreferenceConstants.CATEGORY_PREVIOUS_VALUE, PreferenceConstants.KEY_PREVIOUS_VALUE_RELEASE_NAME, selectedRelease);
        }
        catch (Exception e) {
            log.error("Error setting last selected release", e);
        }
    }

    private Collection<String> getLastSelectedObjectives() {
        try {
            String serializedObjectives = FrameworkAccess.getRemotePreferenceValue(PreferenceConstants.CATEGORY_PREVIOUS_VALUE, PreferenceConstants.KEY_PREVIOUS_VALUE_RELEASE_OBJECTIVES, "");
            Collection<String> objectives = Arrays.asList(StringUtils.split(serializedObjectives, ","));
            log.info("Got last selected objectives: {} -> {}", serializedObjectives, objectives);
            return objectives;
        }
        catch (Exception e) {
            log.error("Error getting last selected objectives", e);
            return null;
        }
    }

    private void setLastSelectedObjectives(Collection<String> objectives) {
        try {
            log.info("Saving last selected objectives {} as {}", objectives, StringUtils.join(objectives, ','));
            FrameworkAccess.setRemotePreferenceValue(PreferenceConstants.CATEGORY_PREVIOUS_VALUE, PreferenceConstants.KEY_PREVIOUS_VALUE_RELEASE_OBJECTIVES, StringUtils.join(objectives, ','));
        }
        catch (Exception e) {
            log.error("Error setting last selected objectives", e);
        }
    }
}
