package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * This action allows the user to create new domain objects underneath an 
 * existing TreeNode, but right-clicking on it and choosing New from the 
 * context menu.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewDomainObjectAction extends NodeAction {

    private final static NewDomainObjectAction singleton = new NewDomainObjectAction();
    public static NewDomainObjectAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    
    private NewDomainObjectAction() {
    }
    
    @Override
    public String getName() {
        return "New";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        // Implemented by popup presenter
    }

    @Override
    protected boolean asynchronous() {
        // We do our own background processing
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            selected.add(node);
        }
        // Enable state is determined by the popup presenter
        return true;
    }
    
    @Override 
    public JMenuItem getPopupPresenter() {
        
        TreeNodeNode node = (TreeNodeNode)selected.get(0);
        
        JMenu newMenu = new JMenu("New");
        
        JMenuItem newFolderItem = new JMenuItem("Folder");
        newFolderItem.addActionListener(new NewFolderAction(node));
        newMenu.add(newFolderItem);
        
        JMenuItem newFilterItem = new JMenuItem("Filter");
        newFilterItem.addActionListener(new NewFilterAction(node));
        newMenu.add(newFilterItem);
        
        JMenuItem newSetItem = new JMenuItem("Set");
        newSetItem.addActionListener(new NewSetAction(node));
        newMenu.add(newSetItem);
        
        if (selected.size()!=1) {
            newMenu.setEnabled(false);
        }
        
        return newMenu;
    }  
}
