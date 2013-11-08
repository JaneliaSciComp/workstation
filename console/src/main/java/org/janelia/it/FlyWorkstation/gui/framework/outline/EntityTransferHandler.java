package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardViewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.LayersPanel;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.action.GetLongAction;

/**
 * Support for dragging entities and dropping them onto the EntityOutline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityTransferHandler extends TransferHandler {

	private static final Logger log = LoggerFactory.getLogger(EntityTransferHandler.class);
	
	protected boolean moveWhenReordering = true;

    public abstract JComponent getDropTargetComponent();

    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {

        log.debug("createTransferable sourceComponent={}",sourceComponent);
        
        if (sourceComponent instanceof JTree) {
            
            EntityTree entityTree = (EntityTree)getEntityTreeAncestor(sourceComponent);
            JTree tree = entityTree.getTree();
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) return null;

            List<RootedEntity> entityList = new ArrayList<RootedEntity>();
            for(TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                RootedEntity rootedEntity = entityTree.getRootedEntity(entityTree.getDynamicTree().getUniqueId(node));
                log.debug("Adding entity to transferrable: {}",EntityUtils.identify(rootedEntity.getEntity()));
                entityList.add(rootedEntity);
            }
            return new TransferableEntityList(entityTree, entityList);
        }
        else if (sourceComponent instanceof AnnotatedImageButton) {
            List<RootedEntity> entityList = new ArrayList<RootedEntity>();
            
            Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
            final List<String> selectedEntities = new ArrayList<String>(
                    ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(viewer.getSelectionCategory()));
            for(String selectedId : selectedEntities) {
                RootedEntity matchingEntity = viewer.getRootedEntityById(selectedId);
                if (matchingEntity == null) {
                    throw new IllegalStateException("Entity not found in viewer: "+selectedId);
                }
                entityList.add(matchingEntity);
            }       
            return new TransferableEntityList(sourceComponent, entityList);
        }
        else if (sourceComponent instanceof LayersPanel) {
            log.warn("No transfer handling defined from a LayersPanel");
            return null;
        }
        else {
            throw new IllegalStateException("Unsupported component type for transfer: "+sourceComponent.getClass().getName());
        }
    }

    public String getAttribute() {
        return EntityConstants.ATTRIBUTE_ENTITY;
    }

    @Override
    public int getSourceActions(JComponent sourceComponent) {
        return LINK;
    }
    
	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
	    
		try {
			// Only dealing with drag and drop for now
			if (!support.isDrop()) return false;
			
			support.setShowDropLocation(true);
			
			DropLocation dropLocation = support.getDropLocation();
			JComponent dropTarget = getDropTargetComponent();
						
			if (dropTarget instanceof EntityTree) {

	            DataFlavor reFlavor = TransferableEntityList.getRootedEntityFlavor();
	            DataFlavor sourceFlavor = TransferableEntityList.getSourceFlavor();
	            
	            if (!support.isDataFlavorSupported(reFlavor)) {
	                log.debug("Disallow transfer because {} data flavor {} supported",reFlavor.getMimeType());
	                return false;
	            }
			    
			    EntityTree targetEntityTree = (EntityTree)dropTarget;
			    
			    // Drag to the entity tree
	            JTree.DropLocation dl = (JTree.DropLocation)dropLocation;
	            TreePath targetPath = dl.getPath();
	            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)targetPath.getLastPathComponent();

                // Can't moved to the root unless it's visible
                if (!targetEntityTree.getTree().isRootVisible() && targetNode.isRoot()) {
                    log.debug("Disallow transfer because target node is a root");
                    return false;
                }

                JComponent sourceComponent = (JComponent)support.getTransferable().getTransferData(sourceFlavor);
                
	            // Derive unique TreePaths for the source entities. It will allow us to enforce some tree-based rules.
	            List<TreePath> sourcePaths = new ArrayList<TreePath>();      
                if (sourceComponent instanceof EntityTree) {

	                if (sourceComponent!=targetEntityTree) {
	                    log.debug("Disallow tree to tree transfer");
	                    return false;
	                }
	                
	                EntityTree tree = (EntityTree)sourceComponent;
	                JTree jtree = tree.getTree();
	                int[] selRows = jtree.getSelectionRows();
	                if (selRows!=null && selRows.length>0) {
	                    TreePath path = jtree.getPathForRow(selRows[0]);
	                    if (path!=null) {
	                        sourcePaths.add(path);
	                    }
	                }
	            }
	            else if (sourceComponent instanceof AnnotatedImageButton) {
	                List<String> selectedEntities = new ArrayList<String>(
	                        ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
	                                SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
	                IconPanel iconPanel = ((AnnotatedImageButton)sourceComponent).getIconPanel();
	                for(String selectedId : selectedEntities) {
	                    RootedEntity rootedEntity = iconPanel.getRootedEntityById(selectedId);
	                    DefaultMutableTreeNode node = targetEntityTree.getNodeByUniqueId(rootedEntity.getUniqueId());
	                    sourcePaths.add(new TreePath(node.getPath()));
	                }               
	            }
	            else if (sourceComponent==null) {
	                throw new IllegalStateException("Source component is null");
	            }
	            else {
	                throw new IllegalStateException("Illegal source component type: "+sourceComponent.getClass().getName());
	            }

	            for(TreePath sourcePath : sourcePaths) {

	                // Can't move to a descendant
	                if (sourcePath.isDescendant(targetPath)) {
	                    log.debug("Disallow transfer because source node descendant of target");
	                    return false;
	                }
                    
	                // Can't move the root
	                DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)sourcePath.getLastPathComponent();
	                if (sourceNode.isRoot()) {
	                    log.debug("Disallow transfer because source node is a root");
	                    return false;
	                }
	                
	                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)sourceNode.getParent();
	                int sourceIndex = parentNode.getIndex(sourceNode);
	                int targetIndex = dl.getChildIndex();
	                
	                // Do not allow a drop in the same location 
	                if ((targetPath.equals(sourcePath) || targetPath.equals(sourcePath.getParentPath())) 
	                        && (sourceIndex==targetIndex || (targetIndex<0 && sourceIndex==parentNode.getChildCount()-1))) {
	                    log.debug("Disallow transfer to the same location");
	                    return false;
	                }
	            }
	            
	            // Enforce some Entity-specific rules
	            List<RootedEntity> entities = (List<RootedEntity>)support.getTransferable().getTransferData(reFlavor);
	            if (!allowTransfer(targetEntityTree, targetNode, entities)) {
	                log.debug("Disallow transfer because of entity rules");
	                return false;
	            }
	            
	            return true;
			    
			}
			else if ((dropTarget instanceof LayersPanel) || (dropTarget instanceof AlignmentBoardViewer)) {
                // Drag to the alignment board

	            Transferable transferable = support.getTransferable();
	            List<RootedEntity> entities = (List<RootedEntity>)transferable.getTransferData(TransferableEntityList.getRootedEntityFlavor());
	            	            
	            return true;
            }
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}

        return false;
	}

	/**
	 * Test if a transfer of a given entities to the target parent node is allowed.
	 * @param targetNode the new parent for the entities
	 * @param entitiesToAdd list of entities to add to the the new parentv
	 * @return true if transfer is allowed
	 */
	protected boolean allowTransfer(EntityTree entityTree, DefaultMutableTreeNode targetNode, List<RootedEntity> entitiesToAdd) {
		
		// Disallow transfer if target node is not owned by the user
		Entity targetEntity = entityTree.getEntity(targetNode);
		if (!ModelMgrUtils.hasWriteAccess(targetEntity)) {
			log.debug("Disallow transfer because user does not have write access to the target");
			return false;
		}

		for(RootedEntity entity : entitiesToAdd) {
			// Disallow transfer if the entity is in the ancestor chain
			DefaultMutableTreeNode nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			if (nextParent!=null) {
				nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			}
			while (nextParent != null) {
				Entity ancestor = entityTree.getEntity(nextParent);
				if (Utils.areSameEntity(entity.getEntity(), ancestor)) {
					log.debug("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
		}
		
		return true;
	}

	@Override
	public boolean importData(final TransferHandler.TransferSupport support) {

		if (!canImport(support)) return false;

		try {
			// Extract transfer data
			final Transferable t = (Transferable)support.getTransferable();
			final List<RootedEntity> rootedEntities = (List<RootedEntity>)t.getTransferData(TransferableEntityList.getRootedEntityFlavor());
			final JComponent dropTarget = getDropTargetComponent();

			DefaultMutableTreeNode parent = null;
			int index = 0;
			
            if (dropTarget instanceof EntityTree) {
                // Get drop location info
                JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
                int childIndex = dl.getChildIndex();
                TreePath targetPath = dl.getPath();
                parent = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                
                // Get drop index
                index = childIndex; // DropMode.INSERT
                if (childIndex == -1) { // DropMode.ON
                    EntityData parentEd = (EntityData)parent.getUserObject();
                    Entity parentEntity = parentEd.getChildEntity();
                    index = parentEntity.getChildren().size();
                }
            }
            
            final DefaultMutableTreeNode finalParent = parent;
            final int finalIndex = index;
                    
            SimpleWorker worker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // Actually perform the transfer
                    if (dropTarget instanceof EntityTree) {
                        EntityTree targetEntityTree = (EntityTree)dropTarget;
                        addEntities(targetEntityTree, finalParent, rootedEntities, finalIndex, support.getDropAction()==MOVE);
                    }
                    else if ((dropTarget instanceof LayersPanel) || (dropTarget instanceof AlignmentBoardViewer)) {
                        addEntitiesToAlignmentBoard(SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext(), rootedEntities);
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
            worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Adding entities...", ""));
            worker.execute();
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return false;
		}

		return true;
	}
	
	/**
	 * Add the given entities to the given target parent, both in the entity model, and in the view.
	 * @param targetNode the new parent for the entities
	 * @param entitiesToAdd list of entities to add to the the new parent
	 * @param destIndex child insertion index in the new parent
	 * @param deleteSources delete the sources after copying them (i.e. make this a move operation)
	 * @throws Exception
	 */
	protected void addEntities(EntityTree entityTree, DefaultMutableTreeNode targetNode, List<RootedEntity> entitiesToAdd, int destIndex, boolean deleteSources) throws Exception {

		Entity parentEntity = entityTree.getEntity(targetNode);
		
		log.debug("Will add entities to {} (deleteSources={})",EntityUtils.identify(parentEntity),deleteSources);
		
		// First update the entity model
		List<EntityData> edsToRemove = new ArrayList<EntityData>();
		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(parentEntity);
		
		int origSize = eds.size();
		int currIndex = destIndex;
		
		for(RootedEntity rootedEntity : entitiesToAdd) {
		    
		    log.debug("  Adding {}",EntityUtils.identify(rootedEntity.getEntity()));
		    
		    // Should we delete the sources after copying into the new location (e.g. is this a move?)
		    boolean deleteSourceEds = deleteSources;
		    if (!deleteSources && moveWhenReordering) {
		        // Not a move, but are we just reordering nodes under the same parent?
                Enumeration enumeration = targetNode.children();
                while (enumeration.hasMoreElements()) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
                    EntityData ed = entityTree.getEntityData(childNode);
                    if (ed!=null && ed.getChildEntity()!=null && Utils.areSameEntity(rootedEntity.getEntity(), ed.getChildEntity())) {
                        log.debug("  This is a reordering, so we'll delete the source: "+ed.getId());
                        deleteSourceEds = true;
                    }
                }
		    }
		    
			// Add the entity to the new parent, generating the ED
			EntityData newEd = ModelMgr.getModelMgr().addEntityToParent(parentEntity, rootedEntity.getEntity(), 
			        parentEntity.getMaxOrderIndex()==null?0:parentEntity.getMaxOrderIndex()+1, getAttribute());

			if (destIndex > origSize) {
				eds.add(newEd);
			} 
			else {
				eds.add(currIndex++, newEd);
			}

			if (deleteSourceEds) {
			    edsToRemove.add(rootedEntity.getEntityData());
			}
		}
        
		log.debug("Renumbering children");
		
		// Renumber the children, re-add the new ones, and update the tree
		int index = 0;
		for (EntityData ed : eds) {
			if (log.isDebugEnabled()) log.debug("  "+EntityUtils.identify(ed.getChildEntity())+" (oldIndex={},newIndex={})",ed.getOrderIndex(),index);
			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
				log.debug("  will save ED {} with index={}",ed.getId(),index);
				EntityData savedEd = ModelMgr.getModelMgr().updateChildIndex(ed, index);
				log.debug("  saved ED {}",savedEd.getId());
			}
			index++;
		}
		
		// Remove old eds if necessary
		for(EntityData ed : edsToRemove) {
		    ModelMgr.getModelMgr().removeEntityData(ed);
            log.debug("Deleted old ED {}",ed.getId());
		}
	}
    
    /**
     * Add the given entities to the specified alignment board, if possible.
     * @param alignmentBoardContext
     * @param entitiesToAdd
     */
    protected void addEntitiesToAlignmentBoard(AlignmentBoardContext alignmentBoardContext, List<RootedEntity> entitiesToAdd) throws Exception {
        for(RootedEntity rootedEntity : entitiesToAdd) {
            alignmentBoardContext.addRootedEntity(rootedEntity);
        }
    }
    
    protected EntityTree getEntityTreeAncestor(JComponent sourceComponent) {
        JComponent component = sourceComponent;
        while (true) {
            if (component == null) break;
            if (component instanceof EntityTree) return (EntityTree)component;
            component = (JComponent)component.getParent();
        }
        return null;
    }
}