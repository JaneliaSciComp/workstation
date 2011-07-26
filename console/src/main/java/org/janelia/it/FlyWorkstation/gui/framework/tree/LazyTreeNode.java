package org.janelia.it.FlyWorkstation.gui.framework.tree;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A node which has unloaded children. If the node is opened, those children should be loaded.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LazyTreeNode extends DefaultMutableTreeNode {

	@Override
	public String toString() {
		return "LazyTreeNode";
	}

}
