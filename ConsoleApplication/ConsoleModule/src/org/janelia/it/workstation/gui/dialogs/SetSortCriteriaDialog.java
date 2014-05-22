package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.workstation.gui.framework.access.Accessibility;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A dialog changing the Sort Criteria for an entity or set of entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSortCriteriaDialog extends ModalDialog implements Accessibility {
    
    /** An attribute must be present on this percentage of child entities in order to be considered a sortable field */
    private static final float PERCENT_PRESENT = 0.8f;
    
    private static final String[] intrinsicFields = {EntityConstants.VALUE_SC_GUID, EntityConstants.VALUE_SC_NAME, EntityConstants.VALUE_SC_DATE_CREATED, EntityConstants.VALUE_SC_DATE_UPDATED};
    private static final String DEFAULT_SORT_VALUE = "Choose field...";
    
    private JPanel attrPanel;
    private JComboBox sortingFieldCombobox;
    private JComboBox sortingOrderCombobox;
    private DefaultComboBoxModel sortingFieldModel;
    private DefaultComboBoxModel sortingOrderModel;
    private Entity entity;
    
    public SetSortCriteriaDialog() {
        
        setTitle("Set Sorting Criteria");
        
        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        sortingFieldCombobox = new JComboBox();
        sortingFieldCombobox.setEditable(false);
        sortingFieldCombobox.setToolTipText("Choose sorting field");

        sortingFieldModel = (DefaultComboBoxModel)sortingFieldCombobox.getModel();
        sortingFieldModel.addElement(DEFAULT_SORT_VALUE);
        for(String field : intrinsicFields) {
            sortingFieldModel.addElement(field);
        }

        sortingOrderCombobox = new JComboBox();
        sortingOrderCombobox.setEditable(false);
        sortingOrderCombobox.setToolTipText("Choose sort order");

        sortingOrderModel = (DefaultComboBoxModel)sortingOrderCombobox.getModel();
        sortingOrderModel.addElement(EntityConstants.VALUE_SC_SORT_ORDER_ASC);
        sortingOrderModel.addElement(EntityConstants.VALUE_SC_SORT_ORDER_DESC);
        sortingOrderModel.setSelectedItem(EntityConstants.VALUE_SC_SORT_ORDER_ASC);
        
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
    
    public void showForEntity(final Entity entity) {

        this.entity = entity;

        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            Utils.setWaitingCursor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame());
            try {
                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(entity, false);
            }
            catch (Exception e) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e);
            }
            Utils.setDefaultCursor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame());
        }

        // Find common attributes in child entities that the user can sort by
        Map<String,Integer> attrCounts = new HashMap<String,Integer>();
        for(Entity child : entity.getChildren()) {
            for(EntityData ed : child.getEntityData()) {
                if (ed.getChildEntity()==null) {
                    Integer count = attrCounts.get(ed.getEntityAttrName());
                    if (count==null) {
                        count = 1;
                    }
                    attrCounts.put(ed.getEntityAttrName(), count+1);
                }
            }
        }
        
        List<String> attrKeys = new ArrayList<String>(attrCounts.keySet());
        Collections.sort(attrKeys);
        
        int total = entity.getChildren().size();
        for(String attr : attrKeys) {
            int count = attrCounts.get(attr);
            if ((float)count/(float)total>PERCENT_PRESENT) {
                sortingFieldModel.addElement(attr);
            }
        }
        
        attrPanel.removeAll();

        attrPanel.add(new JLabel("Sort by: "), "");
        attrPanel.add(sortingFieldCombobox, "wrap");
        attrPanel.add(new JLabel("Sort order: "), "");
        attrPanel.add(sortingOrderCombobox, "wrap");
        
        String currCriteria = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SORT_CRITERIA);
        if (!StringUtils.isEmpty(currCriteria)) {
            String sortField = currCriteria.substring(1);
            String sortOrder = currCriteria.startsWith("-") ? EntityConstants.VALUE_SC_SORT_ORDER_DESC : EntityConstants.VALUE_SC_SORT_ORDER_ASC;
            sortingFieldModel.setSelectedItem(sortField);
            sortingOrderModel.setSelectedItem(sortOrder);
        }
        
        packAndShow();
    }
    
    private void saveAndClose() {
        
    	Utils.setWaitingCursor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame());
    	
    	final String sortField = (String)sortingFieldCombobox.getSelectedItem();
        final String sortOrder = (String)sortingOrderCombobox.getSelectedItem();
    	
        if (DEFAULT_SORT_VALUE.equals(sortField)) {
            JOptionPane.showMessageDialog(this, "No sort field selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
                
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
			    if (StringUtils.isEmpty(sortField)) {
			        EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SORT_CRITERIA);
			        if (ed!=null) {
			            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeEntityData(ed);
			        }
			    }
			    else {
			        String order = EntityConstants.VALUE_SC_SORT_ORDER_DESC.equals(sortOrder)?"-":"+";
	                String sortCriteria = order+sortField;
	                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().setOrUpdateValue(entity, EntityConstants.ATTRIBUTE_SORT_CRITERIA, sortCriteria);
			    }
			}
			
			@Override
			protected void hadSuccess() {
                Utils.setDefaultCursor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame());
			    org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getEntityOutline().refresh(true, true, null);
			}
			
			@Override
			protected void hadError(Throwable error) {
				org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
				Utils.setDefaultCursor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame());
			}
		};
		worker.execute();

        setVisible(false);
    }

	public boolean isAccessible() {
    	return true;
    }
}
