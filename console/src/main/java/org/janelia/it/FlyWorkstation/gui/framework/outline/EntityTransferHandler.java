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
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Support for dragging entities and dropping them onto the EntityOutline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityTransferHandler extends TransferHandler {

	private static final boolean MOVE_WHEN_REORDERING = true;
	
	private static final boolean DEBUG = false;
	
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
	public boolean canImport(TransferHandler.TransferSupport support) {
		
		try {
			// Only dealing with drag and drop for now
			if (!support.isDrop()) return false;
			if (!support.isDataFlavorSupported(nodesFlavor)) {
				if (DEBUG) System.out.println("Disallow transfer because data flavor "+nodesFlavor.getMimeType()+" is not supported");
				return false;
			}
			
			support.setShowDropLocation(true);
			
			Component sourceComponent = support.getComponent();
			DropLocation dropLocation = support.getDropLocation();
			JComponent dropTarget = getDropTargetComponent();
			
			if (!(dropTarget instanceof EntityTree)) return false;
			if (!(dropLocation instanceof JTree.DropLocation)) return false;
			
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
            	IconDemoPanel iconDemoPanel = ((AnnotatedImageButton)sourceComponent).getIconDemoPanel();
				for(String selectedId : selectedEntities) {
					RootedEntity rootedEntity = iconDemoPanel.getRootedEntityById(selectedId);
	            	DefaultMutableTreeNode node = entityOutline.getNodeByUniqueId(rootedEntity.getUniqueId());
	            	sourcePaths.add(new TreePath(node.getPath()));
				}				
			}
			else {
				throw new IllegalStateException("Unknown component for transfer: "+sourceComponent.getClass().getName());
			}

			// Can't moved to the root
			if (targetNode.isRoot()) {
				if (DEBUG) System.out.println("Disallow transfer because target node is a root");
				return false;
			}
			
			for(TreePath sourcePath : sourcePaths) {

				// Can't move to a descendant
				if (sourcePath.isDescendant(targetPath)) {
					if (DEBUG) System.out.println("Disallow transfer because source node descendant of target");
					return false;
				}

				// Can't move the root
				DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)sourcePath.getLastPathComponent();
				if (sourceNode.isRoot()) {
					if (DEBUG) System.out.println("Disallow transfer because source node is a root");
					return false;
				}
				
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)sourceNode.getParent();
				int sourceIndex = parentNode.getIndex(sourceNode);
				int targetIndex = dl.getChildIndex();
				
				// Do not allow a drop in the same location 
				if ((targetPath.equals(sourcePath) || targetPath.equals(sourcePath.getParentPath())) 
						&& (sourceIndex==targetIndex || (targetIndex<0 && sourceIndex==parentNode.getChildCount()-1))) {
					if (DEBUG) System.out.println("Disallow transfer to the same location");
					return false;
				}
			}
			
			// Do not allow a drop on the drag source selections
			int dropRow = tree.getRowForPath(targetPath);
			for (int i = 0; i < selRows.length; i++) {
				if (selRows[i] == dropRow) {
					if (DEBUG) System.out.println("Disallow transfer drag and drop rows are identical");
					return false;
				}
			}
	
			// Enforce some Entity-specific rules
			List<Entity> entities = (List<Entity>)support.getTransferable().getTransferData(nodesFlavor);
			if (!allowTransfer(targetNode, entities)) {
				if (DEBUG) System.out.println("Disallow transfer because of entity rules");
				return false;
			}
			
			return true;
			
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return false;
		}
		
	}

	/**
	 * Test if a transfer of a given entities to the target parent node is allowed.
	 * @param targetNode the new parent for the entities
	 * @param entitiesToAdd list of entities to add to the the new parentv
	 * @return true if transfer is allowed
	 */
	protected boolean allowTransfer(DefaultMutableTreeNode targetNode, List<Entity> entitiesToAdd) {
		
		// Disallow transfer if target node is not owned by the user
		Entity targetEntity = entityOutline.getEntity(targetNode);
		if (!ModelMgrUtils.isOwner(targetEntity)) {
			if (DEBUG) System.out.println("Disallow transfer because user is not owner of target");
			return false;
		}

		for(Entity entity : entitiesToAdd) {
			// Disallow transfer if the entity is in the ancestor chain
			DefaultMutableTreeNode nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			if (nextParent!=null) {
				nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			}
			while (nextParent != null) {
				Entity ancestor = entityOutline.getEntity(nextParent);
				if (Utils.areSameEntity(entity, ancestor)) {
					if (DEBUG) System.out.println("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
			
			// Disallow multiple instances of the same entity
//			for(Entity child : targetEntity.getChildren()) {
//				if (Utils.areSameEntity(entity, child)) {
//					if (DEBUG) System.out.println("Disallow transfer because target already has this child");
//					return false;
//				}
//			}
		}
		
		return true;
	}
	
	@Override
	protected Transferable createTransferable(JComponent sourceComponent) {

		if (DEBUG) System.out.println("EntityTransferHandler.createTransferable");
		
		List<Entity> entityList = new ArrayList<Entity>();
		
		if (sourceComponent instanceof JTree) {
			
			JTree tree = (JTree) sourceComponent;
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null) return null;
			
			for(TreePath path : paths) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				entityList.add(entityOutline.getEntity(node));
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
				entityList.add(matchingEntity.getEntity());
			}		

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
	public boolean importData(TransferHandler.TransferSupport support) {

		if (DEBUG) System.out.println("EntityTransferHandler.importData");
		
		if (!canImport(support)) return false;

		try {
			// Extract transfer data
			Transferable t = support.getTransferable();
			List<Entity> entities = (List<Entity>)t.getTransferData(nodesFlavor);
		
			// Get drop location info
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			int childIndex = dl.getChildIndex();
			TreePath targetPath = dl.getPath();
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
			
			// Get drop index
			int index = childIndex; // DropMode.INSERT
			if (childIndex == -1) { // DropMode.ON
				EntityData parentEd = (EntityData)parent.getUserObject();
				Entity parentEntity = parentEd.getChildEntity();
				index = parentEntity.getChildren().size();
			}
					
			// Actually perform the transfer
			addEntities(parent, entities, index);
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
	protected void addEntities(DefaultMutableTreeNode targetNode, List<Entity> entitiesToAdd, int destIndex) throws Exception {

		Entity parentEntity = entityOutline.getEntity(targetNode);
		
		if (DEBUG) {
			System.out.println("EntityTransferHandler.addEntities");
			int i = destIndex;
			for(Entity entity : entitiesToAdd) {
				System.out.println("Will add "+entity.getName()+" to "+parentEntity.getName()+" at "+i);
				i++;
			}
			System.out.println("EntityTransferHandler.addEntities - updating entity model");
		}

		// First update the entity model
		
		List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();		
		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(parentEntity);
		
		int origSize = eds.size();
		int currIndex = destIndex;		
		
		for(Entity entity : entitiesToAdd) {
			// Add the entity to the new parent, generating the ED
			EntityData newEd = parentEntity.addChildEntity(entity);
			if (DEBUG) System.out.println("EntityTransferHandler.addEntities - created new child "+entity.getName());
			
			// Temporarily remove the ED so that it can be inserted with the correct index
			parentEntity.getEntityData().remove(newEd);
			
			if (destIndex > origSize) {
				eds.add(newEd);
			} 
			else {
				eds.add(currIndex++, newEd);
			}

			if (MOVE_WHEN_REORDERING) {
		        Enumeration enumeration = targetNode.children();
		        while (enumeration.hasMoreElements()) {
		            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
		            EntityData ed = entityOutline.getEntityData(childNode);
					if (ed!=null && ed.getChildEntity()!=null && Utils.areSameEntity(entity, ed.getChildEntity())) {
						toRemove.add(childNode);
					}
		        }
			}
		}

		if (DEBUG) System.out.println("EntityTransferHandler.addEntities - renumbering children and adding to tree");
		
		// Renumber the children, re-add the new ones, and update the tree
		int index = 0;
		for (EntityData ed : eds) {
			if (DEBUG) System.out.println("EntityTransferHandler.addEntities - processing ed order="+ed.getOrderIndex()+" ");
			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
				ed.setOrderIndex(index);
				// For performance reasons, we have to replace the parent with a fake id-only entity. Otherwise, it 
				// tries to transfer the entire object graph every time, and it gradually grinds to a halt, since the 
				// object graph grows at some insane rate.
				Entity fakeParentEntity = new Entity();
				fakeParentEntity.setId(parentEntity.getId());
				ed.setParentEntity(fakeParentEntity);

				if (DEBUG) System.out.println("EntityTransferHandler.addEntities - will save ED "+ed.getId()+" with index="+index+" child:"+ed.getChildEntity().getName()+" value:"+ed.getValue());
				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
				if (DEBUG) System.out.println("EntityTransferHandler.addEntities - saved ED "+savedEd.getId()+" with index="+index);

				if ((index >= destIndex) && (index < destIndex+entitiesToAdd.size())) {
					// Re-add the saved entity data to the parent
					parentEntity.getEntityData().add(savedEd);
					// Now add to the outline, if its not lazy
					if (entityOutline.getDynamicTree().childrenAreLoaded(targetNode)) {
						entityOutline.addNodes(targetNode, savedEd, index);
					}
				}
			}
			index++;
		}
		
		// Remove old eds, if the target is the same
		if (MOVE_WHEN_REORDERING) {
			for (DefaultMutableTreeNode childNode : toRemove) {
	            EntityData ed = entityOutline.getEntityData(childNode);
	            if (!parentEntity.getEntityData().remove(ed)) {
	            	System.out.println("EntityTransferHandler.addEntities - could not remove ED "+ed.getId());
	            }
				ModelMgr.getModelMgr().removeEntityData(ed);
				if (DEBUG) System.out.println("EntityTransferHandler.addEntities - removed old ED "+ed.getId());
				entityOutline.getDynamicTree().removeNode(childNode);
			}
		}
	}
	
	/**
	 * List of entities being transferred.  
	 */
	public class TransferableEntityList implements Transferable {
		
		private List<Entity> entities;

		public TransferableEntityList(Entity entity) {
			this.entities = new ArrayList<Entity>();
			entities.add(entity);
		}
		
		public TransferableEntityList(List<Entity> entities) {
			this.entities = entities;
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