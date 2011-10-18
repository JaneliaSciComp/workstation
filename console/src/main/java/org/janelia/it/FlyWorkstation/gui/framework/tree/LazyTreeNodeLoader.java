package org.janelia.it.FlyWorkstation.gui.framework.tree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;

/**
 * A worker thread which loads the children or ancestors for a node in a DynamicTree. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LazyTreeNodeLoader extends SimpleWorker {

    private final DynamicTree dynamicTree;
    private final DefaultMutableTreeNode node;
    private final boolean recurse;
    private final ExpansionState expansionState;

    public LazyTreeNodeLoader(DynamicTree dynamicTree, DefaultMutableTreeNode node, boolean recurse) {
        this.dynamicTree = dynamicTree;
        this.node = node;
        this.recurse = recurse;
        this.expansionState = new ExpansionState();
        expansionState.storeExpansionState(dynamicTree, node);
    }

    /**
     * This is an alternate way of invoking the loader without spawning a new thread. This method should be used instead
     * of execute if the caller is already running in a worker thread. The doneLoading() callback will still get called.
     * @throws Exception
     */
    public void loadSynchronously() throws Exception {
        dynamicTree.loadLazyNodeData(node, recurse);
        dynamicTree.recreateChildNodes(node, recurse);
        expansionState.restoreExpansionState(dynamicTree, node);
        doneLoading();
    }
    
    @Override
    protected void doStuff() throws Exception {
        dynamicTree.loadLazyNodeData(node, recurse);
    }

    @Override
    protected void hadSuccess() {
        dynamicTree.recreateChildNodes(node, recurse);
        expansionState.restoreExpansionState(dynamicTree, node);
        doneLoading();
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