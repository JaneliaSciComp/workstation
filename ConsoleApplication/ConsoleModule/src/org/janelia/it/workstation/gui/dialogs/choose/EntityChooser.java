package org.janelia.it.workstation.gui.dialogs.choose;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.outline.EntityOutline;
import org.janelia.it.workstation.gui.framework.outline.EntityTree;


/**
 * An entity chooser that can display an Entity tree and allows the user to select one or more entities
 * to work with.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityChooser extends AbstractChooser<EntityData> {

    private final EntityTree entityTree;
    private final List<String> uniqueIds = new ArrayList<String>();

    public EntityChooser(String title, EntityTree entityTree) {
    	setTitle(title);
        this.entityTree = entityTree;
        entityTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        addChooser(entityTree);
    }
    
    public EntityChooser(String title, EntityOutline entityOutline) {
    	setTitle(title);
        entityTree = new EntityTree() {
            protected void nodeDoubleClicked(MouseEvent e) {
                chooseSelection();
            }
        };
        entityTree.initializeTree(entityOutline.getCurrentRootEntity());
        entityTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        addChooser(entityTree);
    }

    public EntityTree getEntityTree() {
        return entityTree;
    }

    protected List<EntityData> choosePressed() {
    	List<EntityData> chosen = new ArrayList<EntityData>();
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
}
