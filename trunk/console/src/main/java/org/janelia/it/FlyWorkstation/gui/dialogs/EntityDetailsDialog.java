package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationTablePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationView;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrResults;
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
    
    private JLabel loadingLabel;
    private JLabel loadingLabel2;
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
    private ModelMgrAdapter modelMgrAdapter;

    private Entity entity;
   
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
        setModalityType(ModalityType.MODELESS);
        
        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        loadingLabel2 = new JLabel();
        loadingLabel2.setOpaque(false);
        loadingLabel2.setIcon(Icons.getLoadingIcon());
        loadingLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel2.setVerticalAlignment(SwingConstants.CENTER);
        
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        nameLabel = addAttribute("Name: ");
        typeLabel = addAttribute("Type: ");
        roleLabel = addAttribute("Role: ");
        ownerLabel = addAttribute("Owner: ");
        creationDateLabel = addAttribute("Creation Date: ");
        updatedDateLabel = addAttribute("Updated Date: ");
        
        add(attrPanel, BorderLayout.NORTH);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        attributePanel = new JPanel(new BorderLayout());
        attributePanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createCompoundBorder(
    							BorderFactory.createEmptyBorder(10, 10, 0, 10), 
    							BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Attributes")),
        		BorderFactory.createEmptyBorder(0, 0, 10, 0)));
    	attributePanel.add(loadingLabel, BorderLayout.CENTER);
        
        dynamicTable = new DynamicTable(true, true) {
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {
            	AttributeValue attrValue = (AttributeValue)userObject;
                if (null!=attrValue) {
                    if (column.getName().equals(COLUMN_KEY)) {
                        return attrValue.getName();
                    }
                    if (column.getName().equals(COLUMN_VALUE)) {
                        return attrValue.getValue();
                    }
                }
                return null;
			}
        };
        
        DynamicColumn keyCol = dynamicTable.addColumn(COLUMN_KEY, COLUMN_KEY, true, false, false, true);
        DynamicColumn valueCol = dynamicTable.addColumn(COLUMN_VALUE, COLUMN_VALUE, true, false, false, true);
        
        annotationPanel = new JPanel(new BorderLayout());
        annotationPanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createCompoundBorder(
    							BorderFactory.createEmptyBorder(10, 10, 0, 10), 
    							BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Annotations")),
        		BorderFactory.createEmptyBorder(0, 0, 10, 0)));
        annotationPanel.add(loadingLabel2, BorderLayout.CENTER);
    	
        annotationView = new AnnotationTablePanel();
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, attributePanel, annotationPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(splitPane, BorderLayout.CENTER);
        
        addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				// Has to happen after the pane is visible
		        splitPane.setDividerLocation(0.5);
			}
		});
        
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
				if (entityId != entity.getId()) return; 
				reloadAnnotation();
			}
		};
    }

    public void showForRootedEntity(RootedEntity rootedEntity) {
    	EntityData entityData = rootedEntity.getEntityData();
    	if (entityData != null) {
    		roleLabel.setText(entityData.getEntityAttribute()==null?"":entityData.getEntityAttribute().getName());
    	}
    	showForEntity(rootedEntity.getEntity());
    }
    
    public void showForEntity(final Entity entity) {

    	this.entity = entity;
    	
    	nameLabel.setText(entity.getName());
    	typeLabel.setText(entity.getEntityType().getName());
        ownerLabel.setText(entity.getUser().getUserLogin());
        creationDateLabel.setText(df.format(entity.getCreationDate()));
        updatedDateLabel.setText(df.format(entity.getUpdatedDate()));

		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.8),(int)(browser.getHeight()*0.8)));
		
        loadData(entity.getId());
		reloadAnnotation();

        // Register this dialog as a model observer
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrAdapter);
        
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrAdapter);
    }
    
    private void loadData(final long entityId) {

    	attributePanel.removeAll();
    	attributePanel.add(loadingLabel, BorderLayout.CENTER);
    	final SearchConfiguration searchConfig = SessionMgr.getBrowser().getGeneralSearchConfig();
    	
        SimpleWorker worker = new SimpleWorker() {

        	private Entity entity;
        	private EntityDocument doc;
        	
			@Override
			protected void doStuff() throws Exception {

	            SolrQuery query = new SolrQuery("id:"+entityId);
	            SolrResults results = ModelMgr.getModelMgr().searchSolr(query);
	            this.doc = results.getEntityDocuments().isEmpty() ? null : results.getEntityDocuments().iterator().next();
	            if (doc==null) {
	            	this.entity = ModelMgr.getModelMgr().getEntityById(entityId);
	            }
	            else {
	            	this.entity = doc.getEntity();	
	            }
			}
			
			@Override
			protected void hadSuccess() {

		        // Update the attribute table
		        dynamicTable.removeAllRows();
		        for (EntityData entityData : entity.getOrderedEntityData()) {
		        	if (entityData.getChildEntity()==null) {
		        		AttributeValue attrValue = new AttributeValue(entityData.getEntityAttribute().getName(), entityData.getValue());
		        		dynamicTable.addRow(attrValue);
		        	}
		        }       
		        
		        if (doc!=null) {
		        	List<SearchAttribute> attrs = searchConfig.getAttributeGroups().get(AttrGroup.SAGE);
	            	for(SearchAttribute attr : attrs) {
	            		String value = searchConfig.getValue(doc, attr.getName());
	            		if (value!=null) {
			        		AttributeValue attrValue = new AttributeValue(attr.getLabel(), value);
			        		dynamicTable.addRow(attrValue);
	            		}
	            	}
	            }
		        
		        dynamicTable.updateTableModel();
		        attributePanel.removeAll();
		        attributePanel.add(dynamicTable, BorderLayout.CENTER);
		        attributePanel.revalidate();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				attributePanel.removeAll();
		        attributePanel.add(dynamicTable, BorderLayout.CENTER);
		        attributePanel.revalidate();
			}
		};
		worker.execute();
    }
    
    private void reloadAnnotation() {

    	annotationView.setAnnotations(null);
    	annotationPanel.removeAll();
    	annotationPanel.add(loadingLabel2, BorderLayout.CENTER);
    	
        SimpleWorker annotationLoadingWorker = new SimpleWorker() {

        	private List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();
        	
			@Override
			protected void doStuff() throws Exception {
	            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(entity.getId())) {
	            	OntologyAnnotation annotation = new OntologyAnnotation();
	            	annotation.init(entityAnnot);
	                if(null!=annotation.getTargetEntityId())
	            	    annotations.add(annotation);
	            }
			}
			
			@Override
			protected void hadSuccess() {
				annotationView.setAnnotations(annotations);
				annotationPanel.removeAll();
				annotationPanel.add((JPanel)annotationView, BorderLayout.CENTER);
				annotationPanel.revalidate();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				annotationPanel.removeAll();
				annotationPanel.add((JPanel)annotationView, BorderLayout.CENTER);
				annotationPanel.revalidate();
			}
		};
		annotationLoadingWorker.execute();
    }
    
    private class AttributeValue {
    	private String name;
    	private String value;
		public AttributeValue(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
    }
}
