package org.janelia.it.FlyWorkstation.gui.framework.outline.choose;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyTree;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;

/**
 * An ontology term chooser that can display an ontology specified by an OntologyRoot and allows the user to select
 * one or more terms for use.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementChooser extends AbstractChooser {

    private OntologyTree ontologyTree;
    private List<OntologyElement> chosenElements = new ArrayList<OntologyElement>();
    
    public OntologyElementChooser(String title, OntologyRoot root) {
    	super(title);
        ontologyTree = new OntologyTree() {
            protected void nodeDoubleClicked(MouseEvent e) {
            	chooseSelection();
            }
        };
    	ontologyTree.initializeTree(root);
    	ontologyTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    	addChooser(ontologyTree);
	}

    public List<OntologyElement> getChosenElements() {
		return chosenElements;
	}
    
    protected void choosePressed() {
    	chosenElements.clear();
		for(TreePath path : ontologyTree.getTree().getSelectionPaths()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			OntologyElement element = (OntologyElement)node.getUserObject();
			chosenElements.add(element);
		}
    }
}
