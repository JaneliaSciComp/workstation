package org.janelia.it.workstation.gui.browser.gui.tree;

import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.Position;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerManager.Provider;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some useful functionality on top of the BeanTreeView. 
 * Adapted from CustomTreeView in com.nbtaskfocus.core.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CustomTreeView extends BeanTreeView {
    
    private final static Logger log = LoggerFactory.getLogger(CustomTreeView.class);
    
    private final ExplorerManager.Provider explorerManagerProvider;
    
    public CustomTreeView(ExplorerManager.Provider explorerManagerProvider) {
        this.explorerManagerProvider = explorerManagerProvider;
        setQuickSearchAllowed(false);
        tree.setScrollsOnExpand(false);
    }

    public Provider getExplorerManagerProvider() {
        return explorerManagerProvider;
    }

    public Node getRootNode() {
        return explorerManagerProvider.getExplorerManager().getRootContext();
    }
    
    public Node[] getSelectedNodes() {
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        return mgr.getSelectedNodes();
    }
    
    public Node getCurrentNode() {
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        Node[] selected = mgr.getSelectedNodes();
        if (selected.length>0) {
            return selected[0];
        }
        return null;
    }
    
    public Node getTopNode() {
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        if (tree.isRootVisible()) {
            return mgr.getRootContext();
        }
        else {
            return mgr.getRootContext().getChildren().nodes().nextElement();
        }
    }
    
    public void navigateToNextRow() {
        Node curr = getCurrentNode();
        CustomTreeFind find = new CustomTreeFind(this, null, curr, Position.Bias.Forward, true);
        Node node = find.find();
        selectNode(node);
    }
        
    public void scrollToTop() {
        tree.scrollRowToVisible(0);
    }
    
    public void scrollToNode(Node n) {
        TreeNode tn = Visualizer.findVisualizer(n);
        if (tn == null) {
            return;
        }

        TreeModel model = tree.getModel();
        if (!(model instanceof DefaultTreeModel)) {
            return;
        }

        TreePath path = new TreePath(((DefaultTreeModel) model).getPathToRoot(tn));
        Rectangle r = tree.getPathBounds(path);
        if (r != null) {
            tree.scrollRectToVisible(r);
        }
    }

    public List<Long[]> getSelectedPaths() {
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        List<Long[]> paths = new ArrayList<>();
        for(Node node : mgr.getSelectedNodes()) {
            Long[] path = NodeUtils.createIdPath(node);
            if (path!=null) paths.add(path);
        }
        return paths;
    }
    
    public List<Long[]> getExpandedPaths() {

        List<Long[]> result = new ArrayList<>();

        TreeNode rtn = Visualizer.findVisualizer(getRootNode());
        TreePath tp = new TreePath(rtn); // Get the root

        Enumeration<TreePath> paths = tree.getExpandedDescendants(tp);
        if (null != paths) {
            while (paths.hasMoreElements()) {
                TreePath ep = paths.nextElement();
                Node en = Visualizer.findNode(ep.getLastPathComponent());
                Long[] path = NodeUtils.createIdPath(en);
                result.add(path);
            }
        }

        return result;
    }
    
    public void selectNode(Node node) {
        if (node==null) return;
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        try {
            log.info("Selecting node: {}",node.getDisplayName());
            Node[] nodes = { node };
            mgr.setSelectedNodes(nodes);
        }
        catch (PropertyVetoException e) {
            log.error("Node selection was vetoed",e);
        }
    }
    
    public void selectPaths(List<Long[]> paths) {
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        try {
            List<Node> nodes = new ArrayList<>();
            for(Long[] path : paths) {
                Node node = NodeUtils.findNodeWithPath(mgr.getRootContext(), path);
                log.info("Selecting node: {}",node.getDisplayName());
                nodes.add(node);
            }        
            Node[] ar = new Node[nodes.size()];
            nodes.toArray(ar);
            mgr.setSelectedNodes(ar);
        }
        catch (PropertyVetoException e) {
            log.error("Node selection was vetoed",e);
        }
    }
    
    public void selectTopNode() {
        selectNode(getTopNode());
    }
    
    /**
     * Select the node containing the given search string. If bias is null then we search forward starting with the
     * current node. If the current node contains the searchString then we don't move. If the bias is Forward then we
     * start searching in the node after the selected one. If bias is Backward then we look backwards from the node
     * before the selected one.
     *
     * @param searchString
     * @param bias
     */
    public void navigateToNodeStartingWith(String searchString, Position.Bias bias, boolean skipStartingNode) {
        CustomTreeFind searcher = new CustomTreeFind(this, searchString, getCurrentNode(), bias, skipStartingNode);
        selectNode(searcher.find());
    }
    
    /**
     * Expands the given path. Useful because it doesn't scroll to the 
     * expanded path like BeanTreeView's showPath.
     * @param path 
     */
    public void expand(TreePath path) {
        tree.expandPath(path);
    }

    /**
     * Expand the given path.
     */
    public void expand(Long[] idPath) {
        List<Long[]> paths = new ArrayList<>();
        paths.add(idPath);
        expand(paths);
    }
    
    /** 
     * Expand all the given paths.
     */
    public void expand(List<Long[]> paths) {
        for (Iterator<Long[]> it = paths.iterator(); it.hasNext();) {
            Long[] path = it.next();
            TreePath tp = getTreePath(path);
            log.debug("Expanding {}",tp);
            if (tp != null) {
                expand(tp);
            }
        }
    }

    private TreePath getTreePath(Long[] path) {
        Node n = NodeUtils.findNodeWithPath(getRootNode(), path);
        if (n==null) return null;

        LinkedList<TreeNode> treeNodes = new LinkedList<>();
        
        while (n != null) {
            treeNodes.addFirst(Visualizer.findVisualizer(n));
            n = n.getParentNode();
        }
        
        TreeNode[] tns = new TreeNode[treeNodes.size()];
        treeNodes.toArray(tns);
        return new TreePath(tns);
    }
    
    public void replaceKeyListeners(KeyListener newListener) {
        for(KeyListener listener : getKeyListeners()) {
            removeKeyListener(listener);
        }
        for(KeyListener listener : tree.getKeyListeners()) {
            tree.removeKeyListener(listener);
        }
        tree.addKeyListener(newListener);
    }
    
    @Override
    public void grabFocus() {
        tree.grabFocus();
        if (getCurrentNode() == null) {
            selectTopNode();
        }
    }
}
