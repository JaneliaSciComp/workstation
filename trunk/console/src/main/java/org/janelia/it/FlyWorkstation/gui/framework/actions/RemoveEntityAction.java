package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.util.*;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This action removes an entity from some parent. If the entity becomes an orphan, then it is completely deleted.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveEntityAction implements Action {

	private final List<RootedEntity> rootedEntityList;
	private final boolean showConfirmationDialogs;
	
	public RemoveEntityAction(List<RootedEntity> rootedEntityList, boolean showConfirmationDialogs) {
		this.rootedEntityList = rootedEntityList;
		this.showConfirmationDialogs = showConfirmationDialogs;
	}
	
    @Override
    public String getName() {
    	return rootedEntityList.size()>1?"Remove "+rootedEntityList.size()+" entities":"Remove";
    }
	
    @Override
    public void doAction() {
        final Browser browser = SessionMgr.getBrowser();
        
		final Set<EntityData> toDelete = new HashSet<EntityData>();
		for(RootedEntity rootedEntity : rootedEntityList) {
			toDelete.add(rootedEntity.getEntityData());
		}
		
		SimpleWorker verifyTask = new SimpleWorker() {

			private Set<EntityData> removeTree = new HashSet<EntityData>();
			private Set<EntityData> removeReference = new HashSet<EntityData>();
			private Set<EntityData> removeRootTag = new HashSet<EntityData>();
            private Map<EntityData,String> sharedNameMap = new HashMap<EntityData,String>();
            private Set<Long> invalidIdSet = new HashSet<Long>();
				
			@Override
			protected void doStuff() throws Exception {
			    
				for(EntityData ed : toDelete) {
					Entity child = ed.getChildEntity();
	                
					List<EntityData> parentEds = ModelMgr.getModelMgr().getParentEntityDatas(child.getId());

					// To be technically correct, this should check for any eds owned by members of the owner group, if
					// the entity is owned by a group. However, groups can't login, so presumably this deletion 
					// action will never be run on a group-owned entity.
			        Set<EntityData> ownedEds = new HashSet<EntityData>();
			        for(EntityData parentEd : parentEds) {
			            invalidIdSet.add(parentEd.getParentEntity().getId());
			            if (ModelMgrUtils.isOwner(parentEd.getParentEntity())) {
			                ownedEds.add(parentEd);
			            }
			        }
					
			        // Determine deletion type
			        if (ModelMgrUtils.hasWriteAccess(child)) {

	                    if (child.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
	                        // Common root
	                        if (ownedEds.isEmpty()) {
	                            // No owned references to this root, so delete the entire tree. If there are non-owned 
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
	                        if (ownedEds.size() > 1) {
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
	                        Set<EntityActorPermission> permissions = ModelMgr.getModelMgr().getFullPermissions(child.getId());
	                        for(EntityActorPermission permission : permissions) {
	                            sharedNames.add(permission.getSubjectName());
	                        }
	                        Collections.sort(sharedNames);
	                        sharedNameMap.put(ed, Task.csvStringFromCollection(sharedNames));
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
							JOptionPane.showMessageDialog(browser, "No permission to remove "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeReference.contains(ed)) {
					    if (!ModelMgrUtils.hasWriteAccess(ed.getParentEntity())) {
							JOptionPane.showMessageDialog(browser, "No permission to remove "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeTree.contains(ed)) {
					    if (!ModelMgrUtils.hasWriteAccess(child)) {
							JOptionPane.showMessageDialog(browser, "No permission to delete "+child.getName(), "Error", JOptionPane.ERROR_MESSAGE);
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
								int r = JOptionPane.showOptionDialog(browser, message.toString(), "Delete",
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
								int r = JOptionPane.showOptionDialog(browser, message.toString(), "Delete",
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
				
				SimpleWorker removeTask = new SimpleWorker() {
					@Override
					protected void doStuff() throws Exception {
						for(EntityData ed : toReallyDelete) {
							if (removeRootTag.contains(ed)) {
								ModelMgr.getModelMgr().demoteCommonRootToFolder(ed.getChildEntity());
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
}
