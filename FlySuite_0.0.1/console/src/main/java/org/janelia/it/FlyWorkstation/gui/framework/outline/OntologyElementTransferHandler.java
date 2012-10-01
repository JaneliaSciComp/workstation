package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Support for dragging ontology elements in the ontology outline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementTransferHandler extends TransferHandler {
	
	private static final boolean DEBUG = false;
	
	private OntologyOutline ontologyOutline;
	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	
	public OntologyElementTransferHandler(OntologyOutline ontologyOutline) {
		this.ontologyOutline = ontologyOutline;
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
					+ DefaultMutableTreeNode.class.getName() + "\"";
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
			
			if (!(dropTarget instanceof OntologyTree)) return false;
			if (!(dropLocation instanceof JTree.DropLocation)) return false;
			
			JTree tree = ontologyOutline.getTree();
			int[] selRows = tree.getSelectionRows();			
			JTree.DropLocation dl = (JTree.DropLocation)dropLocation;
			TreePath targetPath = dl.getPath();
			DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)targetPath.getLastPathComponent();
			
			// Derive unique TreePaths for the entities. It will allow us to enforce some tree-based rules.
			List<TreePath> sourcePaths = new ArrayList<TreePath>();			
			if (sourceComponent instanceof JTree) {
				sourcePaths.add(tree.getPathForRow(selRows[0]));
			}
			else {
				throw new IllegalStateException("Unknown component for transfer: "+sourceComponent.getClass().getName());
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
			List<OntologyElement> elements = getElements((List<DefaultMutableTreeNode>)support.getTransferable().getTransferData(nodesFlavor));
			if (!allowTransfer(targetNode, elements)) {
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
	protected boolean allowTransfer(DefaultMutableTreeNode targetNode, List<OntologyElement> elementsToAdd) {
		
		// Disallow transfer if target node is not owned by the user
		OntologyElement targetElement = ontologyOutline.getElement(targetNode);
		OntologyElementType type = targetElement.getType();

		if (type==null) {
			if (DEBUG) System.out.println("Disallow transfer because the target does not have an ontology element type");
			return false; 
		}
		
		if (!type.allowsChildren()) {
			if (DEBUG) System.out.println("Disallow transfer because the target does not support children");
			return false; 
		}
		
		if (type instanceof org.janelia.it.jacs.model.ontology.types.Enum) {
			for(OntologyElement element : elementsToAdd) {
				if (!(element.getType() instanceof org.janelia.it.jacs.model.ontology.types.EnumItem)) {
					if (DEBUG) System.out.println("Disallow transfer because the target is an Enum, but the element is not an EnumItem");
					return false; 
				}
			}
		}
		else {
			for(OntologyElement element : elementsToAdd) {
				if (element.getType() instanceof org.janelia.it.jacs.model.ontology.types.EnumItem) {
					if (DEBUG) System.out.println("Disallow transfer because the target is not an Enum, but the element is an EnumItem");
					return false; 
				}
			}
		}
		
		for(OntologyElement element : elementsToAdd) {
			// Disallow transfer if the entity is in the ancestor chain
			DefaultMutableTreeNode nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			if (nextParent!=null) {
				nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			}
			while (nextParent != null) {
				OntologyElement ancestor = ontologyOutline.getElement(nextParent);
				if (Utils.areSameEntity(element.getEntity(), ancestor.getEntity())) {
					if (DEBUG) System.out.println("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
		}
		
		return true;
	}
	
	@Override
	protected Transferable createTransferable(JComponent sourceComponent) {

		if (DEBUG) System.out.println("EntityTransferHandler.createTransferable");
		
		List<DefaultMutableTreeNode> nodeList = new ArrayList<DefaultMutableTreeNode>();
		
		if (sourceComponent instanceof JTree) {
			
			JTree tree = (JTree) sourceComponent;
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null) return null;
			
			for(TreePath path : paths) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				nodeList.add(node);
			}
		}
		else {
			throw new IllegalStateException("Unsupported component type for transfer: "+sourceComponent.getClass().getName());
		}
		
		return new TransferableTreeNodeList(nodeList);
	}
	
	private List<OntologyElement> getElements(List<DefaultMutableTreeNode> nodes) {
		List<OntologyElement> elementList = new ArrayList<OntologyElement>();
		for(DefaultMutableTreeNode node : nodes) {
			elementList.add(ontologyOutline.getElement(node));
		}
		return elementList;
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
			List<DefaultMutableTreeNode> nodes = (List<DefaultMutableTreeNode>)t.getTransferData(nodesFlavor);
		
			// Get drop location info
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			int childIndex = dl.getChildIndex();
			TreePath targetPath = dl.getPath();
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
			
			// Get drop index
			int index = childIndex; // DropMode.INSERT
			if (childIndex == -1) { // DropMode.ON
				OntologyElement targetElement = (OntologyElement)parent.getUserObject();
				Entity parentEntity = targetElement.getEntity();
				index = parentEntity.getChildren().size();
			}
					
			// Actually perform the transfer
			addNodes(parent, nodes, index);
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
	 * @param elementsToAdd list of entities to add to the the new parent
	 * @param destIndex child insertion index in the new parent
	 * @throws Exception
	 */
	protected void addNodes(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> nodesToAdd, int destIndex) throws Exception {

		List<OntologyElement> elementsToAdd = getElements(nodesToAdd);
		
		OntologyElement parentElement = ontologyOutline.getElement(targetNode);
		Entity parentEntity = parentElement.getEntity();
		
		if (DEBUG) {
			System.out.println("EntityTransferHandler.addEntities");
			int i = destIndex;
			for(OntologyElement element : elementsToAdd) {
				System.out.println("Will add "+element.getName()+" to "+parentElement.getName()+" at "+i);
				i++;
			}
			System.out.println("EntityTransferHandler.addEntities - updating entity model");
		}

		// First update the entity model
		
		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(parentElement.getEntity());
		List<EntityData> toDelete = new ArrayList<EntityData>();
		
		int origSize = eds.size();
		int currIndex = destIndex;		

		for (DefaultMutableTreeNode childNode : nodesToAdd) {
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)childNode.getParent();
			Entity parent = ontologyOutline.getEntity(parentNode);
            Entity entity = ontologyOutline.getEntity(childNode);

            // Remove nodes from current parent
            for(EntityData ed : new ArrayList<EntityData>(parent.getEntityData())) {
	            if (Utils.areSameEntity(entity,ed.getChildEntity())) {	
	                if (!parent.getEntityData().remove(ed)) {
	                	System.out.println("EntityTransferHandler.addEntities - could not remove ED "+ed.getId());
	                }
	                else {
	                	toDelete.add(ed);
	                }
	            }   	
            }
            
			// Add the entity to the new parent, generating the ED
			EntityData newEd = parentEntity.addChildEntity(entity, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
			if (DEBUG) System.out.println("EntityTransferHandler.addEntities - added new child "+entity.getName()+" with attribute type "+newEd.getEntityAttribute().getName());
			
			// Temporarily remove the ED so that it can be inserted with the correct index
			parentEntity.getEntityData().remove(newEd);
			
			if (destIndex > origSize) {
				eds.add(newEd);
			} 
			else {
				eds.add(currIndex++, newEd);
			}
		}
        
		if (DEBUG) System.out.println("EntityTransferHandler.addEntities - renumbering children and adding to tree");
		
		// Renumber the children, re-add the new ones, and update the tree
		int index = 0;
		for (EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			
			if (DEBUG) System.out.println("  EntityTransferHandler.addEntities - processing ed order="+ed.getOrderIndex()+" (should be "+index+") child:"+child.getName()+" attrType:"+ed.getEntityAttribute().getName());
			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
				ed.setOrderIndex(index);
				// For performance reasons, we have to replace the parent with a fake id-only entity. Otherwise, it 
				// tries to transfer the entire object graph every time, and it gradually grinds to a halt, since the 
				// object graph grows at some insane rate.
				Entity fakeParentEntity = new Entity();
				fakeParentEntity.setId(parentElement.getId());
				ed.setParentEntity(fakeParentEntity);

				if (DEBUG) System.out.println("    EntityTransferHandler.addEntities - will save ED "+ed.getId()+" with index="+index+" value:"+ed.getValue());
				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
				if (DEBUG) System.out.println("    EntityTransferHandler.addEntities - saved ED "+savedEd.getId()+" with index="+index);

				if ((index >= destIndex) && (index < destIndex+elementsToAdd.size())) {
					// Re-add the saved entity data to the parent
					parentEntity.getEntityData().add(savedEd);
					// Now add to the outline, if its not lazy
					if (ontologyOutline.getDynamicTree().childrenAreLoaded(targetNode)) {
						OntologyElement newElement = new OntologyElement(ed.getChildEntity(), parentElement);
						ontologyOutline.addNodes(targetNode, newElement, index);
					}
				}
			}
			index++;
		}
		
		// Delete old eds and update the tree
		for(EntityData ed : toDelete) {
			ModelMgr.getModelMgr().removeEntityData(ed);
			if (DEBUG) System.out.println("EntityTransferHandler.addEntities - removed old ED "+ed.getId());
		}
		for (DefaultMutableTreeNode childNode : nodesToAdd) {
			ontologyOutline.getDynamicTree().removeNode(childNode);
		}
		
	}
	
	/**
	 * List of entities being transferred.  
	 */
	public class TransferableTreeNodeList implements Transferable {
		
		private List<DefaultMutableTreeNode> nodes;

		public TransferableTreeNodeList(DefaultMutableTreeNode node) {
			this.nodes = new ArrayList<DefaultMutableTreeNode>();
			nodes.add(node);
		}
		
		public TransferableTreeNodeList(List<DefaultMutableTreeNode> nodes) {
			this.nodes = nodes;
		}
		
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return nodes;
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