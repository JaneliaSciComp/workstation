package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerManager.Provider;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from CustomTreeView in com.nbtaskfocus.core.
 */
public class CustomTreeView extends BeanTreeView {
    
    private final static Logger log = LoggerFactory.getLogger(CustomTreeView.class);
    
    private final ExplorerManager.Provider explorerManagerProvider;
    
    public CustomTreeView(ExplorerManager.Provider explorerManagerProvider) {
        this.explorerManagerProvider = explorerManagerProvider;
        setDefaultActionAllowed(false);
        setRootVisible(false);
        tree.setScrollsOnExpand(false);
    }

    public Provider getExplorerManagerProvider() {
        return explorerManagerProvider;
    }
    
    public void scrollToTop() {
        tree.scrollRowToVisible(0);
    }
    
//    public void scrollToNode(Node n) {
//        TreeNode tn = Visualizer.findVisualizer(n);
//        if (tn == null) {
//            return;
//        }
//
//        TreeModel model = tree.getModel();
//        if (!(model instanceof DefaultTreeModel)) {
//            return;
//        }
//
//        TreePath path = new TreePath(((DefaultTreeModel) model).getPathToRoot(tn));
//        Rectangle r = tree.getPathBounds(path);
//        if (r != null) {
//            tree.scrollRectToVisible(r);
//        }
//    }

    public List<Long[]> getExpandedPaths() {

        List<Long[]> result = new ArrayList<>();

        TreeNode rtn = Visualizer.findVisualizer(getRootContext());
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

    /**
     * Useful because it doesn't scroll to the expanded path like BeanTreeView's showPath.
     * @param path 
     */
    protected void expandPath(TreePath path) {
        tree.expandPath(path);
    }

    /** 
     * Expands all the paths, when exists
     */
    public void expandNodes(List<Long[]> exPaths) {
        for (Iterator<Long[]> it = exPaths.iterator(); it.hasNext();) {
            Long[] sp = it.next();
            TreePath tp = idPath2TreePath(sp);
            if (tp != null) {
                expandPath(tp);
            }
        }
    }

    private Node getRootContext() {
        return explorerManagerProvider.getExplorerManager().getRootContext();
    }

    private TreePath idPath2TreePath(Long[] sp) {

        Node n = NodeUtils.findPath(getRootContext(), sp);
        if (n==null) return null;

        // Create the tree path
        TreeNode tns[] = new TreeNode[sp.length + 1];

        for (int i = sp.length; i >= 0; i--) {
            tns[i] = Visualizer.findVisualizer(n);
            n = n.getParentNode();
        }
        return new TreePath(tns);
    }
}
