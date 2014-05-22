package org.janelia.it.workstation.gui.dialogs.choose;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * An ontology term chooser that can display an ontology specified by an OntologyRoot and allows the user to select
 * one or more terms for use.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementChooser extends AbstractChooser<OntologyElement> {

    private org.janelia.it.workstation.gui.framework.outline.OntologyOutline ontologyOutline;
    private boolean canAnnotate = false;
    
    public OntologyElementChooser(String title, Entity ontologyRoot) {
    	setTitle(title);
    	
        ontologyOutline = new org.janelia.it.workstation.gui.framework.outline.OntologyOutline() {
            
            @Override
            public List<Entity> loadRootList() throws Exception {
                return null;
            }

            protected void nodeDoubleClicked(MouseEvent e) {
                    DefaultMutableTreeNode node = ontologyOutline.getDynamicTree().getCurrentNode();
                    OntologyElement element = (OntologyElement) node.getUserObject();
                    if(canAnnotate){
                        org.janelia.it.workstation.gui.framework.actions.AnnotateAction action = new org.janelia.it.workstation.gui.framework.actions.AnnotateAction();
                        action.init(element);
                        action.doAction();
                        OntologyElementChooser.this.setVisible(false);
                    }
                    else{
                        chooseSelection();
                    }
            }
        };
        
        ontologyOutline.setLazyLoading(false);
        ontologyOutline.setShowToolbar(false);
        ontologyOutline.showOntologyTree(ontologyRoot);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setMultipleSelection(true);
                addChooser(ontologyOutline);
            }
        });
    }

    public void setMultipleSelection(boolean multipleSelection) {
    	JTree tree = ontologyOutline.getTree();
    	TreeSelectionModel selectionModel = tree.getSelectionModel();
    	selectionModel.setSelectionMode(
    			multipleSelection ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
    
    protected List<OntologyElement> choosePressed() {
    	List<OntologyElement> chosen = new ArrayList<OntologyElement>();
        for (TreePath path : ontologyOutline.getTree().getSelectionPaths()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            chosen.add(ontologyOutline.getOntologyElement(node));
        }

        DefaultMutableTreeNode currNode = ontologyOutline.getDynamicTree().getCurrentNode();
        OntologyElement currElement = ontologyOutline.getOntologyElement(currNode);
        if (canAnnotate){
            org.janelia.it.workstation.gui.framework.actions.AnnotateAction action = new org.janelia.it.workstation.gui.framework.actions.AnnotateAction();
            action.init(currElement);
            action.doAction();
            OntologyElementChooser.this.setVisible(false);
        }

        return chosen;
    }

    public void setCanAnnotate(boolean bool){
        canAnnotate = bool;
    }

}
