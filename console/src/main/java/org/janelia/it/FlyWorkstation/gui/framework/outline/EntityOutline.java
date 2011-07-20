package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
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
public class EntityOutline extends JPanel implements Cloneable {

    private final JPopupMenu popupMenu;
    private final JPanel treesPanel;
    private int nodeCount;
    
    private DynamicTree selectedTree;
    
    public EntityOutline() {
        super(new BorderLayout());
    	
        // Create context menus
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
        
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
			        createNewTree(rootEntity);
                    addNodes(selectedTree, null, rootEntity);
                    
		            treesPanel.removeAll();
			        treesPanel.add(selectedTree);
			        
			        selectedTree.expand(selectedTree.getRootNode(), true);
			        
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

    
    private void createNewTree(Entity root) {
    	
    	selectedTree = new DynamicTree(root) {

            protected void showPopupMenu(MouseEvent e) {

            }

            protected void nodeClicked(MouseEvent e) {
            	
            	DefaultMutableTreeNode node = getCurrentNode();
            	Entity entity = (Entity)node.getUserObject();
            	
            	String type = entity.getEntityType().getName();
            	
            	List<Entity> entities = new ArrayList<Entity>();
            	
            	if (type.equals(EntityConstants.TYPE_TIF_2D)) {
            		entities.add(entity);
            	}
            	else if (type.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
            		// Get all the 2d TIFFs that are children of this result
            		for(EntityData ed : entity.getOrderedEntityData()) {
            			Entity child = ed.getChildEntity();
            			if (child == null) continue;
            			String childType = child.getEntityType().getName();
            			if (!childType.equals(EntityConstants.TYPE_TIF_2D)) continue;
            			entities.add(child);
            		}
            	}

            	ConsoleApp.getMainFrame().getViewerPanel().loadImageEntities(entities);
            }
            
        };
        
        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer(selectedTree));
    }
    
    private void addNodes(DynamicTree tree, DefaultMutableTreeNode parentNode, Entity newEntity) {
        nodeCount++;
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = tree.addObject(parentNode, newEntity, null);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = tree.rootNode;
        }

        List<EntityData> dataList = new ArrayList<EntityData>(newEntity.getEntityData());
        
    	Collections.sort(dataList, new Comparator<EntityData>() {
			@Override
			public int compare(EntityData o1, EntityData o2) {
				if (o1.getOrderIndex() == null) {
					if (o2.getOrderIndex() == null) {
						Entity child1 = o1.getChildEntity();
						Entity child2 = o2.getChildEntity();
						if (child1 == null) {
							if (child2 == null) {
								return o1.getId().compareTo(o2.getId());		
							}
							else {
								return -1;
							}
						}
						else if (child2 == null) {
							return 1;
						}
						return child1.getName().compareTo(child2.getName());
						
					}
					return -1;
				}
				else if (o2.getOrderIndex() == null) {
					return 1;
				}
				return o1.getOrderIndex().compareTo(o2.getOrderIndex());
			}
		});
	    
        for (EntityData entityData : dataList) {
    		// The tree was fetched with getEntityTree, so the child entities have already been prepopulated
        	Entity child = entityData.getChildEntity();
        	if (child != null) {
        		addNodes(tree, newNode, child);
        	}
        }
    }

}
