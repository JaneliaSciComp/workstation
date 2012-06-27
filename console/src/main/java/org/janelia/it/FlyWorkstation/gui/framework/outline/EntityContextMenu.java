package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.MailHelper;
import org.janelia.it.jacs.shared.utils.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

	protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	
	protected final List<RootedEntity> rootedEntityList;
	protected final RootedEntity rootedEntity;
	protected final boolean multiple;
    private JMenu errorMenu;
    private OntologyRoot tmpErrorOntology = ModelMgr.ERROR_ONTOLOGY_ENTITY;
	
	// Internal state
	protected boolean nextAddRequiresSeparator = false;
	
	public EntityContextMenu(List<RootedEntity> rootedEntityList) {
		this.rootedEntityList = rootedEntityList;
		this.rootedEntity = rootedEntityList.size()==1 ? rootedEntityList.get(0) : null;
		this.multiple = rootedEntityList.size()>1;
	}

	public EntityContextMenu(RootedEntity rootedEntity) {
		this.rootedEntity = rootedEntity;
		this.rootedEntityList = new ArrayList<RootedEntity>();
		rootedEntityList.add(rootedEntity);
		this.multiple = false;
	}

	public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());
        add(getGotoRelatedItem());
        
        setNextAddRequiresSeparator(true);
        add(getAddToRootFolderItem());
        add(getRenameItem());
        add(getErrorFlag());
		add(getDeleteItem());
        
		setNextAddRequiresSeparator(true);
		add(getOpenInFirstViewerItem());
		add(getOpenInSecondViewerItem());
		add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dItem());
        add(getFijiViewerItem());

        setNextAddRequiresSeparator(true);
        add(getSearchHereItem());
        
		setNextAddRequiresSeparator(true);
		add(getCreateSessionItem());
        if ((SessionMgr.getUsername().equals("simpsonj") || SessionMgr.getUsername().equals("simpsonlab")) && !this.multiple){
            add(getSpecialAnnotationSession());
        }
	}

    private void addBadDataButtons() {

        if (null!=tmpErrorOntology){
            List<OntologyElement> ontologyElements = tmpErrorOntology.getChildren();
            for(final OntologyElement element: ontologyElements){
                errorMenu.add(new JMenuItem(element.getName())).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        Callable<Void> doSuccess = new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SimpleWorker simpleWorker = new SimpleWorker() {
                                    @Override
                                    protected void doStuff() throws Exception {
                                        String annotationValue = "";
                                        List<Entity> annotationEntities = ModelMgr.getModelMgr().getAnnotationsForEntity(rootedEntity.getEntity().getId());
                                        for(Entity annotation:annotationEntities){
                                            if(annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM).contains(element.getName())){
                                                annotationValue = annotation.getName();
                                            }
                                        }

                                        String tempsubject = "Reported Data: " + rootedEntity.getEntity().getName();
                                        StringBuilder sBuf = new StringBuilder();
                                        sBuf.append("Name: ").append(rootedEntity.getEntity().getName()).append("\n");
                                        sBuf.append("Type: ").append(rootedEntity.getEntity().getEntityType().getName()).append("\n");
                                        sBuf.append("ID: ").append(rootedEntity.getEntity().getId().toString()).append("\n");
                                        sBuf.append("Annotation: ").append(annotationValue).append("\n\n");
                                        MailHelper helper = new MailHelper();
                                        helper.sendEmail((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
                                                ConsoleProperties.getString("console.HelpEmail"),
                                                tempsubject, sBuf.toString());
                                    }

                                    @Override
                                    protected void hadSuccess() {
                                    }

                                    @Override
                                    protected void hadError(Throwable error) {
                                        SessionMgr.getSessionMgr().handleException(error);
                                    }
                                };
                                simpleWorker.execute();
                                return null;
                            }
                        };

                        AnnotateAction action = new AnnotateAction(doSuccess);
                        action.init(element);
                        action.doAction();
                    }
                });

            }
//            OntologyElementChooser flagType = new OntologyElementChooser("Please choose a bad data flag from the list",
//                    ModelMgr.getModelMgr().getOntology(tmpErrorOntology.getId()));
//            flagType.setSize(400,400);
//            flagType.setIconImage(SessionMgr.getBrowser().getIconImage());
//            flagType.setCanAnnotate(true);
//            flagType.showDialog(SessionMgr.getBrowser());

        }
    }

    protected JMenuItem getTitleItem() {;
		String name = multiple ? "(Multiple selected)" : rootedEntity.getEntity().getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
	}
	
	protected JMenuItem getDetailsItem() {
		if (multiple) return null;
        JMenuItem detailsMenuItem = new JMenuItem("  View details");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        new EntityDetailsDialog().showForRootedEntity(rootedEntity);
			}
		});
        return detailsMenuItem;
	}

	private void gotoEntity(final Entity entity) {
		
		Utils.setWaitingCursor(SessionMgr.getBrowser());
		
		SimpleWorker worker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				// Find the best context to show the entity in
				List<List<EntityData>> edPaths = ModelMgr.getModelMgr().getPathsToRoots(entity.getId());
				List<EntityDataPath> paths = new ArrayList<EntityDataPath>();
				for (List<EntityData> path : edPaths) {
					if (ModelMgr.getModelMgr().hasAccess(path.get(0))) {
						EntityDataPath edp = new EntityDataPath(path);
						if (!edp.isHidden()) {
							paths.add(edp);	
						}
					}
				}
				sortPathsByPreference(paths);
				
				for(EntityDataPath edp : paths) {
					System.out.println(""+edp);
				}
				
				if (paths.isEmpty()) {
					throw new Exception("Could not find the related entity");
				}
				
				EntityDataPath chosen = paths.get(0);
				SessionMgr.getBrowser().getEntityOutline().selectEntityByUniqueId(chosen.getUniqueId());
			}
			
			@Override
			protected void hadSuccess() {
				Utils.setDefaultCursor(SessionMgr.getBrowser());
			}
			
			@Override
			protected void hadError(Throwable error) {
				Utils.setDefaultCursor(SessionMgr.getBrowser());
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}
	
	private void sortPathsByPreference(List<EntityDataPath> paths) {
		
		Collections.sort(paths, new Comparator<EntityDataPath>() {
			@Override
			public int compare(EntityDataPath p1, EntityDataPath p2) {
				Integer p1Score = 0;
				Integer p2Score = 0;
				p1Score += "system".equals(p1.getRootOwner())?2:0;
				p2Score += "system".equals(p2.getRootOwner())?2:0;
				p1Score += SessionMgr.getUsername().equals(p1.getRootOwner())?1:0;
				p2Score += SessionMgr.getUsername().equals(p2.getRootOwner())?1:0;
				EntityData e1 = p1.getPath().get(0);
				EntityData e2 = p2.getPath().get(0);
				int c = p2Score.compareTo(p1Score);
				if (c==0) {
					return e2.getId().compareTo(e1.getId());
				}
				return c;
			}
			
		});
	}
	
	private class EntityDataPath {
		private List<EntityData> path;
		private String rootOwner;
		private boolean isHidden = false;
		
		public EntityDataPath(List<EntityData> path) {
			this.path = path;
			EntityData first = path.get(0);
			this.rootOwner = first.getParentEntity().getUser().getUserLogin();
			for (EntityData ed : path) {
				if (EntityUtils.isHidden(ed)) this.isHidden = true;
			}
		}

		public String getUniqueId() {
			return EntityUtils.getUniqueIdFromParentEntityPath(path);
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for(EntityData pathEd : path) {
				if (sb.length()<=0) {
					sb.append(" / "+pathEd.getParentEntity().getName());	
				}
				sb.append(" / "+pathEd.getChildEntity().getName());
			}
			return sb.toString();
		}
		
		public List<EntityData> getPath() {
			return path;
		}

		public String getRootOwner() {
			return rootOwner;
		}

		public boolean isHidden() {
			return isHidden;
		}
	}
	
	protected JMenuItem getGotoRelatedItem() {
		if (multiple) return null;
		JMenu relatedMenu = new JMenu("  Go to related");
		Entity entity = rootedEntity.getEntity();
		add(relatedMenu, getRelatedItem(entity, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE, "Representative Sample"));
		add(relatedMenu, getRelatedItem(entity, EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE, "Original Fly Line"));
		add(relatedMenu, getRelatedItem(entity, EntityConstants.ATTRIBUTE_BALANCED_FLYLINE, "Balanced Fly Line"));
        return relatedMenu;
	}
	
	private JMenuItem getRelatedItem(Entity entity, String attributeName, String label) {
		final EntityData ed = entity.getEntityDataByAttributeName(attributeName);
		if (ed==null) return null;
        JMenuItem relatedMenuItem = new JMenuItem(label);
        relatedMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        gotoEntity(ed.getChildEntity());
			}
		});
        return relatedMenuItem;
	}
	
	protected JMenuItem getCopyNameToClipboardItem() {
		if (multiple) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy name to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(rootedEntity.getEntity().getName());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}

	protected JMenuItem getCopyIdToClipboardItem() {
		if (multiple) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(rootedEntity.getEntity().getId().toString());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getRenameItem() {
		if (multiple) return null;
		JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(browser, "Name:\n", "Rename "+rootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, rootedEntity.getEntity().getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }
	            
	            try {
	            	// Make sure we have the latest entity, then we can rename it
	            	Entity dbEntity = ModelMgr.getModelMgr().getEntityById(""+rootedEntity.getEntity().getId());
	            	dbEntity.setName(newName);
	            	ModelMgr.getModelMgr().saveOrUpdateEntity(dbEntity);
	            }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(), "Error renaming entity", "Error", JOptionPane.ERROR_MESSAGE);
				}
				
            }
        });
		if (!rootedEntity.getEntity().getUser().getUserLogin().equals(SessionMgr.getUsername())) {
			renameItem.setEnabled(false);
		}
        return renameItem;
	}

    protected JMenu getErrorFlag(){
        if (multiple) return null;
        errorMenu = new JMenu("  Report a problem with this data");
        addBadDataButtons();
        return errorMenu;
    }

//    private void bugReport_actionPerformed(){
//        String tempsubject = "Flagged Data: " + rootedEntity.getEntity().getName();
//        StringBuilder sBuf = new StringBuilder();
//        sBuf.append("Name: ").append(rootedEntity.getEntity().getName()).append("\n");
//        sBuf.append("Type: ").append(rootedEntity.getEntity().getEntityType().getName()).append("\n");
//        sBuf.append("ID: ").append(rootedEntity.getEntity().getId().toString()).append("\n\n");
//        MailHelper helper = new MailHelper();
//        helper.sendEmail((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
//                ConsoleProperties.getString("console.HelpEmail"),
//                tempsubject, sBuf.toString());
//
//        Entity tmpErrorOntology = null;
//
//        try {
//           tmpErrorOntology = ModelMgr.getModelMgr().getErrorOntology();
//        }
//
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (null!=tmpErrorOntology){
//            OntologyElementChooser flagType = new OntologyElementChooser("Please choose a bad data flag from the list",
//                    ModelMgr.getModelMgr().getOntology(tmpErrorOntology.getId()));
//            flagType.setSize(400,400);
//            flagType.setIconImage(SessionMgr.getBrowser().getIconImage());
//            flagType.setCanAnnotate(true);
//            flagType.showDialog(SessionMgr.getBrowser());
//
//        }
//
//    }

	protected JMenu getAddToRootFolderItem() {

		if (!multiple && rootedEntity.getEntity()!=null && rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
			return null;
		}
		
		JMenu newFolderMenu = new JMenu("  Add to top-level folder");
		
		List<EntityData> rootEds = SessionMgr.getBrowser().getEntityOutline().getRootEntity().getOrderedEntityData();
		
		for(EntityData rootEd : rootEds) {
			final Entity commonRoot = rootEd.getChildEntity();
			if (!commonRoot.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
			
			JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
			commonRootItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					SimpleWorker worker = new SimpleWorker() {
						@Override
						protected void doStuff() throws Exception {
							List<Long> ids = new ArrayList<Long>();
							for(RootedEntity rootedEntity : rootedEntityList) {
								ids.add(rootedEntity.getEntity().getId());
							}
							ModelMgr.getModelMgr().addChildren(commonRoot.getId(), ids, EntityConstants.ATTRIBUTE_ENTITY);
						}
						@Override
						protected void hadSuccess() {
							SessionMgr.getBrowser().getEntityOutline().refresh(true, null);
						}
						@Override
						protected void hadError(Throwable error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					};
					worker.execute();
				}
			});
			
			newFolderMenu.add(commonRootItem);
		}
		
		newFolderMenu.addSeparator();
		
		JMenuItem createNewItem = new JMenuItem("Create new...");
		
		createNewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				// Add button clicked
				final String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
						"Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
				if ((folderName == null) || (folderName.length() <= 0)) {
					return;
				}

				SimpleWorker worker = new SimpleWorker() {
					private Entity newFolder;
					@Override
					protected void doStuff() throws Exception {
						// Update database
						newFolder = ModelMgrUtils.createNewCommonRoot(folderName);
						
						List<Long> ids = new ArrayList<Long>();
						for(RootedEntity rootedEntity : rootedEntityList) {
							ids.add(rootedEntity.getEntity().getId());
						}
						ModelMgr.getModelMgr().addChildren(newFolder.getId(), ids, EntityConstants.ATTRIBUTE_ENTITY);
					}
					@Override
					protected void hadSuccess() {
						// Update Tree UI
						SessionMgr.getBrowser().getEntityOutline().refresh(true, null);
					}
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				worker.execute();
			}
		});
		
		newFolderMenu.add(createNewItem);
		
		return newFolderMenu;
	}

	protected JMenuItem getDeleteItem() {
		
		for(RootedEntity rootedEntity : rootedEntityList) {
			EntityData ed = rootedEntity.getEntityData();
			if (ed.getId()==null && ed.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)==null) {
				// Fake ED, not a common root, this must be part of an annotation session. 
				// TODO: this check could be done more robustly
				return null;
			}
		}
		
		JMenuItem deleteItem = new JMenuItem(multiple?"  Remove "+rootedEntityList.size()+" entities":"  Remove");
		
		deleteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				final Set<EntityData> toDelete = new HashSet<EntityData>();
				for(RootedEntity rootedEntity : rootedEntityList) {
					toDelete.add(rootedEntity.getEntityData());
				}
				
				// Pre-screen the selections to ensure we have permission to delete everything 
				for(EntityData ed : new HashSet<EntityData>(toDelete)) {
					if (ed.getUser()!=null && !ed.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
						JOptionPane.showMessageDialog(browser, "Do not have permission to delete "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
						toDelete.remove(ed);
					}
				}
					
				SimpleWorker verifyTask = new SimpleWorker() {

					private Set<EntityData> removeTree = new HashSet<EntityData>();
					private Set<EntityData> removeReference = new HashSet<EntityData>();
					private Set<EntityData> removeRootTag = new HashSet<EntityData>();
						
					@Override
					protected void doStuff() throws Exception {
						setProgress(1);
						
						for(EntityData ed : toDelete) {
							Entity child = ed.getChildEntity();
							List<EntityData> eds = ModelMgr.getModelMgr().getParentEntityDatas(ed.getChildEntity().getId());
							// Determine deletion type
							if (child.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
								// Common root
								if (eds.isEmpty()) {
									// No references to this root, so delete the entire tree
									removeTree.add(ed);
								}
								else if (ed.getId()==null) {
									// User wants to delete the root, so just remove the tag, but leave the tree intact
									removeRootTag.add(ed);
								}
								else {
									// User wants to delete a reference to the root
									removeReference.add(ed);
								}
							}
							else {
								if (eds.size() > 1) {
									// Just remove the reference
									removeReference.add(ed);
								}
								else {
									// Only 1 reference left, so delete the entire tree
									removeTree.add(ed);
								}
							}
						}
					}
					
					@Override
					protected void hadSuccess() {
						
						final Set<EntityData> toReallyDelete = new HashSet<EntityData>(toDelete);
						for(EntityData ed : toDelete) {
							Entity child = ed.getChildEntity();
							if (removeRootTag.contains(ed)) {
								// Must own the root in order to de-root it
								if (!child.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
									JOptionPane.showMessageDialog(browser, "No permission to remove "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
									toReallyDelete.remove(ed);
								}
							}
							else if (removeReference.contains(ed)) {
								// Must own the reference to remove it
								if (!ed.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
									JOptionPane.showMessageDialog(browser, "No permission to remove "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
									toReallyDelete.remove(ed);
								}
							}
							else if (removeTree.contains(ed)) {
								// Must own the tree root to delete it
								if (!ed.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
									JOptionPane.showMessageDialog(browser, "No permission to delete "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
									toReallyDelete.remove(ed);
								}
								else {
									Object[] options = {"Yes", "No"};
									int deleteConfirmation = JOptionPane.showOptionDialog(browser,
											"Are you sure you want to permanently delete '" + ed.getChildEntity().getName()
													+ "' and all orphaned items underneath it?", "Delete",
											JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
									if (deleteConfirmation != 0) {
										toReallyDelete.remove(ed);
									}
								}
							}
						}
						
						if (toReallyDelete.isEmpty()) {
							return;
						}
						
						SimpleWorker removeTask = new SimpleWorker() {
							@Override
							protected void doStuff() throws Exception {
								setProgress(1);
								for(EntityData ed : toReallyDelete) {
									if (removeRootTag.contains(ed)) {
										EntityData rootTagEd = ed.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
										ModelMgr.getModelMgr().removeEntityData(rootTagEd);
									}
									else if (removeReference.contains(ed)) {
										ModelMgr.getModelMgr().removeEntityData(ed);
									} 
									else if (removeTree.contains(ed)) {
										ModelMgr.getModelMgr().deleteEntityTree(ed.getChildEntity().getId());
									}
									else {
										throw new IllegalStateException("Unknown deletion type for EntityData.id="+ed.getId());
									}
								}
							}

							@Override
							protected void hadSuccess() {
							}

							@Override
							protected void hadError(Throwable error) {
								SessionMgr.getSessionMgr().handleException(error);
							}
						};

						removeTask.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Removing...", ""));
						removeTask.execute();
					}
					
					@Override
					protected void hadError(Throwable error) {
                    	SessionMgr.getSessionMgr().handleException(error);
					}
					
				};
				verifyTask.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Verifying...", ""));
				verifyTask.execute();
			}
		});

		for(RootedEntity rootedEntity : rootedEntityList) {
			EntityData entityData = rootedEntity.getEntityData();
			if (entityData!=null && entityData.getUser()!=null && !entityData.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
				deleteItem.setEnabled(false);
				break;
			}
		}
		
		return deleteItem;
	}

	protected JMenuItem getOpenInFirstViewerItem() {
		if (multiple) return null;
		if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open in first viewer");
        
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						if (EntityUtils.isInitialized(rootedEntity.getEntity())) {
							ModelMgrUtils.loadLazyEntity(rootedEntity.getEntity(), false);
						}
					}
					
					@Override
					protected void hadSuccess() {
						Viewer mainViewer = SessionMgr.getBrowser().getMainViewer();
			            ((IconDemoPanel)mainViewer).loadEntity(rootedEntity);
			            mainViewer.setAsActive();
			            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				worker.execute();
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getOpenInSecondViewerItem() {
		if (multiple) return null;
		if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open in second viewer");
        
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						if (EntityUtils.isInitialized(rootedEntity.getEntity())) {
							ModelMgrUtils.loadLazyEntity(rootedEntity.getEntity(), false);
						}
					}
					
					@Override
					protected void hadSuccess() {
						Viewer secViewer = SessionMgr.getBrowser().showSecViewer();
			            ((IconDemoPanel)secViewer).loadEntity(rootedEntity);
			            secViewer.setAsActive();
			            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				worker.execute();
			}
		});
        return copyMenuItem;
	}

	protected JMenuItem getOpenInFinderItem() {
		if (multiple) return null;
		if (!OpenInFinderAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        if (!StringUtils.isEmpty(filepath)) {
        	OpenInFinderAction action = new OpenInFinderAction(rootedEntity.getEntity()) {
        		@Override
        		public String getName() {
        			String name = super.getName();
        			if (name==null) return null;
        			return "  "+name;
        		}
        	};
        	return getActionItem(action);
        }
        return null;
	}
    
	protected JMenuItem getOpenWithAppItem() {
		if (multiple) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        if (!StringUtils.isEmpty(filepath)) {
        	OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(rootedEntity.getEntity()) {
        		@Override
        		public String getName() {
        			return "  System default";
        		}
        	};
        	return getActionItem(action);
        }
        return null;
	}

    protected JMenuItem getFijiViewerItem() {
        if (multiple) return null;
        final String path = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());
        if (path!=null) {
            JMenuItem fijiMenuItem = new JMenuItem("  View In Fiji");
            fijiMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_FIJI,path);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch this tool. " +
                                "Please choose the appropriate file path from the Tools->Configure Tools area", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return fijiMenuItem;
        }
        return null;
    }

    protected JMenuItem getNeuronAnnotatorItem() {
		if (multiple) return null;
        final String entityType = rootedEntity.getEntity().getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D (Neuron Annotator)");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        Entity result = rootedEntity.getEntity();
                        if (!entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                            result = ModelMgr.getModelMgr().getAncestorWithType(result, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                        }

                        if (result != null && ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(result.getId())) {
                            // Success
                            return;
                        }
                    }
                    catch (Exception e) {
                    	SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
	}

	protected JMenuItem getVaa3dItem() {
		if (multiple) return null;
        final String path = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());
        if (path!=null) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch this tool. " +
                                "Please choose the appropriate file path from the Tools->Configure Tools area", "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
	}
	
	protected JMenuItem getCreateSessionItem() {
		if (multiple) return null;
		
		JMenuItem newFragSessionItem = new JMenuItem("  Create annotation session...");
		newFragSessionItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				final Entity entity = rootedEntity.getEntity();
				final String uniqueId = rootedEntity.getUniqueId();
				if (uniqueId == null) return;
				
				EntityOutline entityOutline = SessionMgr.getBrowser().getEntityOutline();
				DefaultMutableTreeNode node = entityOutline.getNodeByUniqueId(uniqueId);
				
				SimpleWorker loadingWorker = new LazyTreeNodeLoader(entityOutline.getDynamicTree(), node, true) {

					protected void doneLoading() {
						List<Entity> entities = entity.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT, true);
						browser.getAnnotationSessionPropertyDialog().showForNewSession(entity.getName(), entities);
					}

					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};

				loadingWorker.execute();
			
			}
		});

		return newFragSessionItem;
	}

	protected JMenuItem getSearchHereItem() {
		if (multiple) return null;
        JMenuItem searchHereMenuItem = new JMenuItem("  Search here");
        searchHereMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                	SessionMgr.getSessionMgr().getActiveBrowser().getSearchDialog().showDialog(rootedEntity.getEntity());
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return searchHereMenuItem;
	}
	
	private JMenuItem getActionItem(final Action action) {
        JMenuItem actionMenuItem = new JMenuItem(action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.doAction();
			}
		});
        return actionMenuItem;
	}

    private JMenuItem getSpecialAnnotationSession(){
        JMenuItem specialAnnotationSession = new JMenuItem("  Special Annotation");
        specialAnnotationSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(null==ModelMgr.getModelMgr().getCurrentOntology()){
                    JOptionPane.showMessageDialog(SessionMgr.getBrowser(),"Please select an ontology in the ontology window.", "Null Ontology Warning", JOptionPane.WARNING_MESSAGE);
                }
                else{
                    if(!SpecialAnnotationChooserDialog.getDialog().isVisible()){
                        SpecialAnnotationChooserDialog.getDialog().setVisible(true);
                    }
                    else{
                        SpecialAnnotationChooserDialog.getDialog().transferFocus();
                    }
                }
            }
        });

        return specialAnnotationSession;
    }
	
	
	
	@Override
	public JMenuItem add(JMenuItem menuItem) {
		
		if (menuItem == null) return null;
		
		if ((menuItem instanceof JMenu)) {
			JMenu menu = (JMenu)menuItem;
			if (menu.getItemCount()==0) return null;
		}
		
		if (nextAddRequiresSeparator) {
			addSeparator();
			nextAddRequiresSeparator = false;
		}
		
		return super.add(menuItem);
	}
	
	public JMenuItem add(JMenu menu, JMenuItem menuItem) {
		if (menu==null || menuItem==null) return null;
		return menu.add(menuItem);
	}

	public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
		this.nextAddRequiresSeparator = nextAddRequiresSeparator;
	}
}
