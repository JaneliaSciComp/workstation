package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 * Support for drag and drop operations within DynamicTrees.
 * 
 * Code adapted from http://www.coderanch.com/t/346509/GUI/java/JTree-drag-drop-inside-one
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TreeTransferHandler extends TransferHandler {

	private DynamicTree dynamicTree;
	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	private DefaultMutableTreeNode nodeToRemove;
	
	public TreeTransferHandler(DynamicTree dynamicTree) {
		this.dynamicTree = dynamicTree;
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
					+ javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} 
		catch (ClassNotFoundException e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		
		if (!support.isDrop()) return false;
		support.setShowDropLocation(true);
		
		if (!support.isDataFlavorSupported(nodesFlavor)) return false;

		JTree tree = (JTree) support.getComponent();
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		int[] selRows = tree.getSelectionRows();
		
		TreePath source = tree.getPathForRow(selRows[0]);
		TreePath dest = dl.getPath();

		// Can't move to a descendant
		if (source.isDescendant(dest)) {
			return false;
		}
		
		// Do not allow a drop on the drag source selections.
		int dropRow = tree.getRowForPath(dest);
		for (int i = 0; i < selRows.length; i++) {
			if (selRows[i] == dropRow) {
				return false;
			}
		}

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)source.getLastPathComponent();
		if (node.isRoot()) return false;

		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)node.getParent();
		int sourceIndex = parentNode.getIndex(node);
		int destIndex = dl.getChildIndex();

//		System.out.println(" "+source+"("+sourceIndex+") -> "+dest+"("+destIndex+")");
		
		// Do not allow a drop in the same location 
		if ((dest.equals(source) || dest.equals(source.getParentPath())) 
				&& (sourceIndex==destIndex || (destIndex<0 && sourceIndex==parentNode.getChildCount()-1))) {
			return false;
		}

		DefaultMutableTreeNode destNode = (DefaultMutableTreeNode)dest.getLastPathComponent();
		if (!allowTransfer(node, destNode)) {
			return false;
		}
		
		return true;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null) return null;
		
		if (paths.length>1) throw new IllegalStateException("Only single node selection is supported");
		
		// Make up a node array of copies for transfer and another for/of the nodes that will be removed in
		// exportDone after a successful drop.
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
		DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());

		nodeToRemove = node;
		return new TransferableNode(copy);
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {

		if (!canImport(support)) return false;
		
		try {
			// Extract transfer data.
			Transferable t = support.getTransferable();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) t.getTransferData(nodesFlavor);
		
			// Get drop location info.
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			int childIndex = dl.getChildIndex();
			TreePath dest = dl.getPath();
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
			
			// Configure for drop mode.
			int index = childIndex; // DropMode.INSERT
			if (childIndex == -1) { // DropMode.ON
				index = parent.getChildCount();
			}
					
			// Add data to model
			if (!updateUserData(nodeToRemove, node, parent, index)) return false;
			
			// Add node to the tree
			if (!addNode(parent, node, index)) return false;
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return false;
		}
		
		return true;
	}

	protected abstract boolean allowTransfer(DefaultMutableTreeNode node, DefaultMutableTreeNode destination);
	
	protected abstract boolean updateUserData(DefaultMutableTreeNode nodeRemoved, DefaultMutableTreeNode nodeAdded, DefaultMutableTreeNode newParent, int index);
	
	protected abstract boolean addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode node, int index);
	
	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		if ((action & MOVE) == MOVE) {
			// Remove the old nodes
			dynamicTree.removeNode(nodeToRemove, true);
		}
	}
	
	public class TransferableNode implements Transferable {
		DefaultMutableTreeNode node;

		public TransferableNode(DefaultMutableTreeNode node) {
			this.node = node;
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return node;
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