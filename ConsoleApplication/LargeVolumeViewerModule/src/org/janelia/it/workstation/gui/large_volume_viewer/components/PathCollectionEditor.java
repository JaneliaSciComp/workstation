package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Widget to help with editing the common paths used by all of LVV userdom.
 * @author Leslie L Foster
 */
public class PathCollectionEditor extends JPanel {
    private static final int VALUE_COL = 0;
	public static final int PCE_HEIGHT = 500;
	public static final int PCE_WIDTH = 800;
    public static final String SETTINGS_ENTITY_NAME = "settings";

	private static Logger log = LoggerFactory.getLogger(PathCollectionEditor.class);
	
	public PathCollectionEditor(final Entity entity, final String propName, final CompletionListener completionListener) {
		super();
		this.setSize(PCE_WIDTH, PCE_HEIGHT);
		final JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    table.setModel(new EntityDataModel(propName, entity));
        JScrollPane scrollPane = new JScrollPane(table);
        
        JTextField textField = new JTextField();
        textField.setBorder(new TitledBorder("New Path"));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new AddValueListener(propName, textField, entity));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        if (completionListener != null) {
            JButton doneButton = new JButton("Done");
            //e -> listener.done()
            doneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    completionListener.done();
                }
            });
            buttonPanel.add(doneButton, BorderLayout.EAST);
        }
        
        JButton delButton = new JButton("Delete");
        delButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (table.getSelectedRow() > -1) {
                    Object objToDelete = table.getModel().getValueAt(table.getSelectedRow(), VALUE_COL);
                    if (objToDelete != null) {
                        String valueToDelete = objToDelete.toString();
                        log.info("Deleting {} {}", propName, valueToDelete);
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < table.getModel().getRowCount(); i++) {
                            if (i != table.getSelectedRow()) {
                                if (i > 0) {
                                    builder.append("\n");
                                }
                                builder.append(table.getModel().getValueAt(i, VALUE_COL));                                
                            }
                        }
                        String updatedValue = builder.toString();
                        ValueBean valueBean = findOldValue(entity, propName);
                        updateValue(valueBean, updatedValue, entity);
                    }
                }
            }
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(textField, BorderLayout.NORTH);
        buttonPanel.add(addButton, BorderLayout.WEST);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(controlPanel, BorderLayout.SOUTH);
	}
    
    private static class AddValueListener implements ActionListener {
        private Entity entity;
        private String propName;
        private JTextField inputField;
        
        public AddValueListener(String propName, JTextField inputField, Entity entity) {
            this.propName = propName;
            this.entity = entity;
            this.inputField = inputField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // First, check the input.
            //todo, see LoadedWorkspaceCreator which has a pathTextField.
            //  The pathTextField needs to become its own separate widget.
            //  then, it can be passed into here, as a self-validating
            //  widget.  It can be added to this panel, along with its
            //  descriptive border.
            final String inputValue = inputField.getText().trim();
            
            // Now create an updated value.
            ValueBean oldValue = findOldValue(entity, propName);
            
            String updatedValueString = null;
            if (oldValue == null) {
                // First entry.
                updatedValueString = propName + "=" + inputValue;
            }
            else {
                updatedValueString = oldValue.oldValue;                
            }

            // Now update the set of values. Since we
            // append, we don't mind the equal sign.
            if (updatedValueString.length() > 0) {
                updatedValueString += '\n';
            }
            updatedValueString += inputValue;
            updateValue(oldValue, updatedValueString, entity);
        }

    }
	
    private static ValueBean findOldValue(Entity entity, String propName) {
        ValueBean oldValueBean = new ValueBean();
        // Now find old value in db.
        Set<Entity> children = entity.getChildren();
        for (Entity child : children) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)
                    && child.getName().equals(SETTINGS_ENTITY_NAME)) {

                oldValueBean.targetEntity = child;
                // Now, to find the Name/Value pair with our values.
                for (EntityData ed : child.getEntityData()) {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                        String nameValue = ed.getValue();
                        String[] nameValueArr = nameValue.split("=");
                        if (nameValueArr.length == 2 && nameValueArr[0].trim() == propName) {
                            oldValueBean.targetEntityData = ed;
                            oldValueBean.oldValue = nameValue;
                        }
                    }
                }
            }
        }
        return oldValueBean;
    }
    
    private static void updateValue(ValueBean valueBean, String updatedValueString, Entity entity) {
        if (valueBean == null) {
            valueBean = new ValueBean();
        }
        if (valueBean.targetEntity == null ) {
            // Need to create the entity.
            try {
                Entity targetEntity = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_PROPERTY_SET, SETTINGS_ENTITY_NAME);
                targetEntity.setOwnerKey(entity.getOwnerKey());
                entity.addChildEntity(targetEntity);
                ModelMgr.getModelMgr().saveOrUpdateEntity(entity);

                valueBean.targetEntity = targetEntity;
            } catch (Exception ex) {
                ModelMgr.getModelMgr().handleException(ex);
            }
        }
        if (valueBean.targetEntityData == null) {
            EntityData propertyEntityData = new EntityData();
            propertyEntityData.setOwnerKey(entity.getOwnerKey());
            propertyEntityData.setCreationDate(new Date());
            propertyEntityData.setParentEntity(valueBean.targetEntity);
            propertyEntityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROPERTY);

            valueBean.targetEntityData = propertyEntityData;
        }
        try {
            valueBean.targetEntityData.setValue(updatedValueString); 
            ModelMgr.getModelMgr().saveOrUpdateEntityData(valueBean.targetEntityData);
        } catch (Exception ex) {
            ModelMgr.getModelMgr().handleException(ex);
        }
    }

    /**
     * Encapsulates and adapts the collection of paths to be modified.
     */
	private static class EntityDataModel extends AbstractTableModel {
		private Entity entity;
        private List<String> values = new ArrayList<>();
		public EntityDataModel(String propName, Entity entity) {
			this.entity = entity;
			Set<String> values = new HashSet<>();
            ValueBean valueBean = findOldValue(entity, propName);
            if (valueBean != null  &&  valueBean.oldValue != null) {
                String nameValue = valueBean.oldValue;
                String[] nameValueArr = nameValue.split("=");
                if (nameValueArr.length == 2  &&  nameValueArr[0].trim().equals(propName)) {
                    String[] allValues = nameValueArr[1].split("\n");
                    for (String aValue : allValues) {
                        values.add(aValue);
                    }
                } else {
                    log.warn("Invalid name/value string in property attribute. " + nameValue);
                }
            }
            this.values.addAll( values );
		}

        @Override
        public int getRowCount() {
            return values.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case VALUE_COL: {
                    return values.get(columnIndex);
                }
                default : {
                    return null;
                }
            }
        }
	}
    
    public static interface CompletionListener {
        void done();
    }
    
    private static class ValueBean {
        public String oldValue;
        public Entity targetEntity;
        public EntityData targetEntityData;
    }
}
