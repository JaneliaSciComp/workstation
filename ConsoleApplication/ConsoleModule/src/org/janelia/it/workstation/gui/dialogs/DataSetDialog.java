package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.model.entity.cv.SampleImageType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A dialog for viewing the list of accessible data sets, editing them, and adding new ones. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetDialog extends ModalDialog {
    
    private static final String SLIDE_CODE_PATTERN = "{Slide Code}";
	private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-"+SLIDE_CODE_PATTERN;

    private DataSetListDialog parentDialog;
	
    private JPanel attrPanel;
    private JTextField nameInput;
    private JTextField identifierInput;
    private JTextField sampleNamePatternInput;
    private JComboBox<SampleImageType> sampleImageInput;
    private JCheckBox sageSyncCheckbox;
    private HashMap<String,JCheckBox> processCheckboxes = new LinkedHashMap<>();
    
    private Entity dataSetEntity;
   
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
    
    private Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    public void addSeparator(JPanel panel, String text, boolean first) {
    	JLabel label = new JLabel(text);
    	label.setFont(separatorFont);
    	panel.add(label, "split 2, span"+(first?"":", gaptop 10lp"));
    	panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }
    
    private void updateDataSetIdentifier() {
    	if (dataSetEntity==null) {
    		identifierInput.setText(EntityUtils.createDenormIdentifierFromName(SessionMgr.getSubjectKey(), nameInput.getText()));
    	} 
    }
    
    public void showForDataSet(final Entity dataSetEntity) {

        this.dataSetEntity = dataSetEntity;

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

        if (dataSetEntity != null) {

            nameInput.setText(dataSetEntity.getName());

            setDataSetAttributeText(identifierInput, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            setDataSetAttributeText(sampleNamePatternInput, EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);

            String sampleImageType = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);
            if (sampleImageType != null) {
                sampleImageInput.setSelectedItem(SampleImageType.valueOf(sampleImageType));
            }
            if (dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC) != null) {
                sageSyncCheckbox.setSelected(true);
            }
            applyCheckboxValues(processCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
        } else {
            nameInput.setText("");
            applyCheckboxValues(processCheckboxes, PipelineProcess.FlyLightUnaligned.toString());
        }
        
        packAndShow();
    }

    private void setDataSetAttributeText(JTextComponent component,
                                         String attribute) {
        String value = dataSetEntity.getValueByAttributeName(attribute);
        if (value != null) {
            component.setText(value);
        }
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(DataSetDialog.this);

        final String sampleNamePattern = sampleNamePatternInput.getText();
        if (!sampleNamePattern.contains(SLIDE_CODE_PATTERN)) {
            JOptionPane.showMessageDialog(this,
                                          "Sample name pattern must contain the unique identifier \""+SLIDE_CODE_PATTERN+"\"",
                                          "Invalid Sample Name Pattern",
                                          JOptionPane.ERROR_MESSAGE);
            sampleNamePatternInput.requestFocus();
            return;
        }

        final String sampleImageType = ((SampleImageType)sampleImageInput.getSelectedItem()).name();

        SimpleWorker worker = new SimpleWorker() {

            private ModelMgr modelMgr = ModelMgr.getModelMgr();

            @Override
            protected void doStuff() throws Exception {

                if (dataSetEntity == null) {
                    dataSetEntity = modelMgr.createDataSet(nameInput.getText());
                } else {
                    dataSetEntity.setName(nameInput.getText());
                }

                updateDataSetAttribute(sampleNamePattern, EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);
                updateDataSetAttribute(sampleImageType, EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);

                modelMgr.setOrUpdateValue(dataSetEntity, EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, getCheckboxValues(processCheckboxes));

                if (sageSyncCheckbox.isSelected()) {
                    ModelMgr.getModelMgr().setAttributeAsTag(dataSetEntity, EntityConstants.ATTRIBUTE_SAGE_SYNC);
                } else {
                    removeDataSetAttribute(EntityConstants.ATTRIBUTE_SAGE_SYNC);
                }
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

            private void removeDataSetAttribute(String attributeType) throws Exception {
                final EntityData typeEd = dataSetEntity.getEntityDataByAttributeName(attributeType);
                if (typeEd != null) {
                    dataSetEntity.getEntityData().remove(typeEd);
                    modelMgr.removeEntityData(typeEd);
                }
            }

            private void updateDataSetAttribute(String value,
                                                String attributeType) throws Exception {
                if (!StringUtils.isEmpty(value)) {
                    modelMgr.setOrUpdateValue(dataSetEntity, attributeType, value);
                } else {
                    removeDataSetAttribute(attributeType);
                }
            }
        };

        worker.execute();
    }

    private void addCheckboxes(final Object[] choices, final HashMap<String,JCheckBox> checkboxes, final JPanel panel) {
    	for(Object choice : choices) {
    		NamedEnum namedEnum = ((NamedEnum)choice);
    		JCheckBox checkBox = new JCheckBox(namedEnum.getName());
    		checkboxes.put(namedEnum.toString(), checkBox);
        	panel.add(checkBox);
    	}
    }
    
    private void applyCheckboxValues(final HashMap<String,JCheckBox> checkboxes, String selected) {
    	
    	for(JCheckBox checkbox : checkboxes.values()) {
    		checkbox.setSelected(false);
    	}
    	
    	if (StringUtils.isEmpty(selected)) return;
    	
    	for(String value : selected.split(",")) {
    		JCheckBox checkbox = checkboxes.get(value);
    		if (checkbox!=null) {
    			checkbox.setSelected(true);
    		}
    	}
    }
    
    private String getCheckboxValues(final HashMap<String,JCheckBox> checkboxes) {

    	StringBuilder sb = new StringBuilder();
    	for(String key : checkboxes.keySet()) {
    		JCheckBox checkbox = checkboxes.get(key);
    		if (checkbox!=null && checkbox.isSelected()) {
        		if (sb.length()>0) sb.append(",");
        		sb.append(key);
    		}
    	}
    	
    	return sb.toString();
    }
}
