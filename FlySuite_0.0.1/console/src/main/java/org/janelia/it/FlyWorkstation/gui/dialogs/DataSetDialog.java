package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.*;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A dialog for viewing the list of accessible data sets, editing them, and adding new ones. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetDialog extends ModalDialog implements Accessibility {
    
	private DataSetListDialog parentDialog;
	
    private JPanel attrPanel;
    
    private JLabel nameLabel;
    private JLabel magnificationLabel;
    private JLabel opticalResLabel;
    private JLabel pipelineLabel;
    private JLabel mergeAlgorithmsLabel;
    private JLabel stitchAlgorithmsLabel;
    private JLabel alignmentAlgorithmsLabel;
    private JLabel analysisAlgorithmsLabel;
    
    private JTextField nameInput;
    private JComboBox magnificationInput;
    private JComboBox opticalResInput;
    private JComboBox pipelineInput;
    
    private HashMap<String,JCheckBox> mergeCheckboxes = new HashMap<String,JCheckBox>();
    private HashMap<String,JCheckBox> stitchCheckboxes = new HashMap<String,JCheckBox>();
    private HashMap<String,JCheckBox> alignmentCheckboxes = new HashMap<String,JCheckBox>();
    private HashMap<String,JCheckBox> analysisCheckboxes = new HashMap<String,JCheckBox>();
    
    private Entity dataSetEntity;
   
    public DataSetDialog(DataSetListDialog parentDialog) {

    	this.parentDialog = parentDialog;
    	
        setTitle("Data Set Definition");

    	add(Box.createHorizontalStrut(800), BorderLayout.NORTH);
    	
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Data Set Definition")));

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
    
    public void showForDataSet(final Entity dataSetEntity) {

    	this.dataSetEntity = dataSetEntity;

    	attrPanel.removeAll();
    	
        nameLabel = new JLabel("Data Set Name: ");
        nameInput = new JTextField();
        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel);
        attrPanel.add(nameInput);
        
        magnificationLabel = new JLabel("Magnification: ");
        magnificationInput = new JComboBox();
        magnificationInput.addItem(DataSetAttributes.MAGNIFICATION_20X);
        magnificationInput.addItem(DataSetAttributes.MAGNIFICATION_40X);
        magnificationInput.addItem(DataSetAttributes.MAGNIFICATION_63X);
        magnificationLabel.setLabelFor(magnificationInput);
        attrPanel.add(magnificationLabel);
        attrPanel.add(magnificationInput);

        opticalResLabel = new JLabel("Optical Resolution: ");
        opticalResInput = new JComboBox();
        opticalResInput.setEditable(true);
        opticalResInput.addItem(DataSetAttributes.OPTICAL_RESOLUTION_62x62x1);
        opticalResInput.addItem(DataSetAttributes.OPTICAL_RESOLUTION_52x52x1);
        opticalResInput.addItem(DataSetAttributes.OPTICAL_RESOLUTION_3x3x58);
        opticalResInput.addItem(DataSetAttributes.OPTICAL_RESOLUTION_3x3x38);
        opticalResInput.addItem(DataSetAttributes.OPTICAL_RESOLUTION_19x19x38);
        opticalResLabel.setLabelFor(opticalResInput);
        attrPanel.add(opticalResLabel);
        attrPanel.add(opticalResInput);

        pipelineLabel = new JLabel("Pipeline Template: ");
        pipelineInput = new JComboBox();
        pipelineInput.setEditable(true);
        pipelineInput.addItem(DataSetAttributes.PIPELINE_FLYLIGHT);
        pipelineInput.addItem(DataSetAttributes.PIPELINE_LEET);
        pipelineLabel.setLabelFor(pipelineInput);
        attrPanel.add(pipelineLabel);
        attrPanel.add(pipelineInput);

        mergeAlgorithmsLabel = new JLabel("Merge Algorithms: ");
        JPanel mergePanel = createCheckboxes("Merge Algorithm", MergeAlgorithm.values(), mergeCheckboxes);
        mergeAlgorithmsLabel.setLabelFor(mergePanel);
        attrPanel.add(mergeAlgorithmsLabel);
        attrPanel.add(mergePanel);
        
        stitchAlgorithmsLabel = new JLabel("Stitching Algorithms: ");
        JPanel stitchPanel = createCheckboxes("Stitch Algorithm", StitchAlgorithm.values(), stitchCheckboxes);
        stitchAlgorithmsLabel.setLabelFor(stitchPanel);
        attrPanel.add(stitchAlgorithmsLabel);
        attrPanel.add(stitchPanel);

        alignmentAlgorithmsLabel = new JLabel("Alignment Algorithms: ");
        JPanel alignmentPanel = createCheckboxes("Alignment Algorithm", AlignmentAlgorithm.values(), alignmentCheckboxes);
        alignmentAlgorithmsLabel.setLabelFor(alignmentPanel);
        attrPanel.add(alignmentAlgorithmsLabel);
        attrPanel.add(alignmentPanel);

        analysisAlgorithmsLabel = new JLabel("Analysis Algorithms: ");
        JPanel analysisPanel = createCheckboxes("Analysis Algorithm", AnalysisAlgorithm.values(), analysisCheckboxes);
        mergeAlgorithmsLabel.setLabelFor(analysisPanel);
        attrPanel.add(analysisAlgorithmsLabel);
        attrPanel.add(analysisPanel);
        
        if (dataSetEntity!=null) {
        	nameInput.setText(dataSetEntity.getName());
            magnificationInput.setSelectedItem(dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MAGNIFICATION));
            opticalResInput.setSelectedItem(dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
            pipelineInput.setSelectedItem(dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
            
            applyCheckboxValues(mergeCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MERGE_ALGORITHMS));
            applyCheckboxValues(stitchCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STITCH_ALGORITHMS));
            applyCheckboxValues(alignmentCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_ALGORITHMS));
            applyCheckboxValues(analysisCheckboxes, dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANALYSIS_ALGORITHMS));
        }
        else {
        	nameInput.setText("");
            magnificationInput.setSelectedIndex(0);
            opticalResInput.setSelectedIndex(0);
            pipelineInput.setSelectedIndex(0);
            applyCheckboxValues(mergeCheckboxes, MergeAlgorithm.FLYLIGHT.toString());
            applyCheckboxValues(stitchCheckboxes, StitchAlgorithm.FLYLIGHT.toString());
        }
        
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
        
        packAndShow();
    }
    
    private void saveAndClose() {
    	
    	Utils.setWaitingCursor(DataSetDialog.this);
    	
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				if (dataSetEntity==null) {
					dataSetEntity = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_DATA_SET, nameInput.getText());
				}
				
				dataSetEntity.setName(nameInput.getText());
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_MAGNIFICATION, magnificationInput.getSelectedItem().toString());
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalResInput.getSelectedItem().toString());
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, pipelineInput.getSelectedItem().toString());
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_MERGE_ALGORITHMS, getCheckboxValues(mergeCheckboxes));
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_STITCH_ALGORITHMS, getCheckboxValues(stitchCheckboxes));
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_ALGORITHMS, getCheckboxValues(alignmentCheckboxes));
				dataSetEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_ANALYSIS_ALGORITHMS, getCheckboxValues(analysisCheckboxes));
				
				ModelMgr.getModelMgr().saveOrUpdateEntity(dataSetEntity);
			}
			
			@Override
			protected void hadSuccess() {	
				parentDialog.reloadData();
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

    private JPanel createCheckboxes(final String title, final Object[] choices, final HashMap<String,JCheckBox> checkboxes) {

        
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    	
    	for(Object choice : choices) {
    		NamedEnum namedEnum = ((NamedEnum)choice);
    		JCheckBox checkBox = new JCheckBox(namedEnum.getName());
    		checkboxes.put(namedEnum.toString(), checkBox);
        	panel.add(checkBox);
    	}
    	
    	return panel;
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
