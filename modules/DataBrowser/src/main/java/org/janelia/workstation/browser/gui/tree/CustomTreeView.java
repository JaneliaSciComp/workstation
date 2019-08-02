package org.janelia.workstation.browser.gui.tree;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.Position;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.janelia.workstation.common.nodes.NodeUtils;
import org.openide.awt.MouseUtils;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerManager.Provider;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.TreeView;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOp;
import org.openide.util.Utilities;
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
    private final ExplorerManager manager;
    
    public CustomTreeView(ExplorerManager.Provider explorerManagerProvider) {
        this.explorerManagerProvider = explorerManagerProvider;
        this.manager = explorerManagerProvider.getExplorerManager();
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

        Set<Long[]> result = new LinkedHashSet<>();

        TreeNode rtn = Visualizer.findVisualizer(getRootNode());
        TreePath tp = new TreePath(rtn); // Get the root

        Enumeration<TreePath> paths = tree.getExpandedDescendants(tp);
        if (paths != null) {
            while (paths.hasMoreElements()) {
                TreePath ep = paths.nextElement();
                Node en = Visualizer.findNode(ep.getLastPathComponent());
                Long[] path = NodeUtils.createIdPath(en);
                if (path!=null && path.length>0) {
                    log.debug("Adding expanded path " + NodeUtils.createPathString(en));
                    result.add(path);
                }
            }
        }

        return new ArrayList<>(result);
    }
    
    public void selectNode(Node node) {
        if (node==null) return;
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        try {
            log.debug("Selecting node: {}",node.getDisplayName());
            Node[] nodes = { node };
            mgr.setSelectedNodes(nodes);
        }
        catch (PropertyVetoException e) {
            log.error("Node selection was vetoed",e);
        }
    }
    
    public void selectPaths(List<Long[]> paths) {
        if (paths==null) return;
        ExplorerManager mgr = explorerManagerProvider.getExplorerManager();
        try {
            List<Node> nodes = new ArrayList<>();
            for(Long[] path : paths) {
                Node node = NodeUtils.findNodeWithPath(mgr.getRootContext(), path);
                if (node==null) {
                    log.warn("Could not find node with path {}",NodeUtils.createPathString(path));
                }
                else {
                    log.debug("Selecting node: {}",node.getDisplayName());
                    nodes.add(node);
                }
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
    public int expand(List<Long[]> paths) {
        int numExpanded = 0;
        if (paths==null) return numExpanded;

        // Sort by length so that shorter paths are expanded first
        paths.sort(Comparator.comparingInt(o -> o.length));

        for (Long[] path : paths) {
            if (path == null) continue;
            log.debug("Expanding id path: {}", NodeUtils.createPathString(path));
            TreePath tp = getTreePath(path);
            if (tp != null) {
                log.info("Expanding tree path: {}", tp);
                expand(tp);
                numExpanded++;
            }
        }
        return numExpanded;
    }

    private TreePath getTreePath(Long[] path) {
        Node n = NodeUtils.findNodeWithPath(getRootNode(), path);
        if (n==null) return null;

        LinkedList<TreeNode> treeNodes = new LinkedList<>();
        
        while (n != null) {
            treeNodes.addFirst(Visualizer.findVisualizer(n));
            n = n.getParentNode();
        }
        
        TreeNode[] tns = treeNodes.toArray(new TreeNode[treeNodes.size()]);
        return new TreePath(tns);
    }
    
    public void addTreeKeyListener(KeyListener newListener) {
        tree.addKeyListener(newListener);
    }
    
    public void replaceKeyListeners(KeyListener newListener) {
        for(KeyListener listener : getKeyListeners()) {
            log.trace("Removing from BeanTreeView: "+listener);
            removeKeyListener(listener);
        }
        for(KeyListener listener : tree.getKeyListeners()) {
            log.trace("Removing from JTree: "+listener);
            tree.removeKeyListener(listener);
        }
        tree.addKeyListener(newListener);
    }
    
    public void addMouseListener(MouseListener listener) {
        tree.addMouseListener(listener);
    }

    public void removeMouseListener(MouseListener listener) {
        tree.removeMouseListener(listener);
    }
    
    @Override
    public void grabFocus() {
        tree.grabFocus();
        if (getCurrentNode() == null) {
            selectTopNode();
        }
    }

    /**
     * The rest of this code is copy and pasted from the super class, to fix a minor but annoying bug:
     * when the tree view is not focused, and a node is right-clicked, it should resolve all of the selection events
     * before displaying the popup menu. This is accomplished through the addition of SwingUtilities.invokeLater.
     */
    private transient PopupAdapter popupListener;

    @Override
    public boolean isPopupAllowed() {
        return popupListener != null && isShowing() && isDisplayable();
    }

    @Override
    public void setPopupAllowed(boolean value) {
        if ((popupListener == null) && value) {
            // on
            popupListener = new PopupAdapter();
            tree.addMouseListener(popupListener);
            return;
        }

        if ((popupListener != null) && !value) {
            // off
            tree.removeMouseListener(popupListener);
            popupListener = null;
            return;
        }
    }

    class PopupAdapter extends MouseUtils.PopupMouseAdapter {
        PopupAdapter() {
        }

        @Override
        protected void showPopup(MouseEvent e) {
            tree.cancelEditing();
            int selRow = tree.getRowForLocation(e.getX(), e.getY());

            if ((selRow == -1) && !isRootVisible()) {
                // clear selection
                try {
                    manager.setSelectedNodes(new Node[]{});
                } catch (PropertyVetoException exc) {
                    assert false : exc; // not permitted to be thrown
                }
            } else if (!tree.isRowSelected(selRow)) {
                // This will set ExplorerManager selection as well.
                // If selRow == -1 the selection will be cleared.
                tree.setSelectionRow(selRow);
            }

            if ((selRow != -1) || !isRootVisible()) {
                // Only change here:
                SwingUtilities.invokeLater(() -> {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), CustomTreeView.this);
                    createPopup((int) p.getX(), (int) p.getY());
                });
            }
        }
    }

    private void createPopup(int xpos, int ypos) {
        // bugfix #23932, don't create if it's disabled
        if (isPopupAllowed()) {
            Node[] selNodes = manager.getSelectedNodes();

            if (selNodes.length > 0) {
                Action[] actions = NodeOp.findActions(selNodes);
                if (actions.length > 0) {
                    createPopup(xpos, ypos, Utilities.actionsToPopup(actions, this));
                }
            } else if (manager.getRootContext() != null) {
                JPopupMenu popup = manager.getRootContext().getContextMenu();
                if (popup != null) {
                    createPopup(xpos, ypos, popup);
                }
            }
        }
    }

    private void createPopup(int xpos, int ypos, JPopupMenu popup) {
        if (popup.getSubElements().length > 0) {
            popup.show(CustomTreeView.this, xpos, ypos);
        }
    }
}
