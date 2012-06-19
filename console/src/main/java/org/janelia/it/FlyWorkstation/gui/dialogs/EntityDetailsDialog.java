package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationTablePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationView;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsDialog extends ModalDialog {
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private static final String COLUMN_KEY = "Attribute Name";
    private static final String COLUMN_VALUE = "Attribute Value";
    
    private JPanel attrPanel;
    private JLabel roleLabel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel ownerLabel;
    private JLabel creationDateLabel;
    private JLabel updatedDateLabel;
    private JSplitPane splitPane;
    private JPanel attributePanel;
    private DynamicTable dynamicTable;
    
    private JPanel annotationPanel;
    private AnnotationView annotationView;
    private RootedEntity rootedEntity;
    private ModelMgrAdapter modelMgrAdapter;

    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public EntityDetailsDialog() {

        setTitle("Entity Details");
        setModalityType(ModalityType.TOOLKIT_MODAL);
        
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Properties")));

        nameLabel = addAttribute("Name: ");
        typeLabel = addAttribute("Type: ");
        roleLabel = addAttribute("Role: ");
        ownerLabel = addAttribute("Annotation Owner: ");
        creationDateLabel = addAttribute("Creation Date: ");
        updatedDateLabel = addAttribute("Updated Date: ");
        
        add(attrPanel, BorderLayout.NORTH);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        attributePanel = new JPanel();
        attributePanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createCompoundBorder(
    							BorderFactory.createEmptyBorder(10, 10, 0, 10), 
    							BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Attributes")),
        		BorderFactory.createEmptyBorder(0, 0, 10, 0)));
        
        dynamicTable = new DynamicTable(true, true) {

            @Override
			public Object getValue(Object userObject, DynamicColumn column) {

            	EntityData entityData = (EntityData)userObject;
                if (null!=entityData) {
                    if (column.getName().equals(COLUMN_KEY)) {
                        return entityData.getEntityAttribute().getName();
                    }
                    if (column.getName().equals(COLUMN_VALUE)) {
                        return entityData.getValue();
                    }
                }
                return null;
			}
        };
        
        DynamicColumn keyCol = dynamicTable.addColumn(COLUMN_KEY, COLUMN_KEY, true, false, false, true);
        DynamicColumn valueCol = dynamicTable.addColumn(COLUMN_VALUE, COLUMN_VALUE, true, false, false, true);
        attributePanel.add(dynamicTable, BorderLayout.CENTER);
        
        annotationPanel = new JPanel();
        annotationPanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createCompoundBorder(
    							BorderFactory.createEmptyBorder(10, 10, 0, 10), 
    							BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Annotations")),
        		BorderFactory.createEmptyBorder(0, 0, 10, 0)));
        annotationView = new AnnotationTablePanel();
        annotationPanel.add((JPanel)annotationView, BorderLayout.CENTER);
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, attributePanel, annotationPanel);
        add(splitPane, BorderLayout.CENTER);
        
        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);

        modelMgrAdapter = new ModelMgrAdapter() {
			@Override
			public void annotationsChanged(final long entityId) {
				if (entityId != rootedEntity.getEntity().getId()) return; 
				reloadAnnotation();
			}
		};
    }

    public void showForRootedEntity(RootedEntity rootedEntity) {
    	this.rootedEntity = rootedEntity;
    	EntityData entityData = rootedEntity.getEntityData();
    	if (entityData != null) {
    		roleLabel.setText(entityData.getEntityAttribute()==null?"":entityData.getEntityAttribute().getName());
    	}
    	showForEntity(rootedEntity.getEntity());
    }
    
    public void showForEntity(final Entity entity) {

    	nameLabel.setText(entity.getName());
    	typeLabel.setText(entity.getEntityType().getName());
        ownerLabel.setText(entity.getUser().getUserLogin());
        creationDateLabel.setText(df.format(entity.getCreationDate()));
        updatedDateLabel.setText(df.format(entity.getUpdatedDate()));

        // Update the attribute table
        dynamicTable.removeAllRows();
        for (EntityData entityData : entity.getOrderedEntityData()) {
        	if (entityData.getChildEntity()==null) {
        		dynamicTable.addRow(entityData);
        	}
        }       
        dynamicTable.updateTableModel();

        // Update annotation (async load)
        reloadAnnotation();
        
        // Register this dialog as a model observer
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrAdapter);
        
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrAdapter);
    }
    
    private void reloadAnnotation() {

    	annotationView.setAnnotations(null);
    	
        SimpleWorker annotationLoadingWorker = new SimpleWorker() {

        	private List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();
        	
			@Override
			protected void doStuff() throws Exception {
	            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(rootedEntity.getEntity().getId())) {
	            	OntologyAnnotation annotation = new OntologyAnnotation();
	            	annotation.init(entityAnnot);
	                if(null!=annotation.getTargetEntityId())
	            	    annotations.add(annotation);
	            }
			}
			
			@Override
			protected void hadSuccess() {
				annotationView.setAnnotations(annotations);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		annotationLoadingWorker.execute();
    }
}
