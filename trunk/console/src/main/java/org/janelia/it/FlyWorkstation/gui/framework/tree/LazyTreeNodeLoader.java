package org.janelia.it.FlyWorkstation.gui.framework.tree;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;

/**
 * A worker thread which loads the children or ancestors for a node in a DynamicTree. 
 * 
 * TODO: this class can probably be removed because most of the complexity it encapsulated has been rendered obsolete
 * by the new EntityModel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LazyTreeNodeLoader extends SimpleWorker {

    private final DynamicTree dynamicTree;
    private final DefaultMutableTreeNode node;

    public LazyTreeNodeLoader(DynamicTree dynamicTree, DefaultMutableTreeNode node) {
        this.dynamicTree = dynamicTree;
        this.node = node;
    }

    /**
     * This is an alternate way of invoking the loader without spawning a new thread. This method should be used instead
     * of execute if the caller is already running in a worker thread. The doneLoading() callback will still get called.
     * @throws Exception
     */
    public void loadSynchronously() throws Exception {
        dynamicTree.loadLazyNodeData(node);
        doneLoading();
    }
    
    @Override
    protected void doStuff() throws Exception {
		dynamicTree.loadLazyNodeData(node);	
    }

    @Override
    protected void hadSuccess() {
    	// Queue this up so that it runs after all entity events have a chance to resolve
    	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doneLoading();
			}
		});
    	
    }

    @Override
    protected void hadError(Throwable error) {
        error.printStackTrace();
    }

    /**
     * Override this method to do something once the nodes have been loaded. Called in the EDT.
     */
    protected void doneLoading() {
    }
}