package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Support for dragging entities and dropping them onto the EntityOutline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityTransferHandler extends TransferHandler {

	private static final boolean DEBUG = false;
	
	private EntityOutline entityOutline;
	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	
	public EntityTransferHandler() {
		this.entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
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
			if (!support.isDataFlavorSupported(nodesFlavor)) return false;
			
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
				for(Long entityId : ModelMgr.getModelMgr().getSelectedEntitiesIds()) {
	            	String uniqueId = entityOutline.getChildUniqueIdWithEntity(entityId);
	            	DefaultMutableTreeNode node = entityOutline.getNodeByUniqueId(uniqueId);
	            	sourcePaths.add(new TreePath(node.getPath()));
				}				
			}
			else {
				throw new IllegalStateException("Unknown component for transfer: "+sourceComponent.getClass().getName());
			}

			// Can't moved to the root
			if (targetNode.isRoot()) return false;
			
			for(TreePath sourcePath : sourcePaths) {

				// Can't move to a descendant
				if (sourcePath.isDescendant(targetPath)) return false;

				// Can't move the root
				DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)sourcePath.getLastPathComponent();
				if (sourceNode.isRoot()) return false;
				
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)sourceNode.getParent();
				int sourceIndex = parentNode.getIndex(sourceNode);
				int targetIndex = dl.getChildIndex();
				
				// Do not allow a drop in the same location 
				if ((targetPath.equals(sourcePath) || targetPath.equals(sourcePath.getParentPath())) 
						&& (sourceIndex==targetIndex || (targetIndex<0 && sourceIndex==parentNode.getChildCount()-1))) {
					return false;
				}
			}
			
			// Do not allow a drop on the drag source selections
			int dropRow = tree.getRowForPath(targetPath);
			for (int i = 0; i < selRows.length; i++) {
				if (selRows[i] == dropRow) {
					return false;
				}
			}
	
			// Enforce some Entity-specific rules
			List<Entity> entities = (List<Entity>)support.getTransferable().getTransferData(nodesFlavor);
			if (!allowTransfer(targetNode, entities)) {
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
			DefaultMutableTreeNode nextParent = targetNode;
			while (nextParent != null) {
				Entity ancestor = entityOutline.getEntity(nextParent);
				if (Utils.areSameEntity(entity, ancestor)) {
					if (DEBUG) System.out.println("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
			
			// Disallow multiple instances of the same entity
			for(Entity child : targetEntity.getChildren()) {
				if (Utils.areSameEntity(entity, child)) {
					if (DEBUG) System.out.println("Disallow transfer because target already has this child");
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	protected Transferable createTransferable(JComponent sourceComponent) {

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
			
			for(Long entityId : ModelMgr.getModelMgr().getSelectedEntitiesIds()) {
				Set<Entity> matchingEntities = entityOutline.getEntitiesById(entityId);
				if (matchingEntities.isEmpty()) {
					throw new IllegalStateException("Entity not found in entity tree: "+entityId);
				}
				entityList.add(matchingEntities.iterator().next());
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
				index = parent.getChildCount();
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
			int i = destIndex;
			for(Entity entity : entitiesToAdd) {
				System.out.println("Will add "+entity.getName()+" to "+parentEntity.getName()+" at "+i);
				i++;
			}
		}

		// First update the entity model
		List<EntityData> eds = EntityUtils.getOrderedEntityDataOfType(parentEntity, EntityConstants.ATTRIBUTE_ENTITY);
		int origSize = eds.size();
		List<EntityData> newEds = new ArrayList<EntityData>();
		Entity targetEntity = entityOutline.getEntity(targetNode);
		
		int currIndex = destIndex;		
		for(Entity entity : entitiesToAdd) {
			// Add the entity to the new parent, generating the ED
			EntityData newEd = targetEntity.addChildEntity(entity);
			// Temporarily remove the ED so that it can be inserted with the correct index
			targetEntity.getEntityData().remove(newEd);
			newEds.add(newEd);
			
			if (destIndex > origSize) {
				eds.add(newEd);
			} 
			else {
				eds.add(currIndex++, newEd);
			}
		}

		// Renumber the children, re-add the new ones, and update the tree
		int index = 0;
		for (EntityData ed : eds) {
			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
				ed.setOrderIndex(index);
				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
				if ((index >= destIndex) && (index < destIndex+entitiesToAdd.size())) {
					// Re-add the saved entity data to the parent
					targetEntity.getEntityData().add(savedEd);
					// Now add to the outline
					entityOutline.addNodes(targetNode, savedEd, index);
				}
			}
			index++;
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