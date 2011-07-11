package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class EntityOutline extends JScrollPane implements Cloneable {

    private final JPopupMenu popupMenu;
    private final JPanel treesPanel;
    
    public EntityOutline() {
        // Create context menus
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        treesPanel = new JPanel(new BorderLayout());
        setViewportView(treesPanel);
        
		treesPanel.add(new JLabel(Icons.loadingIcon));

		SimpleWorker loadingWorker = new SimpleWorker() {

			private List<Entity> entityRootList;
			
            protected void doStuff() throws Exception {
            	entityRootList = EJBFactory.getRemoteAnnotationBean().getCommonRootEntitiesByType(EntityConstants.TYPE_FOLDER_ID);
            }

			protected void hadSuccess() {
				if (null != entityRootList && entityRootList.size() >= 1) {
                    initializeTree(entityRootList.get(0).getId());
                }
			}
			
			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(EntityOutline.this, "Error loading folders", "Folder Load Error", JOptionPane.ERROR_MESSAGE);
	            treesPanel.removeAll();
	            EntityOutline.this.updateUI();
			}
            
        };

        loadingWorker.execute();
    }

    private void initializeTree(final Long rootId) {

        treesPanel.removeAll();
        
        if (rootId == null) return;
        
		treesPanel.add(new JLabel(Icons.loadingIcon));
        this.updateUI();
        
		SimpleWorker loadingWorker = new SimpleWorker() {

			private Entity rootEntity;
        	
            protected void doStuff() throws Exception {
            	rootEntity = EJBFactory.getRemoteAnnotationBean().getEntityTree(rootId);
            }

			protected void hadSuccess() {
				try {
			        // Create a new tree and add all the nodes to it
			        DynamicTree newTree = new DynamicTree(rootEntity);
			        addNodes(newTree, null, rootEntity);
			        
		            treesPanel.removeAll();
			        treesPanel.add(newTree);
			        
			        EntityOutline.this.updateUI();
				}
				catch (Exception e) {
					hadError(e);
				}
			}
			
			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(EntityOutline.this, "Error loading folders", "Folder Load Error", JOptionPane.ERROR_MESSAGE);
	            treesPanel.removeAll();
	            EntityOutline.this.updateUI();
			}
            
        };

        loadingWorker.execute();
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

        List<EntityData> dataList = newEntity.getOrderedEntityData();
        for (EntityData entityData : dataList) {
    		// The tree was fetched with getEntityTree, so the child entities have already been prepopulated
        	Entity child = entityData.getChildEntity();
        	if (child != null) {
        		addNodes(tree, newNode, child);
        	}
        }
    }

}
