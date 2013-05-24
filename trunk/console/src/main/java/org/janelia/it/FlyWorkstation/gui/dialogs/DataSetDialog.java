package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A dialog for viewing the list of accessible data sets, editing them, and adding new ones. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetDialog extends ModalDialog implements Accessibility {
    
    private static final String SLIDE_CODE_PATTERN = "{Slide Code}";
	private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-"+SLIDE_CODE_PATTERN;

    private DataSetListDialog parentDialog;
	
    private JPanel attrPanel;
    private JLabel nameLabel;
    private JTextField nameInput;
    private JLabel identifierLabel;
    private JTextField identifierInput;
    private JLabel sampleNamePatternLabel;
    private JTextField sampleNamePatternInput;
    private JCheckBox sageSyncCheckbox;
    private HashMap<String,JCheckBox> processCheckboxes = new HashMap<String,JCheckBox>();
    
    private Entity dataSetEntity;
   
    public DataSetDialog(DataSetListDialog parentDialog) {

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
    
    public void addSeparator(JPanel panel, String text) {
    	JLabel label = new JLabel(text);
    	label.setFont(separatorFont);
    	panel.add(label, "split 2, span, gaptop 10lp");
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
    	
    	addSeparator(attrPanel, "Data Set Attributes");
    	
        nameLabel = new JLabel("Data Set Name: ");
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

        identifierLabel = new JLabel("Data Set Identifier: ");
        identifierInput = new JTextField(40);
        identifierInput.setEditable(false);
        identifierLabel.setLabelFor(identifierInput);
        attrPanel.add(identifierLabel, "gap para");
        attrPanel.add(identifierInput);

        sampleNamePatternLabel = new JLabel("Sample Name Pattern: ");
        sampleNamePatternInput = new JTextField(40);
        sampleNamePatternInput.setText(DEFAULT_SAMPLE_NAME_PATTERN);
        sampleNamePatternLabel.setLabelFor(sampleNamePatternInput);
        attrPanel.add(sampleNamePatternLabel, "gap para");
        attrPanel.add(sampleNamePatternInput);
        
        sageSyncCheckbox = new JCheckBox("Synchronize images from SAGE");
        attrPanel.add(sageSyncCheckbox, "gap para, span 2");
        
        addSeparator(attrPanel, "Pipelines");
        addCheckboxes("Pipelines", PipelineProcess.values(), processCheckboxes, attrPanel);
        
        if (dataSetEntity!=null) {
        	nameInput.setText(dataSetEntity.getName());
        	String dataSetIdentifier = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
			if (dataSetIdentifier!=null) {
				identifierInput.setText(dataSetIdentifier);
	    	}		
			String sampleNamePattern = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);
            if (sampleNamePattern!=null) {
                sampleNamePatternInput.setText(sampleNamePattern);
            }   
        	if (dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)!=null) {
        		sageSyncCheckbox.setSelected(true);
        	}
            applyCheckboxValues(processCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
        }
        else {
        	nameInput.setText("");
            applyCheckboxValues(processCheckboxes, PipelineProcess.FlyLightUnaligned.toString());
        }
        
        packAndShow();
    }
    
    private void saveAndClose() {
    	
    	Utils.setWaitingCursor(DataSetDialog.this);

        final String sampleNamePattern = sampleNamePatternInput.getText();
        if (!sampleNamePattern.contains(SLIDE_CODE_PATTERN)) {
            JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
                    "Sample name pattern must contain the unique identifier \""+SLIDE_CODE_PATTERN+"\"", "Invalid Sample Name Pattern", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				
				if (dataSetEntity==null) {
					dataSetEntity = ModelMgr.getModelMgr().createDataSet(nameInput.getText());
				}
				else {
					dataSetEntity.setName(nameInput.getText());	
				}
				
				if (!StringUtils.isEmpty(sampleNamePattern)) {
				    ModelMgr.getModelMgr().setAttributeValue(dataSetEntity, EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN, sampleNamePattern);    
				}
				else {
                    EntityData patternEd = dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);
                    if (patternEd!=null) {
                        dataSetEntity.getEntityData().remove(patternEd);
                        ModelMgr.getModelMgr().removeEntityData(patternEd);
                    }
				}
				
				ModelMgr.getModelMgr().setAttributeValue(dataSetEntity, EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, getCheckboxValues(processCheckboxes));
				
				if (sageSyncCheckbox.isSelected()) {
					ModelMgr.getModelMgr().setAttributeAsTag(dataSetEntity, EntityConstants.ATTRIBUTE_SAGE_SYNC);
				}
				else {
					EntityData sageSyncEd = dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC);
					if (sageSyncEd!=null) {
					    dataSetEntity.getEntityData().remove(sageSyncEd);
					    ModelMgr.getModelMgr().removeEntityData(sageSyncEd);
					}
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
		};
		worker.execute();
    }

    private void addCheckboxes(final String title, final Object[] choices, final HashMap<String,JCheckBox> checkboxes, final JPanel panel) {
    	for(Object choice : choices) {
    		NamedEnum namedEnum = ((NamedEnum)choice);
    		JCheckBox checkBox = new JCheckBox(namedEnum.getName());
    		checkboxes.put(namedEnum.toString(), checkBox);
        	panel.add(checkBox, "gap para, span 2");
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

    	StringBuffer sb = new StringBuffer();
    	for(String key : checkboxes.keySet()) {
    		JCheckBox checkbox = checkboxes.get(key);
    		if (checkbox!=null && checkbox.isSelected()) {
        		if (sb.length()>0) sb.append(",");
        		sb.append(key);
    		}
    	}
    	
    	return sb.toString();
    }
    
    public boolean isAccessible() {
    	return true;
    }
}
