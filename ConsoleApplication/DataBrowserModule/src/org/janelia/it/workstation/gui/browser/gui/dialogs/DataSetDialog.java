package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.janelia.it.jacs.model.domain.enums.SampleImageType;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import net.miginfocom.swing.MigLayout;

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

    private final DataSetListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput;
    private JTextField identifierInput;
    private JTextField sampleNamePatternInput;
    private JTextField sageConfigPathInput;
    private JTextField sageGrammarPathInput;
    private JComboBox<SampleImageType> sampleImageInput;
    private JCheckBox sageSyncCheckbox;
    private HashMap<String, JCheckBox> processCheckboxes = new LinkedHashMap<>();

    private DataSet dataSet;

    public DataSetDialog(DataSetListDialog parentDialog) {

        super(parentDialog);
        this.parentDialog = parentDialog;

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

        final JLabel sampleImageLabel = new JLabel("Sample Image: ");
        sampleImageInput = new JComboBox<>(SampleImageType.values());
        sampleImageLabel.setLabelFor(sampleImageInput);
        attrPanel.add(sampleImageLabel, "gap para");
        attrPanel.add(sampleImageInput);

        sageSyncCheckbox = new JCheckBox("Synchronize images from SAGE");
        attrPanel.add(sageSyncCheckbox, "gap para, span 2");

        JPanel pipelinesPanel = new JPanel();
        pipelinesPanel.setLayout(new BoxLayout(pipelinesPanel, BoxLayout.PAGE_AXIS));
        addCheckboxes(PipelineProcess.values(), processCheckboxes, pipelinesPanel);

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

            SampleImageType sampleImageType = dataSet.getSampleImageType();
            if (sampleImageType != null) {
                sampleImageInput.setSelectedItem(sampleImageType);
            }

            sageSyncCheckbox.setSelected(dataSet.isSageSync());
            if (dataSet.getPipelineProcesses()!=null && !dataSet.getPipelineProcesses().isEmpty()) {
                applyCheckboxValues(processCheckboxes, dataSet.getPipelineProcesses().get(0));
            }

            ActivityLogHelper.logUserAction("DataSetDialog.showDialog", dataSet);
        }
        else {
            nameInput.setText("");
            applyCheckboxValues(processCheckboxes, PipelineProcess.FlyLightUnaligned.toString());

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
        final String sampleImageType = ((SampleImageType) sampleImageInput.getSelectedItem()).name();

        Utils.setWaitingCursor(DataSetDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                if (dataSet == null) {
                    dataSet = new DataSet();
                    dataSet.setName(nameInput.getText());
                    dataSet.setIdentifier(identifierInput.getText());
                } else {
                    dataSet.setName(nameInput.getText());
                }

                dataSet.setSampleNamePattern(sampleNamePattern);
                dataSet.setSampleImageType(SampleImageType.valueOf(sampleImageType));
                java.util.List<String> pipelineProcesses = new ArrayList<>();
                pipelineProcesses.add(getCheckboxValues(processCheckboxes));
                dataSet.setPipelineProcesses(pipelineProcesses);
                dataSet.setSageSync(new Boolean(sageSyncCheckbox.isSelected()));
                dataSet.setSageConfigPath(sageConfigPath);
                dataSet.setSageGrammarPath(sageGrammarPath);
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.save(dataSet);
            }

            @Override
            protected void hadSuccess() {
                parentDialog.refresh();
                Utils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }

    private void addCheckboxes(final Object[] choices, final HashMap<String, JCheckBox> checkboxes, final JPanel panel) {
        for (Object choice : choices) {
            NamedEnum namedEnum = ((NamedEnum) choice);
            JCheckBox checkBox = new JCheckBox(namedEnum.getName());
            checkboxes.put(namedEnum.toString(), checkBox);
            panel.add(checkBox);
        }
    }

    private void applyCheckboxValues(final HashMap<String, JCheckBox> checkboxes, String selected) {

        for (JCheckBox checkbox : checkboxes.values()) {
            checkbox.setSelected(false);
        }

        if (StringUtils.isEmpty(selected)) {
            return;
        }

        for (String value : selected.split(",")) {
            JCheckBox checkbox = checkboxes.get(value);
            if (checkbox != null) {
                checkbox.setSelected(true);
            }
        }
    }

    private String getCheckboxValues(final HashMap<String, JCheckBox> checkboxes) {

        StringBuilder sb = new StringBuilder();
        for (String key : checkboxes.keySet()) {
            JCheckBox checkbox = checkboxes.get(key);
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
