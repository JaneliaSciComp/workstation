package org.janelia.it.workstation.gui.dialogs.choose;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.outline.EntityTree;


/**
 * An entity chooser that can display multiple Entity trees and allows the user to select one entity to work with.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MultiTreeEntityChooser extends AbstractChooser<EntityData> {

    private final List<String> uniqueIds = new ArrayList<String>();
    private final JTabbedPane tabbedPane;
    
    public MultiTreeEntityChooser(String title, List<EntityTree> entityTrees) {
    	setTitle(title);
        this.tabbedPane = new JTabbedPane();
        
        int i = 1;
        for(EntityTree entityTree : entityTrees) {
        	entityTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        	tabbedPane.addTab("Tree #"+i, entityTree);
        	i++;
        }
        
        addChooser(tabbedPane);
    }

    protected List<EntityData> choosePressed() {
    	List<EntityData> chosen = new ArrayList<EntityData>();
    	EntityTree entityTree = getSelectedTree();
    	TreePath[] selectionPaths = entityTree.getDynamicTree().getTree().getSelectionPaths();
    	if (selectionPaths==null) return chosen;
        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            chosen.add(entityTree.getEntityData(node));
            uniqueIds.add(entityTree.getDynamicTree().getUniqueId(node));
        }
        return chosen;
    }

    public List<String> getUniqueIds() {
    	return uniqueIds;
    }
    
    public EntityTree getSelectedTree() {
    	return (EntityTree)tabbedPane.getSelectedComponent();
    }
}
