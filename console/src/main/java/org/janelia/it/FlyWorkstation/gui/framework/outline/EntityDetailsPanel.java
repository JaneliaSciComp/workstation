package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityActorPermissionDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationTablePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotationView;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.user_data.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * A panel for displaying details about the currently selected entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsPanel extends JPanel implements Accessibility, Refreshable {

	private static final Logger log = LoggerFactory.getLogger(EntityDetailsPanel.class);
	
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private static final String ATTRIBUTES_COLUMN_KEY = "Attribute Name";
    private static final String ATTRIBUTES_COLUMN_VALUE = "Attribute Value";
    
    private static final String PERMISSIONS_COLUMN_SUBJECT = "Subject";
    private static final String PERMISSIONS_COLUMN_TYPE = "Type";
    private static final String PERMISSIONS_COLUMN_PERMS = "Permissions";
    
    public static final String TAB_NAME_ATTRIBUTES = "Attributes";
    public static final String TAB_NAME_PERMISSIONS = "Permissions";
    public static final String TAB_NAME_ANNOTATIONS = "Annotations";
    
    private static final String OWNER_PERMISSION = "owner";
    
    private JTabbedPane tabbedPane;
    private List<String> tabNames = new ArrayList<String>();
    
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
    
    private List<Subject> subjects;
    private Entity entity;
    private String role;
    
    private JLabel createLoadingLabel() {
        JLabel loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        return loadingLabel;
    }
    
    public EntityDetailsPanel() {

        setLayout(new BorderLayout());
        
        tabNames.add(TAB_NAME_ATTRIBUTES);
        tabNames.add(TAB_NAME_PERMISSIONS);
        tabNames.add(TAB_NAME_ANNOTATIONS);
		
		// Child dialogs
		
		eapDialog = new EntityActorPermissionDialog(this);
		
		// Tabbed pane
		
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        
        // Attributes tab
        
        attributesLoadingLabel = createLoadingLabel();
        attributesPanel = new JPanel(new BorderLayout());
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

        tabbedPane.addTab(TAB_NAME_ATTRIBUTES, Icons.getIcon("table.png"), attributesPanel, "The data entity's attributes");
        
        
        // Permissions tab

        permissionsLoadingLabel = createLoadingLabel();
        permissionsPanel = new JPanel(new BorderLayout());
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
	    		        	
	        			JMenuItem editItem = new JMenuItem("  Edit Permission");
	    		        editItem.addActionListener(new ActionListener() {
	    					@Override
	    					public void actionPerformed(ActionEvent e) {
	    		    			eapDialog.showForPermission(eap);
	    					}
	    				});
	    		        menu.add(editItem);
    		        
	    		        JMenuItem deleteItem = new JMenuItem("  Delete Permission");
	    		        deleteItem.addActionListener(new ActionListener() {
	    					@Override
	    					public void actionPerformed(ActionEvent e) {
	    						
	    						Utils.setWaitingCursor(EntityDetailsPanel.this);

    							Object[] options = {"All subfolders", "Just this entity", "Cancel"};
    							String message = "Remove this permission from all subfolders, or just this entity?";
    							final int removeConfirmation = JOptionPane.showOptionDialog(EntityDetailsPanel.this, message, "Apply permissions recursively?",
    									JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    							if (removeConfirmation == 2) {
    								return;
    							}
    							final boolean recursive = removeConfirmation==0;
    							
	    				        SimpleWorker worker = new SimpleWorker() {
	
	    							@Override
	    							protected void doStuff() throws Exception {
	    								ModelMgr.getModelMgr().revokePermissions(eap.getEntity().getId(), eap.getSubjectKey(), recursive);
	    								if (recursive) {
                                            ModelMgr.getModelMgr().invalidateCache(entity, true);
                                        }
	    							}
	    							
	    							@Override
	    							protected void hadSuccess() {
	    								Utils.setDefaultCursor(EntityDetailsPanel.this);
	    								refresh();
	    							}
	    							
	    							@Override
	    							protected void hadError(Throwable error) {
	    								SessionMgr.getSessionMgr().handleException(error);
	    								Utils.setDefaultCursor(EntityDetailsPanel.this);
	    								refresh();
	    							}
	    						};
	    						worker.setProgressMonitor(new IndeterminateProgressMonitor(EntityDetailsPanel.this, "Revoking permissions...", ""));
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

        permissionsPanel = new JPanel(new BorderLayout());
        permissionsPanel.add(permissionsButtonPane, BorderLayout.NORTH);
        permissionsPanel.add(permissionsTable, BorderLayout.CENTER);
        
        tabbedPane.addTab("Permissions", Icons.getIcon("group.png"), permissionsPanel, "Who has access to the this data entity");
        
        
        // Annotations tab

        annotationsLoadingLabel = createLoadingLabel();
        annotationsPanel = new JPanel(new BorderLayout());
        annotationsView = new AnnotationTablePanel();

        tabbedPane.addTab("Annotations", Icons.getIcon("page_white_edit.png"), annotationsPanel, "The user annotations");
    }

    public void showNothing() {
        attributesPanel.removeAll();
        permissionsPanel.removeAll();
        annotationsPanel.removeAll();
        this.updateUI();
    }
    
    public void showLoadingIndicator() {
        showAttributesLoadingIndicator();
        showPermissionsLoadingIndicator();
        showAnnotationsLoadingIndicator();
        this.updateUI();
    }
    
    private void showAttributesLoadingIndicator() {
        attributesPanel.removeAll();
        attributesPanel.add(attributesLoadingLabel, BorderLayout.CENTER);
    }
    
    private void showPermissionsLoadingIndicator() {
        permissionsPanel.removeAll();
        permissionsPanel.add(permissionsLoadingLabel, BorderLayout.CENTER);
    }
    
    private void showAnnotationsLoadingIndicator() {
        annotationsPanel.removeAll();
        annotationsPanel.add(annotationsLoadingLabel, BorderLayout.CENTER);
    }
    
    public void loadRootedEntity(RootedEntity rootedEntity) {
    	loadRootedEntity(rootedEntity, TAB_NAME_ATTRIBUTES);
    }
    
    public void loadRootedEntity(RootedEntity rootedEntity, String defaultTab) {
        if (rootedEntity==null) {
            showNothing();
            return;
        }
    	EntityData entityData = rootedEntity.getEntityData();
    	loadEntity(rootedEntity.getEntity(), entityData.getEntityAttribute().getName(), defaultTab);
    }

    public void loadEntity(final Entity entity) {
    	loadEntity(entity, TAB_NAME_ATTRIBUTES);
    }
    
    public void loadEntity(final Entity entity, final String defaultTab) {
    	loadEntity(entity, null, defaultTab);
    }
    
    public void loadEntity(final Entity entity, final String role, String defaultTab) {

    	this.role = role;
    	this.entity = entity;
    	
		loadSubjects();
        loadAttributes(entity.getId());
        
        // Select the default tab
        if (defaultTab!=null) {
            tabbedPane.setSelectedIndex(tabNames.indexOf(defaultTab));
        }
    }
    
    private void loadAttributes(final long entityId) {

        log.debug("Loading attributes for {}",entity.getId());
        showAttributesLoadingIndicator();
        
    	final SearchConfiguration searchConfig = SessionMgr.getBrowser().getGeneralSearchConfig();
    	
        SimpleWorker worker = new SimpleWorker() {

        	private Entity loadedEntity;
        	private EntityDocument doc;
			private boolean solrError = false;
        	
			@Override
			protected void doStuff() throws Exception {
				this.loadedEntity = ModelMgr.getModelMgr().getEntityById(entityId);
				log.debug("Loading entity {}",entity.getId());
				try {
		            SolrQuery query = new SolrQuery("id:"+entityId);
		            SolrResults results = ModelMgr.getModelMgr().searchSolr(query);
		            this.doc = results.getEntityDocuments().isEmpty() ? null : results.getEntityDocuments().iterator().next();
		            log.debug("Loading entity document {}",entity.getId());
				}
				catch (Exception e) {
					log.error("Error loading Solr attributes",e);
					solrError  = true;
				}
			}
			
			@Override
			protected void hadSuccess() {

				setEntity(loadedEntity);
				loadAnnotations();
		        loadPermissions();

                log.debug("Showing attributes");
                
		        // Update the attribute table
		        attributesTable.removeAllRows();
		        
		        attributesTable.addRow(new AttributeValue("GUID", ""+loadedEntity.getId()));
        		attributesTable.addRow(new AttributeValue("Name", loadedEntity.getName()));
        		attributesTable.addRow(new AttributeValue("Type", loadedEntity.getEntityType().getName()));
        		if (role!=null) {
        			attributesTable.addRow(new AttributeValue("Role", role));
        		}
        		if (loadedEntity.getCreationDate()!=null) {
        		    attributesTable.addRow(new AttributeValue("Creation Date", df.format(loadedEntity.getCreationDate())));
        		}
        		if (loadedEntity.getUpdatedDate()!=null) {
        		    attributesTable.addRow(new AttributeValue("Updated Date", df.format(loadedEntity.getUpdatedDate())));
        		}
        		
        		Set<String> attrNames = new HashSet<String>();
        		
        		List<EntityData> entityDatas = new ArrayList<EntityData>(loadedEntity.getEntityData());
                Collections.sort(entityDatas, new Comparator<EntityData>() {
                    @Override
                    public int compare(EntityData o1, EntityData o2) {
                        return o1.getEntityAttribute().getName().compareTo(o2.getEntityAttribute().getName());
                    }
                });
                
		        for (EntityData entityData : entityDatas) {
		        	if (entityData.getChildEntity()==null) {
		        	    String attrName = entityData.getEntityAttribute().getName();
		        		AttributeValue attrValue = new AttributeValue(attrName, entityData.getValue());
		        		attrNames.add(attrName);
		        		attributesTable.addRow(attrValue);
		        	}
		        }       
		        
		        if (doc!=null) {
		        	List<SearchAttribute> attrs = searchConfig.getAttributeGroups().get(AttrGroup.SAGE);
	            	for(SearchAttribute attr : attrs) {
	            		String value = searchConfig.getValue(doc, attr.getName());
	            		if (value!=null) {
	            		    String attrName = attr.getLabel();
	            		    if (!attrNames.contains(attrName)) {
    	                        attrNames.add(attrName);
    			        		AttributeValue attrValue = new AttributeValue(attrName+" (SAGE)", value);
    			        		attributesTable.addRow(attrValue);
	            		    }
	            		}
	            	}
	            }
		        
		        if (solrError) {
		        	attributesTable.addRow(new AttributeValue("ERROR", "Could not query Solr server for additional attributes"));
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

        log.debug("Loading subjects for {}",entity.getId());
        
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
                addPermissionButton.setEnabled(ModelMgrUtils.isOwner(entity));
                log.debug("Setting permission button state to {}",addPermissionButton.isEnabled());
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		worker.execute();
    }
    
    private void loadPermissions() {

        log.debug("Loading permissions for {}",entity.getId());
        
        showPermissionsLoadingIndicator();
        
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

                log.debug("Showing permissions");
                
				permissionsTable.removeAllRows();
				permissionsTable.addRow(new EntityActorPermission(entity, entity.getOwnerKey(), OWNER_PERMISSION));
				for(EntityActorPermission eap : eaps) {
					permissionsTable.addRow(eap);
				}
				permissionsTable.updateTableModel();
                permissionsPanel.removeAll();
                permissionsPanel.add(permissionsButtonPane, BorderLayout.SOUTH);
                permissionsPanel.add(permissionsTable, BorderLayout.CENTER);
                permissionsPanel.revalidate();
                permissionsPanel.repaint();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		permissionsLoadingWorker.execute();
    }
    
    
    public void loadAnnotations() {

        log.debug("Loading annotations for {}",entity.getId());
        
    	showAnnotationsLoadingIndicator();
    	
    	annotationsView.setAnnotations(null);
    	
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

                log.debug("Showing annotations");
                
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
        Collections.sort(filtered, new Comparator<Subject>() {
            @Override
            public int compare(Subject o1, Subject o2) {
                ComparisonChain chain = ComparisonChain.start()
                        .compare(o1.getClass().getName(), o2.getClass().getName(), Ordering.natural())
                        .compare(o1.getFullName(), o2.getFullName(), Ordering.natural().nullsLast())
                        .compare(o1.getName(), o2.getName(), Ordering.natural().nullsFirst());
                return chain.result();
            }
        });
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
