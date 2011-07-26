package org.janelia.it.FlyWorkstation.gui.framework.outline.choose;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTree;
import org.janelia.it.jacs.model.entity.Entity;


/**
 * An entity chooser that can display an Entity tree and allows the user to select one or more entities
 * to work with.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityChooser extends AbstractChooser {

    private final EntityTree entityTree;
    private final List<Entity> chosenEntities = new ArrayList<Entity>();
    
    public EntityChooser(String title, EntityOutline entityOutline) {
    	super(title);
        entityTree = new EntityTree(entityOutline.getDynamicTree().isLazyLoading()) {
            protected void nodeDoubleClicked(MouseEvent e) {
            	chooseSelection();
            }
        };
    	entityTree.initializeTree(entityOutline.getCurrentRootEntity());
        entityTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    	addChooser(entityTree);
	}

    public List<Entity> getChosenEntities() {
		return chosenEntities;
	}
	
    protected void choosePressed() {
    	chosenEntities.clear();
		for(TreePath path : entityTree.getDynamicTree().getTree().getSelectionPaths()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			Entity entity = (Entity)node.getUserObject();
			chosenEntities.add(entity);
		}
    }
    
}
