package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.tree.TreeSelectionModel;
import org.janelia.it.workstation.gui.browser.nodes.CustomTreeView;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.RootNode;

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
    private final BeanTreeView beanTreeView;
    private final List<String> selectedPaths = new ArrayList<>();

    public NodeChooser(String title, RootNode root, boolean allowMulti) {
        mgr.setRootContext(root);
        setTitle(title);
        
        this.beanTreeView = new CustomTreeView(this);
        
        addChooser(beanTreeView);        
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
