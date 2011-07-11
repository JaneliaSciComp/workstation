package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import sun.awt.VerticalBagLayout;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class EntityOutline extends JScrollPane implements Cloneable {

    private final JPopupMenu popupMenu;

    public EntityOutline() {
        // Create context menus
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        // Populate the tree view with the user's first tree
        // Load the tree in the background so that the app starts up first
        SwingWorker<Void, Void> loadTasks = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    List<Entity> entityRootList = EJBFactory.getRemoteAnnotationBean().getCommonRootEntitiesByType(EntityConstants.TYPE_FOLDER_ID);
                    if (null != entityRootList
                            && entityRootList.size() >= 1) {
                        initializeTree(entityRootList);
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

    private void initializeTree(List<Entity> entities) {
        JPanel treesPanel = new JPanel(new VerticalBagLayout());
        for (Entity entity : entities) {
            // Create a new tree and add all the nodes to it
            DynamicTree newTree = new DynamicTree(entity);
            addNodes(newTree, null, entity);
            treesPanel.add(newTree);
        }
        setViewportView(treesPanel);
        this.updateUI();
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
            List<EntityData> dataList = newEntity.getOrderedEntityData();
            for (EntityData tmpData : dataList) {
                Entity childEntity = tmpData.getChildEntity();
                if (childEntity != null) {
                    addNodes(tree, newNode, childEntity);
                }
            }
        }
    }

}
