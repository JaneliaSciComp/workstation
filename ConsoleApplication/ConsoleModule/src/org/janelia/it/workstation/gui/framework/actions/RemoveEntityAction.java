package org.janelia.it.workstation.gui.framework.actions;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action removes an entity from some parent. If the entity becomes an orphan, then it is completely deleted.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveEntityAction implements Action {
    
    private static final Logger log = LoggerFactory.getLogger(RemoveEntityAction.class);
    
	private final List<RootedEntity> rootedEntityList;
	private final boolean showConfirmationDialogs;
	private final boolean runInBackground;
	private final Callable<Void> success;
	
	public RemoveEntityAction(List<RootedEntity> rootedEntityList, boolean showConfirmationDialogs, boolean runInBackground) {
	    this(rootedEntityList, showConfirmationDialogs, runInBackground, null);
	}
	
	public RemoveEntityAction(List<RootedEntity> rootedEntityList, boolean showConfirmationDialogs, boolean runInBackground, Callable<Void> success) {
		this.rootedEntityList = rootedEntityList;
		this.showConfirmationDialogs = showConfirmationDialogs;
		this.runInBackground = runInBackground;
		this.success = success;
	}
	
    @Override
    public String getName() {
    	return rootedEntityList.size()>1?"Remove "+rootedEntityList.size()+" entities":"Remove";
    }
	
    @Override
    public void doAction() {
        final Browser browser = SessionMgr.getBrowser();
        final Component mainFrame = SessionMgr.getMainFrame();
        
		final Set<EntityData> toDelete = new HashSet<EntityData>();
		for(RootedEntity rootedEntity : rootedEntityList) {
			toDelete.add(rootedEntity.getEntityData());
		}
		
		SimpleWorker verifyTask = new SimpleWorker() {

			private Set<EntityData> removeTree = new HashSet<EntityData>();
			private Set<EntityData> removeReference = new HashSet<EntityData>();
			private Set<EntityData> removeRootTag = new HashSet<EntityData>();
            private Map<EntityData,String> sharedNameMap = new HashMap<EntityData,String>();
				
			@Override
			protected void doStuff() throws Exception {
			    
				for(EntityData ed : toDelete) {
					Entity child = ed.getChildEntity();
	                
					List<EntityData> parentEds = ModelMgr.getModelMgr().getAllParentEntityDatas(child.getId());
			        Map<Long,EntityData> respectedEds = new HashMap<Long,EntityData>();
			        // The current reference should always be considered
                    respectedEds.put(ed.getId(),ed);
                    
			        for(EntityData parentEd : parentEds) {
			            if (!ModelMgrUtils.isOwner(child)) {
			                // If we don't own the current entity, then all references to it are respected
			                respectedEds.put(parentEd.getId(),parentEd);
			            }
			            else {
			                // If we own the current entity, then we only care about our own references
			                if (ModelMgrUtils.isOwner(parentEd.getParentEntity())) {
	                            respectedEds.put(parentEd.getId(),parentEd);
			                }
			            }
			        }
					
			        // Determine deletion type
			        if (ModelMgrUtils.hasWriteAccess(child)) {

	                    if (child.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
	                        // Common root
	                        if (respectedEds.size()==1) {
	                            // No accessible references to this root aside from the one we're deleting, so delete the entire tree. If there are non-accessible 
	                            // references, the user will be warned about them before the tree is deleted.
	                            removeTree.add(ed);
	                        }
	                        else if (ed.getId()==null) {
	                            // User wants to delete the root, but it's referenced elsewhere. So let's just remove the 
	                            // common root tag, but leave the tree intact, so that it remains in the other places it 
	                            // is referenced.
	                            removeRootTag.add(ed);
	                        }
	                        else {
	                            // User wants to delete a reference to the root.
	                            removeReference.add(ed);
	                        }
	                    }
	                    else {
	                        if (respectedEds.size() > 1) {
	                            // Just remove the reference
	                            removeReference.add(ed);
	                        }
	                        else {
	                            // Only 1 reference left, so delete the entire tree
	                            removeTree.add(ed);
	                        }
	                    }
	                    
	                    if (removeTree.contains(ed)) {
	                        // When removing a tree, we need to check if its shared, and ask the user if they want to really delete a shared object.
	                        List<String> sharedNames = new ArrayList<String>();
	                        List<String> sharedKeys = new ArrayList<String>();
	                        Set<EntityActorPermission> permissions = ModelMgr.getModelMgr().getFullPermissions(child.getId());
	                        for(EntityActorPermission permission : permissions) {
	                            sharedNames.add(permission.getSubjectName());
	                            sharedKeys.add(permission.getSubjectKey());
	                        }
                            if (sharedKeys.size()==1 && sharedKeys.get(0).equals(SessionMgr.getSubjectKey())) {
                                log.trace("Entity {} is shared with the current user ({}) only",child.getId(),SessionMgr.getSubjectKey());
                            }
                            else {
                                Collections.sort(sharedNames);
                                sharedNameMap.put(ed, Task.csvStringFromCollection(sharedNames));   
                            }
	                    }
			        }
			        else {
			            removeReference.add(ed);
			        }
				}
			}
			
			@Override
			protected void hadSuccess() {
		        
				boolean confirmedAll = !showConfirmationDialogs;
				
				final Set<EntityData> toReallyDelete = new HashSet<EntityData>(toDelete);
				for(EntityData ed : toDelete) {
					Entity child = ed.getChildEntity();
					if (removeRootTag.contains(ed)) {
					    if (!ModelMgrUtils.hasWriteAccess(child)) {
							JOptionPane.showMessageDialog(mainFrame, "No permission to remove "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeReference.contains(ed)) {
					    if (!ModelMgrUtils.hasWriteAccess(ed.getParentEntity())) {
							JOptionPane.showMessageDialog(mainFrame, "No permission to remove "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeTree.contains(ed)) {
					    if (!ModelMgrUtils.hasWriteAccess(child)) {
							JOptionPane.showMessageDialog(mainFrame, "No permission to delete "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
						else if (!confirmedAll) {
						    
						    String sharedNames = sharedNameMap.get(ed);
						    StringBuilder message = new StringBuilder();
						    if (!StringUtils.isEmpty(sharedNames)) {
						        message.append(ed.getChildEntity().getName());
						        message.append(" is currently shared with the following users: ");
						        message.append(sharedNames);
						        message.append(".\nAre you sure you want to permanently delete it?");
						    }
						    else {
						        message.append("Are you sure you want to permanently delete\n '");
						        message.append(ed.getChildEntity().getName());    
						        message.append("'\nand all orphaned items inside it?");
						    }
						    
							if (toDelete.size() > 1) {
								Object[] options = {"Yes", "Yes to All", "No", "Cancel"};	
								int r = JOptionPane.showOptionDialog(mainFrame, message.toString(), "Delete",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
								switch (r) {
								case 0:
									break;
								case 1:
									confirmedAll = true;
									break;
								case 2:
									toReallyDelete.remove(ed);
									break;
								case 3:
									return;
								}
							}
							else {
								Object[] options = {"Yes", "No", "Cancel"};
								int r = JOptionPane.showOptionDialog(mainFrame, message.toString(), "Delete",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
								switch (r) {
								case 0:
									break;
								case 1:
									return;
								case 2:
									return;
								}
							}
						}
					}
				}
				
				if (toReallyDelete.isEmpty()) {
					return;
				}
				        
				if (runInBackground) {
    	            BackgroundWorker removeTask = new BackgroundWorker() {
    
                        @Override
                        public String getName() {
                            return "Remove entities";
                        }
            
    					@Override
    					protected void doStuff() throws Exception {
                            
    						for(EntityData ed : toReallyDelete) {
    							if (removeRootTag.contains(ed)) {
    							    setStatus("Demoting "+ed.getChildEntity().getName()+" from folder");
    							    log.debug("Demoting to common root: "+ed.getChildEntity().getName());
    								ModelMgr.getModelMgr().demoteCommonRootToFolder(ed.getChildEntity());
    							}
    							else if (removeReference.contains(ed)) {
    							    setStatus("Removing reference "+ed.getId());
    							    log.debug("Removing reference: "+ed.getId());
    								ModelMgr.getModelMgr().removeEntityData(ed);
    							} 
    							else if (removeTree.contains(ed)) {
    							    setStatus("Removing tree "+ed.getChildEntity().getName());
    							    log.debug("Removing tree: "+ed.getChildEntity().getId());
    								ModelMgr.getModelMgr().deleteEntityTree(ed.getChildEntity().getId());
    							}
    							else {
    								throw new IllegalStateException("Unknown deletion type for EntityData.id="+ed.getId());
    							}
    						}
    					}
    
    					@Override
    					protected void hadSuccess() {
    					    ConcurrentUtils.invokeAndHandleExceptions(success);
    					}
    
    					@Override
    					protected void hadError(Throwable error) {
    						SessionMgr.getSessionMgr().handleException(error);
    					}
    				};
    
    				removeTask.executeWithEvents();
				}
				else {
                    SimpleWorker removeTask = new SimpleWorker() {
    
                        @Override
                        protected void doStuff() throws Exception {
                            
                            for(EntityData ed : toReallyDelete) {
                                if (removeRootTag.contains(ed)) {
                                    log.debug("Demoting to common root: "+ed.getChildEntity().getName());
                                    ModelMgr.getModelMgr().demoteCommonRootToFolder(ed.getChildEntity());
                                }
                                else if (removeReference.contains(ed)) {
                                    log.debug("Removing reference: "+ed.getId());
                                    ModelMgr.getModelMgr().removeEntityData(ed);
                                } 
                                else if (removeTree.contains(ed)) {
                                    log.debug("Removing tree: "+ed.getChildEntity().getId());
                                    ModelMgr.getModelMgr().deleteEntityTree(ed.getChildEntity().getId());
                                }
                                else {
                                    throw new IllegalStateException("Unknown deletion type for EntityData.id="+ed.getId());
                                }
                            }
                        }
    
                        @Override
                        protected void hadSuccess() {
                            ConcurrentUtils.invokeAndHandleExceptions(success);
                        }
    
                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
    
                    removeTask.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Removing...", ""));
                    removeTask.execute();
                }
			}
			
			@Override
			protected void hadError(Throwable error) {
            	SessionMgr.getSessionMgr().handleException(error);
			}
			
		};
		verifyTask.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Verifying...", ""));
		verifyTask.execute();	
    }
}
