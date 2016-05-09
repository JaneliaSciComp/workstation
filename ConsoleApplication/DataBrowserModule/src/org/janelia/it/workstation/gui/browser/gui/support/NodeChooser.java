package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.tree.TreeSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.tree.CustomTreeView;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;

import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;

/**
 * An node chooser that can display a node tree and allows the user to select 
 * one or more nodes to work with.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeChooser extends AbstractChooser<Node> implements ExplorerManager.Provider {

    private final ExplorerManager mgr = new ExplorerManager();  
    private final Node root;
    private final BeanTreeView beanTreeView;
    private final List<String> selectedPaths = new ArrayList<>();

    public NodeChooser(Node rootNode, String title) {
        setTitle(title);
        this.root = rootNode;
        this.beanTreeView = new CustomTreeView(this);
        beanTreeView.setDefaultActionAllowed(false);
        mgr.setRootContext(root);     
        
        // Defaults
        setRootVisible(true);
        setMultipleSelection(false);
        
        for(Node node : root.getChildren().getNodes()) {
            beanTreeView.expandNode(node);
        }
        
        addChooser(beanTreeView);   
    }
    
    public final void setRootVisible(boolean visible) {
        beanTreeView.setRootVisible(visible);
    }
    
    public final void setMultipleSelection(boolean allowMulti) {
        beanTreeView.setSelectionMode(allowMulti 
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION 
                : TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
    
    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }

    @Override
    protected List<Node> choosePressed() {
        Node[] selectedNodes = mgr.getSelectedNodes();
        
        for(Node selectedNode : selectedNodes) {
            if (selectedNode instanceof DomainObjectNode) {
                selectedPaths.add(NodeUtils.createPathString((DomainObjectNode)selectedNode));
            }
        }
        
        return Arrays.asList(selectedNodes);
    }

    public List<String> getSelectedPaths() {
    	return selectedPaths;
    }
}
