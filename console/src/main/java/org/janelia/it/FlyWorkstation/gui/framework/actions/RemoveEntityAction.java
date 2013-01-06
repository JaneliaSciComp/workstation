package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * This action removes an entity from some parent. If the entity becomes an orphan, then it is completely deleted.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveEntityAction implements Action {

	private List<RootedEntity> rootedEntityList;
	
	public RemoveEntityAction(List<RootedEntity> rootedEntityList) {
		this.rootedEntityList = rootedEntityList;
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
			
			Entity parent = rootedEntity.getEntityData().getParentEntity();
			if (parent!=null && parent.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)!=null) {
			    JOptionPane.showMessageDialog(browser, "Cannot remove items from a protected folder", "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
		}
		
		// Pre-screen the selections to ensure we have permission to delete everything 
		for(EntityData ed : new HashSet<EntityData>(toDelete)) {
			if (ed.getOwnerKey()!=null && !ed.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
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
				
				boolean confirmedAll = false;
				
				final Set<EntityData> toReallyDelete = new HashSet<EntityData>(toDelete);
				for(EntityData ed : toDelete) {
					Entity child = ed.getChildEntity();
					if (removeRootTag.contains(ed)) {
						// Must own the root in order to de-root it
						if (!child.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
							JOptionPane.showMessageDialog(browser, "No permission to remove "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeReference.contains(ed)) {
						// Must own the reference to remove it
						if (!ed.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
							JOptionPane.showMessageDialog(browser, "No permission to remove "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
					}
					else if (removeTree.contains(ed)) {
						// Must own the tree root to delete it
						if (!ed.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
							JOptionPane.showMessageDialog(browser, "No permission to delete "+ed.getChildEntity().getName(), "Error", JOptionPane.ERROR_MESSAGE);
							toReallyDelete.remove(ed);
						}
						else if (!confirmedAll) {
							
							if (toDelete.size() > 1) {
								Object[] options = {"Yes", "Yes to All", "No", "Cancel"};	
								int r = JOptionPane.showOptionDialog(browser,
										"Are you sure you want to permanently delete\n'" + ed.getChildEntity().getName()
												+ "'\nand all orphaned items inside it?", "Delete",
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
								int r = JOptionPane.showOptionDialog(browser,
										"Are you sure you want to permanently delete\n'" + ed.getChildEntity().getName()
												+ "'\nand all orphaned items inside it?", "Delete",
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
