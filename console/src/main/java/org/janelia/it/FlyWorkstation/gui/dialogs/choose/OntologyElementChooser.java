package org.janelia.it.FlyWorkstation.gui.dialogs.choose;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.NavigateToNodeAction;
import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyTree;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;

/**
 * An ontology term chooser that can display an ontology specified by an OntologyRoot and allows the user to select
 * one or more terms for use.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementChooser extends AbstractChooser<OntologyElement> {

    private final OntologyTree ontologyTree;
    private boolean canAnnotate = false;
    
    public OntologyElementChooser(String title, OntologyRoot root) {
    	
    	setTitle(title);
    	
        ontologyTree = new OntologyTree() {
            protected void nodeDoubleClicked(MouseEvent e) {
                if(canAnnotate){
                    org.janelia.it.FlyWorkstation.gui.framework.actions.Action action = SessionMgr.getBrowser().getOntologyOutline().getActionForNode(ontologyTree.getDynamicTree().getCurrentNode());
                    if (action != null && !(action instanceof NavigateToNodeAction)) {
                        action.doAction();
                        OntologyElementChooser.this.setVisible(false);
                    }
                }
                else{
                    chooseSelection();
                }
            }
        };
        ontologyTree.initializeTree(root);
        setMultipleSelection(true);
        addChooser(ontologyTree);
        ontologyTree.getDynamicTree().expandAll(true);

    }

    public void setMultipleSelection(boolean multipleSelection) {
    	ontologyTree.getTree().getSelectionModel().setSelectionMode(
    			multipleSelection ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
    
    protected List<OntologyElement> choosePressed() {
    	List<OntologyElement> chosen = new ArrayList<OntologyElement>();
        for (TreePath path : ontologyTree.getTree().getSelectionPaths()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            OntologyElement element = (OntologyElement) node.getUserObject();
            chosen.add(element);
            if(canAnnotate){
                Action action = SessionMgr.getBrowser().getOntologyOutline().getActionForNode(ontologyTree.getDynamicTree().getCurrentNode());
                if (action != null && !(action instanceof NavigateToNodeAction)) {
                    action.doAction();
                    OntologyElementChooser.this.setVisible(false);
                }
            }
        }
        return chosen;
    }

    public void setCanAnnotate(boolean bool){
        canAnnotate = bool;
    }

}
