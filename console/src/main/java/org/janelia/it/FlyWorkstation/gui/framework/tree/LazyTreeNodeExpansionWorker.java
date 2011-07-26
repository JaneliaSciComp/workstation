package org.janelia.it.FlyWorkstation.gui.framework.tree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;

/**
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LazyTreeNodeExpansionWorker extends SimpleWorker {
	
	private final DynamicTree dynamicTree;
	private final DefaultMutableTreeNode node;
	private final boolean recurse;
	
	public LazyTreeNodeExpansionWorker(DynamicTree dynamicTree, DefaultMutableTreeNode node, boolean recurse) {
		this.dynamicTree = dynamicTree;
		this.node = node;
		this.recurse = recurse;
	}
	
	@Override
	protected void doStuff() throws Exception {
		dynamicTree.loadLazyNodeData(node, recurse);
	}

	@Override
	protected void hadSuccess() {
		dynamicTree.recreateChildNodes(node, recurse);
		doneExpanding();
	}

	@Override
	protected void hadError(Throwable error) {
		error.printStackTrace();
//		JOptionPane.showMessageDialog(EntityTree.this, "Error expanding tree", "Internal Error", JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Override this method to do something once the node has been expanded. Called in the EDT.
	 */
	protected void doneExpanding() {
	}
}