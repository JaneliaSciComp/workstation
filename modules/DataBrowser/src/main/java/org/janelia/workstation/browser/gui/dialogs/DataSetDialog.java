package org.janelia.workstation.browser.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.PipelineProcess;
import org.janelia.model.security.Subject;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.Set;

import static org.janelia.workstation.core.util.Utils.SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI;

/**
 * A dialog for viewing the list of accessible data sets, editing them, and
 * adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private static final String SLIDE_CODE_PATTERN = "{Slide Code}";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-" + SLIDE_CODE_PATTERN;

    private static final String[][] VALID_CONFIG_PATHS = {
            {"Dickson Lab Light Imagery","/groups/scicomp/informatics/data/dickson_light_imagery-config.xml"},
            {"FlyLight Light Imagery","/groups/scicomp/informatics/data/flylightflip_light_imagery-config.xml"},
            {"Lee Lab Light Imagery","/groups/scicomp/informatics/data/leetlineage_light_imagery-config.xml"},
    };

    private static final String[][] VALID_GRAMMAR_PATHS = {
            {"Dickson Lab","/misc/local/pipeline/grammar/dickson.gra"},
            {"FlyLight Polarity","/misc/local/pipeline/grammar/flylightpolarity.gra"},
            {"FlyLight FLIP","/misc/local/pipeline/grammar/flylightflip.gra"},
            {"FlyLight Split Screen Review","/misc/local/pipeline/grammar/split_screen_review.gra"},
            {"Project Technical Resources","/misc/local/pipeline/grammar/projtechres.gra"},
            {"FlyLight Test","/misc/local/pipeline/grammar/flylighttest.gra"},
            {"FlyLight Polarity","/misc/local/pipeline/grammar/flylightpolarity.gra "},
            {"Truman Lab Larval Split Screen","/misc/local/pipeline/grammar/trumanj_larval_ss.gra"},
            {"Lee Lab Pan Lineage","/misc/local/pipeline/grammar/leet_pan_lineage.gra"},
            {"Lee Lab Central Brain Lineage","/misc/local/pipeline/grammar/leet_central_brain_lineage.gra"},
    };

    DocumentListener dataSetListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            updateDataSetIdentifier();
        }
        public void removeUpdate(DocumentEvent e) {
            updateDataSetIdentifier();
        }
        public void insertUpdate(DocumentEvent e) {
            updateDataSetIdentifier();
        }
    };

    private JPanel attrPanel;
    private JTextField ownerInput;
    private JTextField nameInput;
    private JTextField identifierInput;
    private JTextField sampleNamePatternInput;
    private JTextField sageConfigPathInput;
    private JTextField sageGrammarPathInput;
    private JCheckBox sageSyncCheckbox;
    private JCheckBox neuronSeparationCheckbox;
    private JTextField unalignedCompressionInput;
    private JTextField alignedCompressionInput;
    private JTextField separationCompressionInput;
    private String currPipelineProcess;

    private DataSet dataSet;

    public DataSetDialog() {
        this(null);
    }

    public DataSetDialog(DataSetListDialog parentDialog) {

        super(parentDialog);

        setTitle("Data Set Definition");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton okButton = new JButton("Save");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(e -> saveAndClose());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (nameInput.isEditable()) {
                    nameInput.requestFocus();
                }
                else {
                    okButton.requestFocus();
                }
            }
        });
    }

    public void showForNewDataSet() {
        showForDataSet(null);
    }

    public void showForDataSet(final DataSet dataSet) {

        this.dataSet = dataSet;

        attrPanel.removeAll();

        addSeparator(attrPanel, "Data Set Attributes", true);

        final JLabel ownerLabel = new JLabel("Data Set Owner: ");
        ownerInput = new JTextField(16);

        ownerLabel.setLabelFor(nameInput);
        if (AccessManager.getAccessManager().isDataSetCreator()) {
            attrPanel.add(ownerLabel, "gap para");
            attrPanel.add(ownerInput);
        }

        final JLabel nameLabel = new JLabel("Data Set Name: ");
        nameInput = new JTextField(40);

        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel, "gap para");
        attrPanel.add(nameInput);

        // Set up listeners
        ownerInput.getDocument().addDocumentListener(dataSetListener);
        nameInput.getDocument().addDocumentListener(dataSetListener);

        final JLabel identifierLabel = new JLabel("Data Set Identifier: ");
        identifierInput = new JTextField(40);
        identifierInput.setEditable(false);
        identifierLabel.setLabelFor(identifierInput);
        attrPanel.add(identifierLabel, "gap para");
        attrPanel.add(identifierInput);

        //final JLabel sampleNamePatternLabel = new JLabel("Sample Name Pattern: ");
        sampleNamePatternInput = new JTextField(40);
        sampleNamePatternInput.setText(DEFAULT_SAMPLE_NAME_PATTERN);
        // Hide this for now, because no one is using it, and it just adds to confusion around creating data sets
        //sampleNamePatternLabel.setLabelFor(sampleNamePatternInput);
        //attrPanel.add(sampleNamePatternLabel, "gap para");
        //attrPanel.add(sampleNamePatternInput);

        sageConfigPathInput = new JTextField(40);
        String currConfigPath = null;
        if (dataSet != null && dataSet.getSageConfigPath() != null) {
            currConfigPath = dataSet.getSageConfigPath().trim();
        }
        addPathControls(VALID_CONFIG_PATHS, sageConfigPathInput, currConfigPath,"SAGE Config: ",
                "", "Choose SAGE Config");

        sageGrammarPathInput = new JTextField(40);
        String currGrammarPath = null;
        if (dataSet != null && dataSet.getSageGrammarPath() != null) {
            currGrammarPath = dataSet.getSageGrammarPath().trim();
        }
        addPathControls(VALID_GRAMMAR_PATHS, sageGrammarPathInput, currGrammarPath,"SAGE Grammar: ",
                "", "Choose SAGE Grammar");

        DropDownButton pipelineProcessButton = new DropDownButton();
        pipelineProcessButton.setText("Select Sample Processing Pipeline"); // default value

        if (dataSet==null) {
            // Default pipeline for new data sets
            currPipelineProcess = PipelineProcess.FlyLightSample.name();
        }
        else if (dataSet.getPipelineProcesses() != null && !dataSet.getPipelineProcesses().isEmpty()) {
            currPipelineProcess = dataSet.getPipelineProcesses().get(0);
        }

        ButtonGroup typeGroup = new ButtonGroup();
        for (PipelineProcess process : PipelineProcess.values()) {
            JMenuItem menuItem = new JRadioButtonMenuItem(process.getName());
            if (process.name().equals(currPipelineProcess)) {
                menuItem.setSelected(true);
                pipelineProcessButton.setText(process.getName());
            }
            menuItem.addActionListener(e -> {
                currPipelineProcess = process.name();
                pipelineProcessButton.setText(process.getName());
            });
            typeGroup.add(menuItem);
            pipelineProcessButton.addMenuItem(menuItem);
        }

        final JLabel pipelineProcessLabel = new JLabel("Sample Processing Pipeline");
        pipelineProcessLabel.setLabelFor(pipelineProcessButton);
        attrPanel.add(pipelineProcessLabel, "gap para");
        attrPanel.add(pipelineProcessButton);

        unalignedCompressionInput = new JTextField(40);
        unalignedCompressionInput.setEditable(false);
        alignedCompressionInput = new JTextField(40);
        alignedCompressionInput.setEditable(false);
        separationCompressionInput = new JTextField(40);
        separationCompressionInput.setEditable(false);

        if (dataSet != null) {
            final JLabel unalignedCompressionLabel = new JLabel("Default Unaligned Compression: ");
            unalignedCompressionLabel.setLabelFor(unalignedCompressionInput);
            attrPanel.add(unalignedCompressionLabel, "gap para");
            attrPanel.add(unalignedCompressionInput);

            final JLabel alignedCompressionLabel = new JLabel("Default Aligned Compression: ");
            alignedCompressionLabel.setLabelFor(alignedCompressionInput);
            attrPanel.add(alignedCompressionLabel, "gap para");
            attrPanel.add(alignedCompressionInput);

            final JLabel separationCompressionLabel = new JLabel("Default Separation Compression: ");
            separationCompressionLabel.setLabelFor(separationCompressionInput);
            attrPanel.add(separationCompressionLabel, "gap para");
            attrPanel.add(separationCompressionInput);
        }

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));

        sageSyncCheckbox = new JCheckBox("Synchronize images from SAGE");
        sageSyncCheckbox.setSelected(true);
        optionsPanel.add(sageSyncCheckbox);

        neuronSeparationCheckbox = new JCheckBox("Run neuron separation (if pipeline supports it)");
        neuronSeparationCheckbox.setEnabled(SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI);
        optionsPanel.add(neuronSeparationCheckbox);

        final JLabel processingOptionsLabel = new JLabel("Processing Options: ");
        processingOptionsLabel.setLabelFor(optionsPanel);
        attrPanel.add(processingOptionsLabel, "gap para, top");
        attrPanel.add(optionsPanel);

        if (dataSet != null) {

            ownerInput.setText(dataSet.getOwnerName());
            String identifier = dataSet.getIdentifier();
            nameInput.setText(identifier.substring(identifier.indexOf('_')+1));

            // It's not possible to edit the identifier (and thus owner or name) of an existing data set
            ownerInput.setEnabled(false);
            nameInput.setEnabled(false);

            identifierInput.setText(dataSet.getIdentifier());
            sampleNamePatternInput.setText(dataSet.getSampleNamePattern());
            sageConfigPathInput.setText(dataSet.getSageConfigPath());
            sageGrammarPathInput.setText(dataSet.getSageGrammarPath());

            String currUnalignedCompression = SampleUtils.getUnalignedCompression(dataSet, null);
            String currAlignedCompression = SampleUtils.getAlignedCompression(dataSet, null);
            String currSepCompression = SampleUtils.getSeparationCompression(dataSet, null);

            unalignedCompressionInput.setText(currUnalignedCompression);
            alignedCompressionInput.setText(currAlignedCompression);
            separationCompressionInput.setText(currSepCompression);
            
            sageSyncCheckbox.setSelected(dataSet.isSageSync());
            neuronSeparationCheckbox.setSelected(dataSet.isNeuronSeparationSupported());

            ActivityLogHelper.logUserAction("DataSetDialog.showDialog", dataSet);
        }
        else {
            ownerInput.setText(AccessManager.getSubjectName());
            nameInput.setText("");

            // Allow editing
            ownerInput.setEnabled(true);
            nameInput.setEnabled(true);

            ActivityLogHelper.logUserAction("DataSetDialog.showDialog");
        }

        packAndShow();
    }

    private void addPathControls(final String[][] validNamedPaths, final JTextField rawPathInput, String currValue,
                                 String buttonLabelText, String overrideFieldLabelText, String defaultButtonText) {

        DropDownButton dropDownButton = new DropDownButton();
        ButtonGroup typeGroup = new ButtonGroup();
        String selectedName = null;
        for (String[] namedPath : validNamedPaths) {
            String name = namedPath[0];
            String path = namedPath[1];
            JMenuItem menuItem = new JRadioButtonMenuItem(name);
            if (path.equals(currValue)) {
                menuItem.setSelected(true);
                selectedName = name;
            }
            menuItem.addActionListener(e -> {
                rawPathInput.setText(path);
                dropDownButton.setText(name);
            });
            typeGroup.add(menuItem);
            dropDownButton.addMenuItem(menuItem);
        }
        if (selectedName!=null) {
            dropDownButton.setText(selectedName);
        }
        else {
            dropDownButton.setText(defaultButtonText);
        }

        JCheckBox overrideCheckbox = new JCheckBox("Override");
        overrideCheckbox.addItemListener(e -> {
            boolean selected = e.getStateChange()==ItemEvent.SELECTED;
            rawPathInput.setEnabled(selected);
            dropDownButton.setEnabled(!selected);
            if (selected) {
                dropDownButton.setText("Custom Path");
            }
            else {
                dropDownButton.setText(defaultButtonText);
                rawPathInput.setText("");
            }
        });

        JPanel overridePanel = new JPanel();
        overridePanel.setLayout(new BoxLayout(overridePanel, BoxLayout.LINE_AXIS));
        overridePanel.add(dropDownButton);
        overridePanel.add(Box.createHorizontalStrut(10));
        overridePanel.add(overrideCheckbox);

        final JLabel buttonLabel = new JLabel(buttonLabelText);
        attrPanel.add(buttonLabel, "gap para");
        attrPanel.add(overridePanel);

        final JLabel rawFieldLabel = new JLabel(overrideFieldLabelText);
        rawFieldLabel.setLabelFor(rawPathInput);
        attrPanel.add(rawFieldLabel, "gap para");
        attrPanel.add(rawPathInput);

        // By default, ask the user to choose from the list
        overrideCheckbox.setSelected(false);
        rawPathInput.setEnabled(false);
        dropDownButton.setEnabled(true);

        if (currValue!=null && selectedName==null) {
            // But enable raw override if the current value is not in the list (custom value)
            overrideCheckbox.setSelected(true);
            rawPathInput.setEnabled(true);
            dropDownButton.setEnabled(false);
        }
    }

    private void saveAndClose() {

        if (dataSet!=null && !ClientDomainUtils.hasWriteAccess(dataSet)) {
            JOptionPane.showMessageDialog(this, "You do not have access to make changes to this data set", "Access denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String sampleNamePattern = sampleNamePatternInput.getText();
        if (!sampleNamePattern.contains(SLIDE_CODE_PATTERN)) {
            JOptionPane.showMessageDialog(this,
                    "Sample name pattern must contain the unique identifier \"" + SLIDE_CODE_PATTERN + "\"",
                    "Invalid Sample Name Pattern",
                    JOptionPane.ERROR_MESSAGE);
            sampleNamePatternInput.requestFocus();
            return;
        }

        final String name = nameInput.getText();
        final String sageConfigPath = sageConfigPathInput.getText();
        final String sageGrammarPath = sageGrammarPathInput.getText();

        if (dataSet == null && StringUtils.isBlank(name)) {
            JOptionPane.showMessageDialog(this,
                    "You must give this data set a name in order to save it",
                    "Data set name is empty",
                    JOptionPane.ERROR_MESSAGE);
            nameInput.requestFocus();
            return;
        }

        if (StringUtils.isBlank(sageConfigPath)) {
            JOptionPane.showMessageDialog(this,
                    "Samples added to this data set cannot be processed without a SAGE configuration",
                    "SAGE Config Path is empty",
                    JOptionPane.ERROR_MESSAGE);
            sageConfigPathInput.requestFocus();
            return;
        }

        if (StringUtils.isBlank(sageGrammarPath)) {
            JOptionPane.showMessageDialog(this,
                    "Samples added to this data set cannot be processed without a SAGE grammar",
                    "SAGE Grammar Path is empty",
                    JOptionPane.ERROR_MESSAGE);
            sageGrammarPathInput.requestFocus();
            return;
        }

        UIUtils.setWaitingCursor(DataSetDialog.this);

        String owner = getOwnerText();

        SimpleWorker worker = new SimpleWorker() {

            boolean subjectNotFound = false;

            @Override
            protected void doStuff() throws Exception {

                if (dataSet == null) {

                    // Determine owner
                    Subject subject = DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(owner);
                    if (subject == null) {
                        subjectNotFound = true;
                        return;
                    }

                    // Create new data set
                    dataSet = new DataSet();
                    dataSet.setOwnerKey(subject.getKey());
                    dataSet.setIdentifier(identifierInput.getText());

                    if (!AccessManager.getSubjectKey().equals(subject.getKey())) {
                        // Ths user is creating this data set on behalf of someone else
                        Set<String> writerSet = AccessManager.getAccessManager().getActualWriterSet();
                        // TODO: This logic should at least be moved to to the server side
                        // Share the data set back to the technician group that created it
                        if (writerSet.contains(AccessManager.FLYLIGHT_GROUP)) {
                            dataSet.getReaders().add(AccessManager.FLYLIGHT_GROUP);
                            dataSet.getWriters().add(AccessManager.FLYLIGHT_GROUP);
                        }
                        if (writerSet.contains(AccessManager.PTR_GROUP)) {
                            dataSet.getReaders().add(AccessManager.PTR_GROUP);
                            dataSet.getWriters().add(AccessManager.PTR_GROUP);
                        }
                    }
                } 

                dataSet.setName(dataSet.getIdentifier());
                dataSet.setSampleNamePattern(sampleNamePattern);
                dataSet.setPipelineProcesses(Collections.singletonList(currPipelineProcess));
                dataSet.setSageSync(sageSyncCheckbox.isSelected());
                dataSet.setNeuronSeparationSupported(neuronSeparationCheckbox.isSelected());
                dataSet.setSageConfigPath(sageConfigPath);
                dataSet.setSageGrammarPath(sageGrammarPath);
                
                DomainMgr.getDomainMgr().getModel().save(dataSet);
            }

            @Override
            protected void hadSuccess() {
                UIUtils.setDefaultCursor(DataSetDialog.this);
                if (subjectNotFound) {
                    JOptionPane.showMessageDialog(
                            DataSetDialog.this,
                            "Owner could not be found: "+owner,
                            "Data set owner not found",
                            JOptionPane.ERROR_MESSAGE);
                }
                else {
                    setVisible(false);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                UIUtils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }


    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    private void updateDataSetIdentifier() {
        if (dataSet == null) {
            identifierInput.setText(createDenormalizedIdentifier(ownerInput.getText(), nameInput.getText()));
        }
    }

    private String getOwnerText() {
        String owner = ownerInput.getText();
        if (StringUtils.isBlank(owner)) owner = AccessManager.getSubjectName();
        return owner;
    }

    private String createDenormalizedIdentifier(String username, String name) {
        // default ownership to current user
        if (StringUtils.isBlank(username)) username = AccessManager.getSubjectName();
        // strip off any prefix
        username = SubjectUtils.getSubjectName(username);
        return username+"_"+name.toLowerCase().replaceAll("\\W+", "_");
    }
}
