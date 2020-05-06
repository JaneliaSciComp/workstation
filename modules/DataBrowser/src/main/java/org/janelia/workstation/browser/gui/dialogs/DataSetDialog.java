package org.janelia.workstation.browser.gui.dialogs;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.PipelineProcess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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

    private JPanel attrPanel;
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
    private HashMap<String, JRadioButton> processCheckboxes = new LinkedHashMap<>();

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
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForNewDataSet() {
        showForDataSet(null);
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    private void updateDataSetIdentifier() {
        if (dataSet == null) {
            identifierInput.setText(createDenormIdentifierFromName(AccessManager.getSubjectKey(), nameInput.getText()));
        }
    }

    public void showForDataSet(final DataSet dataSet) {

        this.dataSet = dataSet;

        attrPanel.removeAll();

        addSeparator(attrPanel, "Data Set Attributes", true);

        final JLabel nameLabel = new JLabel("Data Set Name: ");
        nameInput = new JTextField(40);

        nameInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }

            public void removeUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }

            public void insertUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }
        });

        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel, "gap para");
        attrPanel.add(nameInput);

        final JLabel identifierLabel = new JLabel("Data Set Identifier: ");
        identifierInput = new JTextField(40);
        identifierInput.setEditable(false);
        identifierLabel.setLabelFor(identifierInput);
        attrPanel.add(identifierLabel, "gap para");
        attrPanel.add(identifierInput);

        final JLabel sampleNamePatternLabel = new JLabel("Sample Name Pattern: ");
        sampleNamePatternInput = new JTextField(40);
        sampleNamePatternInput.setText(DEFAULT_SAMPLE_NAME_PATTERN);
        sampleNamePatternLabel.setLabelFor(sampleNamePatternInput);
        attrPanel.add(sampleNamePatternLabel, "gap para");
        attrPanel.add(sampleNamePatternInput);

        final JLabel sageConfigPathLabel = new JLabel("SAGE Config Path: ");
        sageConfigPathInput = new JTextField(80);
        sageConfigPathLabel.setLabelFor(sageConfigPathInput);
        attrPanel.add(sageConfigPathLabel, "gap para");
        attrPanel.add(sageConfigPathInput);

        final JLabel sageGrammarPathLabel = new JLabel("SAGE Grammar Path: ");
        sageGrammarPathInput = new JTextField(80);
        sageGrammarPathLabel.setLabelFor(sageGrammarPathInput);
        attrPanel.add(sageGrammarPathLabel, "gap para");
        attrPanel.add(sageGrammarPathInput);


        final JLabel unalignedCompressionLabel = new JLabel("Default Unaligned Compression: ");
        unalignedCompressionInput = new JTextField(40);
        unalignedCompressionInput.setEditable(false);
        unalignedCompressionLabel.setLabelFor(unalignedCompressionInput);
        attrPanel.add(unalignedCompressionLabel, "gap para");
        attrPanel.add(unalignedCompressionInput);

        final JLabel alignedCompressionLabel = new JLabel("Default Aligned Compression: ");
        alignedCompressionInput = new JTextField(40);
        alignedCompressionInput.setEditable(false);
        alignedCompressionLabel.setLabelFor(alignedCompressionInput);
        attrPanel.add(alignedCompressionLabel, "gap para");
        attrPanel.add(alignedCompressionInput);
        
        final JLabel separationCompressionLabel = new JLabel("Default Separation Compression: ");
        separationCompressionInput = new JTextField(40);
        separationCompressionInput.setEditable(false);
        separationCompressionLabel.setLabelFor(separationCompressionInput);
        attrPanel.add(separationCompressionLabel, "gap para");
        attrPanel.add(separationCompressionInput);
        
        
        sageSyncCheckbox = new JCheckBox("Synchronize images from SAGE");
        attrPanel.add(sageSyncCheckbox, "gap para");

        neuronSeparationCheckbox = new JCheckBox("Support Neuron Separation");
        neuronSeparationCheckbox.setToolTipText("If pipeline does Neuron Separation by default, unchecking avoids it");
        neuronSeparationCheckbox.setEnabled(SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI);
        attrPanel.add(neuronSeparationCheckbox, "gap para, span 2");

        JPanel pipelinesPanel = new JPanel();
        pipelinesPanel.setLayout(new BoxLayout(pipelinesPanel, BoxLayout.PAGE_AXIS));
        addRadioButtons(PipelineProcess.values(), processCheckboxes, pipelinesPanel);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(pipelinesPanel);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        addSeparator(attrPanel, "Pipelines", false);
        attrPanel.add(scrollPane, "span 2, growx");

        if (dataSet != null) {

            nameInput.setText(dataSet.getName());

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
            if (dataSet.getPipelineProcesses()!=null && !dataSet.getPipelineProcesses().isEmpty()) {
                applyRadioButtonValues(processCheckboxes, dataSet.getPipelineProcesses().get(0));
            }

            ActivityLogHelper.logUserAction("DataSetDialog.showDialog", dataSet);
        }
        else {
            nameInput.setText("");
            applyRadioButtonValues(processCheckboxes, PipelineProcess.FlyLightUnaligned.toString());

            ActivityLogHelper.logUserAction("DataSetDialog.showDialog");
        }

        packAndShow();
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

        final String sageConfigPath = sageConfigPathInput.getText();
        final String sageGrammarPath = sageGrammarPathInput.getText();

        UIUtils.setWaitingCursor(DataSetDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                if (dataSet == null) {
                    dataSet = new DataSet();
                    dataSet.setIdentifier(identifierInput.getText());
                } 

                dataSet.setName(nameInput.getText());
                dataSet.setSampleNamePattern(sampleNamePattern);
                java.util.List<String> pipelineProcesses = new ArrayList<>();
                pipelineProcesses.add(getRadioButtonValues(processCheckboxes));
                dataSet.setPipelineProcesses(pipelineProcesses);
                dataSet.setSageSync(sageSyncCheckbox.isSelected());
                dataSet.setNeuronSeparationSupported(neuronSeparationCheckbox.isSelected());
                dataSet.setSageConfigPath(sageConfigPath);
                dataSet.setSageGrammarPath(sageGrammarPath);
                
                DomainMgr.getDomainMgr().getModel().save(dataSet);
            }

            @Override
            protected void hadSuccess() {
                UIUtils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }

    private void addRadioButtons(final PipelineProcess[] choices, final HashMap<String, JRadioButton> radioButtons, final JPanel panel) {
        ButtonGroup group = new ButtonGroup();
        for (PipelineProcess choice : choices) {
            JRadioButton checkBox = new JRadioButton(choice.getName());
            radioButtons.put(choice.toString(), checkBox);
            panel.add(checkBox);
            group.add(checkBox);
        }
    }

    private void applyRadioButtonValues(final HashMap<String, JRadioButton> radioButtons, String selectedButtons) {

        for (JRadioButton checkbox : radioButtons.values()) {
            checkbox.setSelected(false);
        }

        if (StringUtils.isEmpty(selectedButtons)) {
            return;
        }

        for (String value : selectedButtons.split(",")) {
            JRadioButton checkbox = radioButtons.get(value);
            if (checkbox != null) {
                checkbox.setSelected(true);
            }
        }
    }

    private String getRadioButtonValues(final HashMap<String, JRadioButton> radioButtons) {

        StringBuilder sb = new StringBuilder();
        for (String key : radioButtons.keySet()) {
            JRadioButton checkbox = radioButtons.get(key);
            if (checkbox != null && checkbox.isSelected()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(key);
            }
        }

        return sb.toString();
    }

    private String createDenormIdentifierFromName (String username, String name) {
        if (username.contains(":")) username = username.split(":")[1];
        return username+"_"+name.toLowerCase().replaceAll("\\W+", "_");
    }
}
