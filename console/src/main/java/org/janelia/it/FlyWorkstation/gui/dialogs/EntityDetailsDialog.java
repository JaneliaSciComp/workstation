package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationTablePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationView;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.user_data.Subject;

import com.google.common.collect.ComparisonChain;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsDialog extends ModalDialog implements Accessibility, Refreshable {
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private static final String ATTRIBUTES_COLUMN_KEY = "Attribute Name";
    private static final String ATTRIBUTES_COLUMN_VALUE = "Attribute Value";
    
    private static final String PERMISSIONS_COLUMN_SUBJECT = "Subject";
    private static final String PERMISSIONS_COLUMN_TYPE = "Type";
    private static final String PERMISSIONS_COLUMN_PERMS = "Permissions";
    
    private static final String OWNER_PERMISSION = "owner";
    
    private JTabbedPane tabbedPane;
    
    private JLabel attributesLoadingLabel;
    private JPanel attributesPanel;
    private DynamicTable attributesTable;
    
    private JLabel permissionsLoadingLabel;
    private JPanel permissionsPanel;
    private DynamicTable permissionsTable;
    private JPanel permissionsButtonPane;
    private JButton addPermissionButton;
    
    private JLabel annotationsLoadingLabel;
    private JPanel annotationsPanel;
    private AnnotationView annotationsView;
    
    private EntityActorPermissionDialog eapDialog;
    
    private ModelMgrAdapter modelMgrAdapter;
    private List<Subject> subjects;
    private Entity entity;
    private String role;
    
    public EntityDetailsDialog() {

        setModalityType(ModalityType.MODELESS);

        modelMgrAdapter = new ModelMgrAdapter() {
			@Override
			public void annotationsChanged(final long entityId) {
				if (entityId != entity.getId()) return; 
				loadAnnotations();
			}
		};
		
		// Child dialogs
		
		eapDialog = new EntityActorPermissionDialog(this);
		
		// Tabbed pane
		
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        
        // Attributes tab
        
        attributesLoadingLabel = new JLabel();
        attributesLoadingLabel.setOpaque(false);
        attributesLoadingLabel.setIcon(Icons.getLoadingIcon());
        attributesLoadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        attributesLoadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        attributesPanel = new JPanel(new BorderLayout());
    	attributesPanel.add(attributesLoadingLabel, BorderLayout.CENTER);
    	
        attributesTable = new DynamicTable(true, false) {
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {
            	AttributeValue attrValue = (AttributeValue)userObject;
                if (null!=attrValue) {
                    if (column.getName().equals(ATTRIBUTES_COLUMN_KEY)) {
                        return attrValue.getName();
                    }
                    else if (column.getName().equals(ATTRIBUTES_COLUMN_VALUE)) {
                        return attrValue.getValue();
                    }
                }
                return null;
			}
        };
        attributesTable.addColumn(ATTRIBUTES_COLUMN_KEY, ATTRIBUTES_COLUMN_KEY, true, false, false, true);
        attributesTable.addColumn(ATTRIBUTES_COLUMN_VALUE, ATTRIBUTES_COLUMN_VALUE, true, false, false, true);

        tabbedPane.addTab("Attributes", Icons.getIcon("table.png"), attributesPanel, "The data entity's attributes");
        
        
        // Permissions tab

        permissionsLoadingLabel = new JLabel();
        permissionsLoadingLabel.setOpaque(false);
        permissionsLoadingLabel.setIcon(Icons.getLoadingIcon());
        permissionsLoadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        permissionsLoadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        permissionsPanel = new JPanel(new BorderLayout());
        permissionsPanel.add(permissionsLoadingLabel, BorderLayout.CENTER);
    	
        permissionsTable = new DynamicTable(true, false) {
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {
            	EntityActorPermission eap = (EntityActorPermission)userObject;
                if (null!=eap) {
                    if (column.getName().equals(PERMISSIONS_COLUMN_SUBJECT)) {
                        return eap.getSubjectKey().split(":")[1];
                    }
                    else if (column.getName().equals(PERMISSIONS_COLUMN_TYPE)) {
                    	if (OWNER_PERMISSION.equals(eap.getPermissions())) {
                    		return OWNER_PERMISSION;
                    	}
                    	return eap.getSubjectKey().split(":")[0];
                    }
                    else if (column.getName().equals(PERMISSIONS_COLUMN_PERMS)) {
                    	if (OWNER_PERMISSION.equals(eap.getPermissions())) {
                    		return "rw";
                    	}
                        return eap.getPermissions();
                    }
                }
                return null;
			}

        	@Override
        	protected JPopupMenu createPopupMenu(MouseEvent e) {
        		JPopupMenu menu = super.createPopupMenu(e);
        		
        		if (menu!=null) {
        			JTable table = getTable();
        			ListSelectionModel lsm = table.getSelectionModel();
            		if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) return menu;
        		
        			final EntityActorPermission eap = (EntityActorPermission)getRows().get(table.getSelectedRow()).getUserObject();

        			if (OWNER_PERMISSION.equals(eap.getPermissions())) {
        				// No menu for the permanent owner permission. In the future this might show a "gifting" option
        				// if the owner wants to transfer ownership.
        			}
        			else if (ModelMgrUtils.isOwner(entity)) {
	    		        	
	        			JMenuItem editItem = new JMenuItem("  Edit");
	    		        editItem.addActionListener(new ActionListener() {
	    					@Override
	    					public void actionPerformed(ActionEvent e) {
	    		    			eapDialog.showForPermission(eap);
	    					}
	    				});
	    		        menu.add(editItem);
    		        
	    		        JMenuItem deleteItem = new JMenuItem("  Delete");
	    		        deleteItem.addActionListener(new ActionListener() {
	    					@Override
	    					public void actionPerformed(ActionEvent e) {
	    						
	    						Utils.setWaitingCursor(EntityDetailsDialog.this);

    							Object[] options = {"All data in tree", "Just this entity", "Cancel"};
    							String message = "Remove the permissions from all data in this tree, or just this entity?";
    							final int removeConfirmation = JOptionPane.showOptionDialog(EntityDetailsDialog.this, message, "Apply permissions recursively?",
    									JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
    							if (removeConfirmation == 2) {
    								return;
    							}
    							final boolean recursive = removeConfirmation==0;
    							
	    				        SimpleWorker worker = new SimpleWorker() {
	
	    							@Override
	    							protected void doStuff() throws Exception {
	    								ModelMgr.getModelMgr().revokePermissions(eap.getEntity().getId(), eap.getSubjectKey(), recursive);
	    							}
	    							
	    							@Override
	    							protected void hadSuccess() {
	    								Utils.setDefaultCursor(EntityDetailsDialog.this);
	    								refresh();
	    								if (recursive) {
	    									ModelMgr.getModelMgr().invalidateCache(entity, true);
	    								}
	    							}
	    							
	    							@Override
	    							protected void hadError(Throwable error) {
	    								SessionMgr.getSessionMgr().handleException(error);
	    								Utils.setDefaultCursor(EntityDetailsDialog.this);
	    								refresh();
	    							}
	    						};
	    						worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Revoking permissions...", ""));
	    						worker.execute();
	    					}
	    				});
	    		        menu.add(deleteItem);
    		        }
        		}
        		
        		return menu;
        	}
        	
			@Override
			protected void rowDoubleClicked(int row) {
    			final EntityActorPermission eap = (EntityActorPermission)getRows().get(row).getUserObject();
    			eapDialog.showForPermission(eap);
			}
        };
        permissionsTable.addColumn(PERMISSIONS_COLUMN_SUBJECT, PERMISSIONS_COLUMN_SUBJECT, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_TYPE, PERMISSIONS_COLUMN_TYPE, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_PERMS, PERMISSIONS_COLUMN_PERMS, true, false, false, true);
        
        addPermissionButton = new JButton("Grant permission");
        addPermissionButton.setEnabled(false);
        addPermissionButton.setToolTipText("Grant permission to a user or group");
        addPermissionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				eapDialog.showForNewPermission(entity);
			}
		});

        permissionsButtonPane = new JPanel();
        permissionsButtonPane.setLayout(new BoxLayout(permissionsButtonPane, BoxLayout.LINE_AXIS));
        permissionsButtonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        permissionsButtonPane.add(addPermissionButton);
        permissionsButtonPane.add(Box.createHorizontalGlue());
        
        permissionsPanel.add(permissionsButtonPane, BorderLayout.NORTH);
        
        tabbedPane.addTab("Permissions", Icons.getIcon("group.png"), permissionsPanel, "Who has access to the this data entity");
        
        
        // Annotations tab

        annotationsLoadingLabel = new JLabel();
        annotationsLoadingLabel.setOpaque(false);
        annotationsLoadingLabel.setIcon(Icons.getLoadingIcon());
        annotationsLoadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        annotationsLoadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        annotationsPanel = new JPanel(new BorderLayout());
        annotationsPanel.add(annotationsLoadingLabel, BorderLayout.CENTER);
        annotationsView = new AnnotationTablePanel();

        
        tabbedPane.addTab("Annotations", Icons.getIcon("page_white_edit.png"), annotationsPanel, "The user annotations");
        
        
        // Buttons
        
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
    }

    public void showForRootedEntity(RootedEntity rootedEntity) {
    	EntityData entityData = rootedEntity.getEntityData();
    	showForEntity(rootedEntity.getEntity(), entityData.getEntityAttribute().getName());
    }

    public void showForEntity(final Entity entity) {
    	showForEntity(entity, null);
    }
    
    private void showForEntity(final Entity entity, final String role) {

    	this.role = role;
    	
    	// Do not allow non-owners to add permissions
    	addPermissionButton.setVisible(ModelMgrUtils.isOwner(entity));
    	
		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.5),(int)(browser.getHeight()*0.8)));

		loadSubjects();
        loadAttributes(entity.getId());

        // Register this dialog as a model observer
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrAdapter);
        
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrAdapter);
    }
    
    private void loadAttributes(final long entityId) {

    	attributesPanel.removeAll();
    	attributesPanel.add(attributesLoadingLabel, BorderLayout.CENTER);
    	final SearchConfiguration searchConfig = SessionMgr.getBrowser().getGeneralSearchConfig();
    	
        SimpleWorker worker = new SimpleWorker() {

        	private Entity loadedEntity;
        	private EntityDocument doc;
        	
			@Override
			protected void doStuff() throws Exception {
				this.loadedEntity = ModelMgr.getModelMgr().getEntityById(entityId);
	            SolrQuery query = new SolrQuery("id:"+entityId);
	            SolrResults results = ModelMgr.getModelMgr().searchSolr(query);
	            this.doc = results.getEntityDocuments().isEmpty() ? null : results.getEntityDocuments().iterator().next();
			}
			
			@Override
			protected void hadSuccess() {

				setEntity(loadedEntity);
				loadAnnotations();
		        loadPermissions();
				
		    	setTitle("Entity Details: "+entity.getName());
				
		        // Update the attribute table
		        attributesTable.removeAllRows();
		        
		        attributesTable.addRow(new AttributeValue("GUID", ""+loadedEntity.getId()));
        		attributesTable.addRow(new AttributeValue("Name", loadedEntity.getName()));
        		attributesTable.addRow(new AttributeValue("Type", loadedEntity.getEntityType().getName()));
        		if (role!=null) {
        			attributesTable.addRow(new AttributeValue("Role", role));
        		}
        		attributesTable.addRow(new AttributeValue("Creation Date", df.format(loadedEntity.getCreationDate())));
        		attributesTable.addRow(new AttributeValue("Updated Date", df.format(loadedEntity.getUpdatedDate())));
        		
		        for (EntityData entityData : loadedEntity.getOrderedEntityData()) {
		        	if (entityData.getChildEntity()==null) {
		        		AttributeValue attrValue = new AttributeValue(entityData.getEntityAttribute().getName(), entityData.getValue());
		        		attributesTable.addRow(attrValue);
		        	}
		        }       
		        
		        if (doc!=null) {
		        	List<SearchAttribute> attrs = searchConfig.getAttributeGroups().get(AttrGroup.SAGE);
	            	for(SearchAttribute attr : attrs) {
	            		String value = searchConfig.getValue(doc, attr.getName());
	            		if (value!=null) {
			        		AttributeValue attrValue = new AttributeValue(attr.getLabel(), value);
			        		attributesTable.addRow(attrValue);
	            		}
	            	}
	            }
		        
		        attributesTable.updateTableModel();
		        attributesPanel.removeAll();
		        attributesPanel.add(attributesTable, BorderLayout.CENTER);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				attributesPanel.removeAll();
		        attributesPanel.add(attributesTable, BorderLayout.CENTER);
		        attributesPanel.revalidate();
			}
		};
		worker.execute();
    }

    private void loadSubjects() {

        SimpleWorker worker = new SimpleWorker() {

        	List<Subject> subjects;
        	
			@Override
			protected void doStuff() throws Exception {
				subjects = ModelMgr.getModelMgr().getSubjects();
				Collections.sort(subjects, new Comparator<Subject>() {
					@Override
					public int compare(Subject o1, Subject o2) {
						return ComparisonChain.start().compare(o1.getKey(), o2.getKey()).result();
					}
				});
			}
			
			@Override
			protected void hadSuccess() {	
				setSubjects(subjects);
				addPermissionButton.setEnabled(true);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		worker.execute();
    }
    
    private void loadPermissions() {

    	permissionsPanel.removeAll();
    	permissionsPanel.add(permissionsLoadingLabel, BorderLayout.CENTER);
    	
        SimpleWorker permissionsLoadingWorker = new SimpleWorker() {

        	private List<EntityActorPermission> eaps = new ArrayList<EntityActorPermission>();
        	
			@Override
			protected void doStuff() throws Exception {
				eaps.addAll(entity.getEntityActorPermissions());
				Collections.sort(eaps, new Comparator<EntityActorPermission>() {
					@Override
					public int compare(EntityActorPermission o1, EntityActorPermission o2) {
						return ComparisonChain.start().compare(o1.getSubjectKey(), o2.getSubjectKey()).compare(o1.getId(), o2.getId()).result();
					}
				});
			}
			
			@Override
			protected void hadSuccess() {
				permissionsTable.removeAllRows();
				permissionsTable.addRow(new EntityActorPermission(entity, entity.getOwnerKey(), OWNER_PERMISSION));
				for(EntityActorPermission eap : eaps) {
					permissionsTable.addRow(eap);
				}
				permissionsTable.updateTableModel();
				permissionsPanel.removeAll();
				permissionsPanel.add(permissionsButtonPane, BorderLayout.NORTH);
				permissionsPanel.add((JPanel)permissionsTable, BorderLayout.CENTER);
				permissionsPanel.revalidate();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				permissionsPanel.removeAll();
				permissionsPanel.add(permissionsButtonPane, BorderLayout.NORTH);
				permissionsPanel.add((JPanel)permissionsTable, BorderLayout.CENTER);
				permissionsPanel.revalidate();
			}
		};
		permissionsLoadingWorker.execute();
    }
    
    
    private void loadAnnotations() {

    	annotationsView.setAnnotations(null);
    	annotationsPanel.removeAll();
    	annotationsPanel.add(annotationsLoadingLabel, BorderLayout.CENTER);
    	
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
				annotationsView.setAnnotations(annotations);
				annotationsPanel.removeAll();
				annotationsPanel.add((JPanel)annotationsView, BorderLayout.CENTER);
				annotationsPanel.revalidate();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				annotationsPanel.removeAll();
				annotationsPanel.add((JPanel)annotationsView, BorderLayout.CENTER);
				annotationsPanel.revalidate();
			}
		};
		annotationLoadingWorker.execute();
    }
    
    public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public List<Subject> getSubjects() {
		return subjects;
	}

	public List<Subject> getUnusedSubjects() {
		List<Subject> filtered = new ArrayList<Subject>();
		for(Subject subject : subjects) {
			boolean used = false;
			for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
				if (subject.equals(eap.getSubjectKey())) {
					used = true;
				}
			}
			if (!used) {
				filtered.add(subject);
			}
		}
		return filtered;
	}
	
	public void setSubjects(List<Subject> subjects) {
		this.subjects = subjects;
	}
		
    public void refresh() {
		loadSubjects();
        loadAttributes(entity.getId());
    }

    public void totalRefresh() {
    	throw new UnsupportedOperationException();
    }

    public boolean isAccessible() {
    	return true;
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
