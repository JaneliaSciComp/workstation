package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JOptionPane;
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
import org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Widget to help with editing the common paths used by all of LVV userdom.
 * @author Leslie L Foster
 * @todo pre-check paths on remote server prior to adding them.
 */
public class PathCollectionEditor extends JPanel {
    private static final int VALUE_COL = 0;
	public static final int PCE_HEIGHT = 500;
	public static final int PCE_WIDTH = 800;
    public static final String SETTINGS_ENTITY_NAME = "settings";
    
    private JTable table;

	private static Logger log = LoggerFactory.getLogger(PathCollectionEditor.class);
	
    /**
     * Takes entity which should have a property by the name given, and uses
     * that to lookup its collection of paths.  The paths are expected to be
     * as Property on the PropertySet entity under the given entity.  The
     * Property should have format propName=Path1\nPath2\nPath3\n...
     * 
     * @param entity should have a PropertySet sub entity.
     * @param propName a Property entityData on the PropertySet subentity has a value beginning {propName}=.
     * @param completionListener allows the 'Done' button to signal back to some containing dialog/frame.
     */
	public PathCollectionEditor(final Entity entity, final String propName, final CompletionListener completionListener) {
		super();
		this.setSize(PCE_WIDTH, PCE_HEIGHT);
		table = new JTable();
    
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    table.setModel(new EntityDataModel(propName, entity));
        JScrollPane scrollPane = new JScrollPane(table);
        
        JTextField textField = new JTextField();
        textField.setBorder(new TitledBorder("New Path"));
        textField.addKeyListener(new PathCorrectionKeyListener( textField ));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new AddValueListener(propName, table, textField, entity, completionListener));
        
        JButton doneButton = null;

        if (completionListener != null) {
            doneButton = new JButton("Done");
            //e -> listener.done()
            doneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    completionListener.done();
                }
            });
        }
        
        JButton delButton = new JButton("Delete");
        delButton.addActionListener(new DeleteValueListener(propName, table, entity, completionListener));

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(textField, BorderLayout.NORTH);
        JPanel buttonPanel = layoutButtonPanel(addButton, delButton, doneButton);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(controlPanel, BorderLayout.SOUTH);
	}

    private JPanel layoutButtonPanel(JButton addButton, JButton delButton, JButton doneButton) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets( 0, 0, 0, 30 );
        buttonPanel.add(addButton, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets( 0, 15, 0, 15 );
        buttonPanel.add(delButton, gbc);
        if (doneButton != null) {
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(0, 30, 0, 0);
            buttonPanel.add(doneButton, gbc);
        }
        return buttonPanel;
    }

    private static boolean ensureFullyLoaded(Entity entity) {
        try {
            ModelMgr.getModelMgr().loadLazyEntity(entity, true);
        } catch (Exception ex) {
            ModelMgr.getModelMgr().handleException(ex);
            return false;
        }
        return true;
    }
    
    private static ValueBean findOldValue(Entity entity, String propName) {
        if (! ensureFullyLoaded(entity)) {
            return null;
        }
        ValueBean oldValueBean = new ValueBean();
        // Now find old value in db.
        Set<Entity> children = entity.getChildren();
        for (Entity child : children) {
            if (child.getEntityTypeName() == null  ||  child.getName() == null) {
                continue;
            }
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)
                    && child.getName().equals(SETTINGS_ENTITY_NAME)) {

                oldValueBean.targetEntity = child;
                // Now, to find the Name/Value pair with our values.
                for (EntityData ed : child.getEntityData()) {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                        String nameValue = ed.getValue();
                        String[] nameValueArr = nameValue.split("=");
                        if (nameValueArr.length == 2 && nameValueArr[0].trim().equals(propName)) {
                            oldValueBean.targetEntityData = ed;
                            oldValueBean.oldValue = nameValue;
                            break;
                        }
                    }
                }
            }
        }
        return oldValueBean;
    }
    
    private static EntityData updateValue(ValueBean valueBean, String updatedValueString, Entity entity) {
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
            //valueBean.targetEntityData = ModelMgr.getModelMgr().saveOrUpdateEntityData(valueBean.targetEntityData);
            valueBean.targetEntity.getEntityData().add(valueBean.targetEntityData);
            ModelMgr.getModelMgr().saveOrUpdateEntity(valueBean.targetEntity);
            return valueBean.targetEntityData;
        } catch (Exception ex) {
            ModelMgr.getModelMgr().handleException(ex);
            return null;
        }
    }

    private static void updateValue(ValueBean valueBean, String updatedValue, CompletionListener completionListener, Entity entity, JTable table) {
        EntityData rtnVal = updateValue(valueBean, updatedValue, entity);
        if (rtnVal != null) {
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        } else if (completionListener != null) {
            // Break out on exception.
            completionListener.done();
        }
    }

    private static class DeleteValueListener implements ActionListener {

        private JTable table;
        private String propName;
        private Entity entity;
        private CompletionListener completionListener;

        public DeleteValueListener(String propName, JTable table, Entity entity, CompletionListener completionListener) {
            this.table = table;
            this.propName = propName;
            this.entity = entity;
            this.completionListener = completionListener;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (table.getSelectedRow() > -1) {
                SimpleWorker delTask = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        Object objToDelete = table.getModel().getValueAt(table.getSelectedRow(), VALUE_COL);
                        if (objToDelete != null) {
                            String valueToDelete = objToDelete.toString();
                            log.info("Deleting {} {}", propName, valueToDelete);
                            StringBuilder builder = new StringBuilder(propName).append('=');
                            int appendCount = 0;
                            for (int i = 0; i < table.getModel().getRowCount(); i++) {
                                if (i != table.getSelectedRow()) {
                                    if (appendCount > 0) {
                                        builder.append("\n");
                                    }
                                    builder.append(table.getModel().getValueAt(i, VALUE_COL));
                                    appendCount++;
                                }
                            }
                            String updatedValue = builder.toString();
                            ValueBean valueBean = findOldValue(entity, propName);
                            updateValue(valueBean, updatedValue, completionListener, entity, table);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        // Nothing
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ModelMgr.getModelMgr().handleException(error);
                    }
                    
                };

                delTask.execute();
            }
        }
    }

    private static class AddValueListener implements ActionListener {

        private Entity entity;
        private String propName;
        private JTable table;
        private JTextField inputField;
        private CompletionListener completionListener;

        public AddValueListener(String propName, JTable table, JTextField inputField, Entity entity, CompletionListener completionListener) {
            this.propName = propName;
            this.entity = entity;
            this.table = table;
            this.inputField = inputField;
            this.completionListener = completionListener;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SimpleWorker updateTask = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    // First, check the input.
                    // Wish to check if this one exists on server.
                    final String inputValue = inputField.getText().trim();
                    final ComputeFacade cf = FacadeManager.getFacadeManager().getComputeFacade();
                    if (cf.isServerPathAvailable(inputValue, true)) {
                        // Now create an updated value.
                        ValueBean valueBean = findOldValue(entity, propName);

                        String updatedValueString = null;
                        if (valueBean == null || valueBean.oldValue == null) {
                            // First entry.
                            updatedValueString = propName + "=" + inputValue;
                        } else {
                            // Subsequent entries.
                            updatedValueString = valueBean.oldValue + '\n' + inputValue;
                        }

                        // Now update the set of values.
                        updateValue(valueBean, updatedValueString, completionListener, entity, table);
                        inputField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(table, "Directory " + inputValue + " not found on server.  Please check.");
                    }

                }

                @Override
                protected void hadSuccess() {
                    // Nothing to do.
                }

                @Override
                protected void hadError(Throwable error) {
                    // Not much to do.
                    ModelMgr.getModelMgr().handleException(error);
                }
                
            };
            updateTask.execute();
        }

    }

    /**
     * Encapsulates and adapts the collection of paths to be modified.
     */
	private static class EntityDataModel extends AbstractTableModel {
        private static final String[] COL_NAMES = new String[] {"Path"};
		private Entity entity;
        private List<String> values = new ArrayList<>();
        private String propName;
		public EntityDataModel(String propName, Entity entity) {
			this.entity = entity;
            this.propName = propName;
            update();
		}

        @Override
        public String getColumnName(int colNum) {
            if (colNum <= COL_NAMES.length) {
                return COL_NAMES[colNum];
            }
            else {
                return "-Unknown-";
            }
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
                    return values.get(rowIndex);
                }
                default : {
                    return null;
                }
            }
        }
        
        private void update() {
            Set<String> valuesSet = new HashSet<>();
            ValueBean valueBean = findOldValue(entity, propName);
            if (valueBean != null && valueBean.oldValue != null) {
                String nameValue = valueBean.oldValue;
                String[] nameValueArr = nameValue.split("=");
                if (nameValueArr.length == 2 && nameValueArr[0].trim().equals(propName)) {
                    String[] allValues = nameValueArr[1].split("\n");
                    log.info("Found " + allValues.length + " paths.");
                    for (String aValue : allValues) {
                        valuesSet.add(aValue);
                    }
                } else {
                    log.warn("Invalid name/value string in property attribute. " + nameValue);
                }
            }
            this.values.clear();
            this.values.addAll(valuesSet);
        }

        @Override
        public void fireTableDataChanged() {
            update();
            super.fireTableDataChanged();
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
