package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
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
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for dragging entities and dropping them onto the EntityOutline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityTransferHandler extends TransferHandler {

	private static final Logger log = LoggerFactory.getLogger(EntityTransferHandler.class);
	
	private static final boolean MOVE_WHEN_REORDERING = true;
	
	private EntityOutline entityOutline;
	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	
	public EntityTransferHandler() {
		this.entityOutline = SessionMgr.getBrowser().getEntityOutline();
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
					+ Entity.class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} 
		catch (ClassNotFoundException e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}
	
	public abstract JComponent getDropTargetComponent();

    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {

        log.debug("EntityTransferHandler.createTransferable");
        
        List<RootedEntity> entityList = new ArrayList<RootedEntity>();
        
        if (sourceComponent instanceof JTree) {
            
            JTree tree = (JTree) sourceComponent;
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) return null;
            
            for(TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                String parentUniqueId = entityOutline.getDynamicTree().getUniqueId(node);
                entityList.add(new RootedEntity(parentUniqueId, entityOutline.getEntityData(node)));
            }
        }
        else if (sourceComponent instanceof AnnotatedImageButton) {
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

        }
        else if (sourceComponent instanceof LayersPanel) {
            log.warn("No transfer handling defined from a LayersPanel");
        }
        else {
            throw new IllegalStateException("Unsupported component type for transfer: "+sourceComponent.getClass().getName());
        }
        
        return new TransferableEntityList(entityList);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return LINK;
    }
    
	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		
		try {
			// Only dealing with drag and drop for now
			if (!support.isDrop()) return false;
			if (!support.isDataFlavorSupported(nodesFlavor)) {
				log.debug("Disallow transfer because data flavor {} is not supported",nodesFlavor.getMimeType());
				return false;
			}
			
			support.setShowDropLocation(true);
			
			Component sourceComponent = support.getComponent();
			DropLocation dropLocation = support.getDropLocation();
			JComponent dropTarget = getDropTargetComponent();
			
			if (dropTarget instanceof EntityTree) {
			    // Drag to the entity tree
			    
	            JTree tree = entityOutline.getTree();
	            int[] selRows = tree.getSelectionRows();
	            JTree.DropLocation dl = (JTree.DropLocation)dropLocation;
	            TreePath targetPath = dl.getPath();
	            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)targetPath.getLastPathComponent();
	            
	            // Derive unique TreePaths for the entities. It will allow us to enforce some tree-based rules.
	            List<TreePath> sourcePaths = new ArrayList<TreePath>();         
	            if (sourceComponent instanceof JTree) {
	                sourcePaths.add(tree.getPathForRow(selRows[0]));
	            }
	            else if (sourceComponent instanceof AnnotatedImageButton) {
	                List<String> selectedEntities = new ArrayList<String>(
	                        ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
	                                SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
	                IconPanel iconPanel = ((AnnotatedImageButton)sourceComponent).getIconPanel();
	                for(String selectedId : selectedEntities) {
	                    RootedEntity rootedEntity = iconPanel.getRootedEntityById(selectedId);
	                    DefaultMutableTreeNode node = entityOutline.getNodeByUniqueId(rootedEntity.getUniqueId());
	                    sourcePaths.add(new TreePath(node.getPath()));
	                }               
	            }
	            else {
	                throw new IllegalStateException("Unknown component for transfer: "+sourceComponent.getClass().getName());
	            }

	            // Can't moved to the root
	            if (targetNode.isRoot()) {
	                log.debug("Disallow transfer because target node is a root");
	                return false;
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
	            
	            // Do not allow a drop on the drag source selections
	            int dropRow = tree.getRowForPath(targetPath);
	            for (int i = 0; i < selRows.length; i++) {
	                if (selRows[i] == dropRow) {
	                    log.debug("Disallow transfer drag and drop rows are identical");
	                    return false;
	                }
	            }
	    
	            // Enforce some Entity-specific rules
	            List<RootedEntity> entities = (List<RootedEntity>)support.getTransferable().getTransferData(nodesFlavor);
	            if (!allowTransfer(targetNode, entities)) {
	                log.debug("Disallow transfer because of entity rules");
	                return false;
	            }
	            
	            return true;
			    
			}
			else if ((dropTarget instanceof LayersPanel) || (dropTarget instanceof AlignmentBoardViewer)) {
                // Drag to the alignment board

	            Transferable transferable = support.getTransferable();
	            List<RootedEntity> entities = (List<RootedEntity>)transferable.getTransferData(nodesFlavor);
	            
//	            if (entities.size()>1) {
//	                log.error("Cannot transfer more than one entity at a time");
//	                return false;
//	            }
	            
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
	protected boolean allowTransfer(DefaultMutableTreeNode targetNode, List<RootedEntity> entitiesToAdd) {
		
		// Disallow transfer if target node is not owned by the user
		Entity targetEntity = entityOutline.getEntity(targetNode);
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
				Entity ancestor = entityOutline.getEntity(nextParent);
				if (Utils.areSameEntity(entity.getEntity(), ancestor)) {
					log.debug("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
			
			// Disallow multiple instances of the same entity
//			for(Entity child : targetEntity.getChildren()) {
//				if (Utils.areSameEntity(entity, child)) {
//					log.debug("Disallow transfer because target already has this child");
//					return false;
//				}
//			}
		}
		
		return true;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {

		if (!canImport(support)) return false;

		try {
			// Extract transfer data
			Transferable t = support.getTransferable();
			final List<RootedEntity> rootedEntities = (List<RootedEntity>)t.getTransferData(nodesFlavor);
		
			JComponent dropTarget = getDropTargetComponent();
            if (dropTarget instanceof EntityTree) {

                // Get drop location info
                JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
                int childIndex = dl.getChildIndex();
                TreePath targetPath = dl.getPath();
                final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                
                // Get drop index
                int index = childIndex; // DropMode.INSERT
                if (childIndex == -1) { // DropMode.ON
                    EntityData parentEd = (EntityData)parent.getUserObject();
                    Entity parentEntity = parentEd.getChildEntity();
                    index = parentEntity.getChildren().size();
                }
                
                final int finalIndex = index;
                        
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // Actually perform the transfer
                        addEntities(parent, rootedEntities, finalIndex);
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
            else if ((dropTarget instanceof LayersPanel) || (dropTarget instanceof AlignmentBoardViewer)) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        addEntities(SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext(), rootedEntities);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Adding aligned entities...", ""));
                worker.execute();
            }
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
	 * @throws Exception
	 */
	protected void addEntities(DefaultMutableTreeNode targetNode, List<RootedEntity> entitiesToAdd, int destIndex) throws Exception {

		Entity parentEntity = entityOutline.getEntity(targetNode);
		
		int i = destIndex;
		for(RootedEntity rootedEntity : entitiesToAdd) {
			log.debug("will add {} to {}",rootedEntity.getName(),parentEntity.getName());
			i++;
		}
		log.debug("updating entity model");

		// First update the entity model
		
		List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();		
		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(parentEntity);
		
		int origSize = eds.size();
		int currIndex = destIndex;		
		
		for(RootedEntity rootedEntity : entitiesToAdd) {
			// Add the entity to the new parent, generating the ED
			EntityData newEd = ModelMgr.getModelMgr().addEntityToParent(parentEntity, rootedEntity.getEntity());
			log.debug("created new child {}",rootedEntity.getName());
			
			if (destIndex > origSize) {
				eds.add(newEd);
			} 
			else {
				eds.add(currIndex++, newEd);
			}

			// Since entities have a single instance, we can no longer tell when we're reordering by comparing 
			// instances. We need a new way to implement this!
			
			if (MOVE_WHEN_REORDERING) {
		        Enumeration enumeration = targetNode.children();
		        while (enumeration.hasMoreElements()) {
		            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
		            EntityData ed = entityOutline.getEntityData(childNode);
					if (ed!=null && ed.getChildEntity()!=null && Utils.areSameEntity(rootedEntity.getEntity(), ed.getChildEntity()) && ed.getId()!=newEd.getId()) {
						toRemove.add(childNode);
					}
		        }
			}
		}

		log.debug("renumbering children and adding to tree");
		
		// Renumber the children, re-add the new ones, and update the tree
		int index = 0;
		for (EntityData ed : eds) {
			log.debug("processing ed order={}",ed.getOrderIndex());
			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
				ed.setOrderIndex(index);
				
				// Remember actual entities
				Entity parent = ed.getParentEntity();
				Entity child = ed.getChildEntity();
				
				// For performance reasons, we have to replace the parent with a fake id-only entity. Otherwise, it 
				// tries to transfer the entire object graph every time, and it gradually grinds to a halt, since the 
				// object graph grows at some insane rate.
				Entity fakeParentEntity = new Entity();
				fakeParentEntity.setId(parentEntity.getId());
				ed.setParentEntity(fakeParentEntity);

				Entity fakeChildEntity = new Entity();
				fakeChildEntity.setId(child.getId());
				ed.setChildEntity(fakeChildEntity);
				
				log.debug("will save ED {} with index={}",ed.getId(),index);
				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
				log.debug("saved ED {}",savedEd.getId());
				
				// Restore actual entities
				ed.setParentEntity(parent);
				ed.setChildEntity(child);
			}
			index++;
		}
		
		// Remove old eds, if the target is the same
		if (MOVE_WHEN_REORDERING) {
			for (DefaultMutableTreeNode childNode : toRemove) {
	            EntityData ed = entityOutline.getEntityData(childNode);
				ModelMgr.getModelMgr().removeEntityData(ed);
				log.debug("removed old ED {}",ed.getId());
			}
		}
	}
    
    /**
     * Add the given entities to the specified alignment board, if possible.
     * @param alignmentBoardContext
     * @param entitiesToAdd
     */
    protected void addEntities(AlignmentBoardContext alignmentBoardContext, List<RootedEntity> entitiesToAdd) throws Exception {
        for(RootedEntity rootedEntity : entitiesToAdd) {
            alignmentBoardContext.addRootedEntity(rootedEntity);
        }
    }

    protected DataFlavor getNodesFlavor() {
        return nodesFlavor;
    }

    /**
	 * List of entities being transferred.  
	 */
	public class TransferableEntityList implements Transferable {
		
		private List<RootedEntity> entities;

		public TransferableEntityList(RootedEntity rootedEntity) {
			this.entities = new ArrayList<RootedEntity>();
			entities.add(rootedEntity);
		}
		
		public TransferableEntityList(List<RootedEntity> rootedEntities) {
			this.entities = rootedEntities;
		}
		
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return entities;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}
}