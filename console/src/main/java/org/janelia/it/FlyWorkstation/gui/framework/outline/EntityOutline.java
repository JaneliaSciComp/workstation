package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.KeyListener;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class EntityOutline extends JPanel implements Cloneable {
    private JPanel treesPanel;
    private DynamicTree selectedTree;

    private final JPopupMenu popupMenu;

    public EntityOutline() {
        super(new BorderLayout());

        // Create context menus
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        // Lay everything out
        this.treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);

        // Populate the tree view with the user's first tree
        // Load the tree in the background so that the app starts up first
        SwingWorker<Void, Void> loadTasks = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    List<Entity> entityRootList = EJBFactory.getRemoteAnnotationBean().getEntitiesByType(EntityConstants.TYPE_FOLDER_ID);
                    if (null != entityRootList
                            && entityRootList.size() >= 1) {
                        initializeTree(entityRootList.get(0));
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        loadTasks.execute();
    }

    private DynamicTree initializeTree(Entity entityRoot) {
        // Create a new tree and add all the nodes to it
        selectedTree = new DynamicTree(new DefaultTreeCellRenderer(), entityRoot);
        addNodes(selectedTree, null, entityRoot);
        selectedTree.expandAll();

        // Replace the default key listener on the tree

        final JTree tree = selectedTree.getTree();
        KeyListener defaultKeyListener = tree.getKeyListeners()[0];

        // Replace the tree in the panel
        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        this.updateUI();
        return selectedTree;
    }

    private void addNodes(DynamicTree tree, DefaultMutableTreeNode parentNode, Entity newEntity) {
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = tree.addObject(parentNode, newEntity);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = tree.rootNode;
        }

        if (newEntity.getEntityData() != null) {
            for (EntityData tmpData : newEntity.getOrderedEntityData()) {
                Entity childEntity = tmpData.getChildEntity();
                if (childEntity != null) {
                    addNodes(tree, newNode, childEntity);
                }
            }
        }
    }

}
