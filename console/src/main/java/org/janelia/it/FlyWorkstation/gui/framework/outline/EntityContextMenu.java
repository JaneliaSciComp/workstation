package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.PreferenceConstants;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

	protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	
	// Option A: a list of entities, where the operation affects all of them
	protected final List<EntityData> entityDataList;
	
	// Option B: a specific entity
	protected final EntityData entityData;
	protected final Entity entity; // just a shortcut to entityData.getChildEntity()
	protected final String uniqueId;
	
	// Internal state
	protected boolean nextAddRequiresSeparator = false;
	
	public EntityContextMenu(EntityData entityData, String uniqueId) {
		super();
		this.entityDataList = new ArrayList<EntityData>();
		entityDataList.add(entityData);
		this.entityData = entityData;
		this.entity = entityData!=null ? entityData.getChildEntity() : null;
		this.uniqueId = uniqueId;
	}

	public EntityContextMenu(List<RootedEntity> rootedEntityList) {
		super();
		this.entityDataList = new ArrayList<EntityData>();
		// TODO: use the rootedEntity internally instead of this
		for(RootedEntity rootedEntity : rootedEntityList) {
			entityDataList.add(rootedEntity.getEntityData());
		}
		
		this.entityData = null;
		this.entity = null;
		this.uniqueId = null;
	}

	public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());
        add(getOpenInSecondViewerItem());
        
        setNextAddRequiresSeparator(true);
        add(getAddToRootFolderItem());
        add(getRenameItem());
		add(getDeleteItem());
        
		setNextAddRequiresSeparator(true);
    	add(getOpenInFinderItem());
    	add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dItem());
        
        setNextAddRequiresSeparator(true);
        add(getSearchHereItem());
	}

	protected JMenuItem getTitleItem() {;
		String name = entity == null ? "(Multiple selected)" : entity.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
	}
	
	protected JMenuItem getDetailsItem() {
		if (entityData==null) return null;
        JMenuItem detailsMenuItem = new JMenuItem("  View details");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        new EntityDetailsDialog().showForEntityData(entityData);
			}
		});
        return detailsMenuItem;
	}
	
	protected JMenuItem getCopyNameToClipboardItem() {
		if (entityData==null) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy name to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getName());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}

	protected JMenuItem getCopyIdToClipboardItem() {
		if (entityData==null) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getId().toString());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getRenameItem() {
		if (entityData==null || entityData.getId()==null) return null;
		JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(browser, "Name:\n", "Rename "+entity.getName(), JOptionPane.PLAIN_MESSAGE, null, null, entity.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }
	            
	            try {
	            	// Make sure we have the latest entity, then we can rename it
	            	Entity dbEntity = ModelMgr.getModelMgr().getEntityById(""+entity.getId());
	            	dbEntity.setName(newName);
	            	ModelMgr.getModelMgr().saveOrUpdateEntity(dbEntity);
	            }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(), "Error renaming entity", "Error", JOptionPane.ERROR_MESSAGE);
				}
				
            }
        });
		if (!entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
			renameItem.setEnabled(false);
		}
        return renameItem;
	}

	protected JMenu getAddToRootFolderItem() {

		if (entity!=null && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
			return null;
		}
		
		JMenu newFolderMenu = new JMenu("  Add to top-level folder...");
		
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
							for(EntityData entityData : entityDataList) {
								ModelMgrUtils.addChild(commonRoot, entityData.getChildEntity());
							}
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
						for(EntityData entityData : entityDataList) {
							ModelMgrUtils.addChild(newFolder, entityData.getChildEntity());
						}
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
		
		for(EntityData ed : entityDataList) {
			if (ed.getId()==null && ed.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)==null) {
				// Fake ED, not a common root, this must be part of an annotation session. 
				// TODO: this check could be done more robustly
				return null;
			}
		}
		
		JMenuItem deleteItem = new JMenuItem(entityData!=null?"  Remove":"  Remove "+entityDataList.size()+" entities");
		
		deleteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				Utils.setWaitingCursor(browser.getEntityOutline());

				// Pre-screen the selections to ensure we have permission to delete everything 
				final Set<EntityData> toDelete = new HashSet<EntityData>(entityDataList);
				for(EntityData ed : entityDataList) {
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
						
						int i = 0;
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
							setProgress(i++, toDelete.size());
						}
					}
					
					@Override
					protected void hadSuccess() {
						
						Utils.setWaitingCursor(browser.getEntityOutline());
						
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
							Utils.setDefaultCursor(browser.getEntityOutline());
							return;
						}
						
						SimpleWorker removeTask = new SimpleWorker() {
							@Override
							protected void doStuff() throws Exception {
								int i = 0;
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
									setProgress(i++, toReallyDelete.size());
								}
							}

							@Override
							protected void hadSuccess() {
								Utils.setDefaultCursor(browser.getEntityOutline());
							}

							@Override
							protected void hadError(Throwable error) {
								Utils.setDefaultCursor(browser.getEntityOutline());
								SessionMgr.getSessionMgr().handleException(error);
							}
						};

						removeTask.setProgressMonitor(new ProgressMonitor(SessionMgr.getBrowser(), "Removing", "", 0, 100));
						removeTask.execute();
					}
					
					@Override
					protected void hadError(Throwable error) {
						Utils.setDefaultCursor(browser.getEntityOutline());
                    	SessionMgr.getSessionMgr().handleException(error);
					}
					
				};
				verifyTask.setProgressMonitor(new ProgressMonitor(SessionMgr.getBrowser(), "Verifying", "", 0, 100));
				verifyTask.execute();
			}
		});

		if (entityData!=null && entityData.getUser()!=null && !entityData.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
			deleteItem.setEnabled(false);
		}
		return deleteItem;
	}
	
	protected JMenuItem getOpenInSecondViewerItem() {
		if (entityData==null) return null;
		if (Utils.isEmpty(uniqueId)) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open in second viewer");
        
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Viewer secViewer = SessionMgr.getBrowser().getViewersPanel().getSecViewer();
				if (secViewer==null) {
					secViewer = new IconDemoPanel(SessionMgr.getBrowser().getViewersPanel(), EntitySelectionModel.CATEGORY_SEC_VIEW);
					SessionMgr.getBrowser().getViewersPanel().setSecViewer(secViewer);
				}
	            ((IconDemoPanel)secViewer).loadEntity(new RootedEntity(uniqueId, entityData));
	            secViewer.setAsActive();
	            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId, true);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getOpenInFinderItem() {
		if (entityData==null) return null;
		if (!OpenInFinderAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenInFinderAction(entity));
        }
        return null;
	}
	
	protected JMenuItem getOpenWithAppItem() {
		if (entityData==null) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenWithDefaultAppAction(entity));
        }
        return null;
	}
	
	protected JMenuItem getNeuronAnnotatorItem() {
		if (entityData==null) return null;
        final String entityType = entity.getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D (Neuron Annotator)");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        Entity result = entity;
                        if (!entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                            result = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
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
		if (entityData==null) return null;
        final String entityType = entity.getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_IMAGE_3D) ||
            entityType.equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK) ||
            entityType.equals(EntityConstants.TYPE_LSM_STACK) ||
            entityType.equals(EntityConstants.TYPE_STITCHED_V3D_RAW) ||
            entityType.equals(EntityConstants.TYPE_SWC_FILE) ||
            entityType.equals(EntityConstants.TYPE_V3D_ANO_FILE) ||
            entityType.equals(EntityConstants.TYPE_TIF_3D)) {
        	
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
//                        if (entity != null && ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(entity.getId())) {
//                            // Success
//                            return;
//                        }
                        String vaa3dExePath = (String) SessionMgr.getSessionMgr().getModelProperty(PreferenceConstants.PATH_VAA3D);
//                        vaa3dExePath = "/Applications/FlySuite.app/Contents/Resources/vaa3d64.app/Contents/MacOS/vaa3d64"; // DEBUG ONLY
                        File tmpFile = new File(vaa3dExePath);
                        if (tmpFile.exists()&&tmpFile.canExecute()) {
                            vaa3dExePath+=" -i "+ PathTranslator.convertPath(EntityUtils.getAnyFilePath(entity));
                            System.out.println("Calling to open file with: "+vaa3dExePath);
                            Runtime.getRuntime().exec(vaa3dExePath);
                        }
                        else {
                            JOptionPane.showMessageDialog(browser, "Could not launch Vaa3D", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
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

	protected JMenuItem getSearchHereItem() {
		if (entityData==null) return null;
        JMenuItem searchHereMenuItem = new JMenuItem("  Search here...");
        searchHereMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                	SessionMgr.getSessionMgr().getActiveBrowser().getSearchDialog().showDialog(entity);
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return searchHereMenuItem;
	}
	
	private JMenuItem getActionItem(final Action action) {
        JMenuItem actionMenuItem = new JMenuItem("  "+action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.doAction();
			}
		});
        return actionMenuItem;
	}
	
	@Override
	public JMenuItem add(JMenuItem menuItem) {
		
		if (menuItem == null) return null;
		
		if (nextAddRequiresSeparator) {
			addSeparator();
			nextAddRequiresSeparator = false;
		}
		
		return super.add(menuItem);
	}

	public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
		this.nextAddRequiresSeparator = nextAddRequiresSeparator;
	}
}
