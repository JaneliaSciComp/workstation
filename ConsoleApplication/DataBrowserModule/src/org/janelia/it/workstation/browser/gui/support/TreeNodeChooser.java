package org.janelia.it.workstation.browser.gui.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.UserViewTreeNodeNode;
import org.janelia.model.domain.DomainObject;
import org.openide.nodes.Node;


/**
 * An node chooser that can display a node tree and allows the user to select 
 * one or more tree nodes to work with. 
 * 
 * Can also enforce the user picking a writeable tree node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeChooser extends NodeChooser {

    private final boolean ensureWriteAccess;
    
    public TreeNodeChooser(Node rootNode, String title, boolean ensureWriteAccess) {
        super(rootNode, title);
        this.ensureWriteAccess = ensureWriteAccess;    
    }
    
    @Override
    protected List<Node> choosePressed() {
        
        Node[] selectedNodes = mgr.getSelectedNodes();
        
        List<Node> domainObjectNodes = new ArrayList<>();
        for(Node selectedNode : selectedNodes) {
            if (allowChoose(selectedNode)) {
                AbstractDomainObjectNode<?> domainObjectNode = (AbstractDomainObjectNode<?>)selectedNode;
                
                if (ensureWriteAccess) {
                    final DomainObject domainObject = domainObjectNode.getDomainObject();
                    if (!ClientDomainUtils.hasWriteAccess(domainObject)) {
                        JOptionPane.showMessageDialog(this, "You do not have write permission for folder '"+domainObject.getName()+"'", "Action not allowed", JOptionPane.ERROR_MESSAGE);
                        return Collections.emptyList();
                    }
                }

                domainObjectNodes.add(domainObjectNode);
            }
            else {
                JOptionPane.showMessageDialog(this, "Item is not allowed: '"+selectedNode.getName()+"'", "Item not allowed", JOptionPane.ERROR_MESSAGE);
                return Collections.emptyList();
            }
        }
        
        return domainObjectNodes;
    }
    
    protected boolean allowChoose(Node selectedNode) {
        return selectedNode instanceof UserViewTreeNodeNode;
    }
}
