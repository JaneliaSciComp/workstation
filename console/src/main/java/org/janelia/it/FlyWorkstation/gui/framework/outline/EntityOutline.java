package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.hibernate.Hibernate;
import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.ConsoleFrame;
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

    private final ConsoleFrame consoleFrame;
    private final JPopupMenu popupMenu;
    private final JPanel treesPanel;
    private DynamicTree selectedTree;
    private boolean lazy = true;
    
    
    public EntityOutline(final ConsoleFrame consoleFrame) {
        super(new BorderLayout());
        this.consoleFrame = consoleFrame;
        this.setMinimumSize(new Dimension(400,400));
        
        // Create context menus
        popupMenu = new JPopupMenu();
        JMenuItem newSessionItem = new JMenuItem("Create Annotation Session for 2D Images");
        newSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	
            	DefaultMutableTreeNode node = selectedTree.getCurrentNode();
        		Entity entity = (Entity)node.getUserObject();
        		
            	try {
            		loadLazyDescendants(node);	
            		List<Entity> entities = getDescendantsOfType(entity, EntityConstants.TYPE_TIF_2D);
                	consoleFrame.getAnnotationSessionPropertyPanel().showForNewSession(entity.getName(), entities);
		            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
            	}
            	catch (Exception e) {
            		e.printStackTrace();
            	}
            	
            	// TODO: move this to AnnotationSessionPropertyPanel.save()
//                System.out.println("DEBUG: Creating new Annotation Session Task");
//                AnnotationSessionTask newTask = createAnnotationSession(getSelectedEntity());
//                consoleFrame.getOutlookBar().setVisibleBarByName(ConsoleFrame.BAR_SESSION);
//                consoleFrame.getAnnotationSessionOutline().rebuildDataModel();
//                consoleFrame.getAnnotationSessionOutline().selectSession(newTask.getObjectId().toString());
            }
        });
        popupMenu.add(newSessionItem);
        
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
            	if (lazy) {
            		rootEntity = EJBFactory.getRemoteAnnotationBean().getEntityById(rootId.toString());
            	}
            	else {
            		rootEntity = EJBFactory.getRemoteAnnotationBean().getEntityTree(rootId);
            	}
            }

			protected void hadSuccess() {
				try {
			        createNewTree(rootEntity);
                    addNodes(null, rootEntity);

                    selectedTree.expand(selectedTree.getRootNode(), true);
                    
		            treesPanel.removeAll();
			        treesPanel.add(selectedTree);

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

    	selectedTree = new DynamicTree(root, true, true) {
    		
            protected void showPopupMenu(MouseEvent e) {
                popupMenu.show(tree, e.getX(), e.getY());
            }

            protected void nodeClicked(MouseEvent e) {

            	DefaultMutableTreeNode node = getCurrentNode();
            	if (node instanceof LazyTreeNode) return;
            	
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

            public SimpleWorker loadLazyChildren(final DefaultMutableTreeNode node) {
            	
				SimpleWorker loadingWorker = new SimpleWorker() {

		            protected void doStuff() throws Exception {
		                Entity entity = (Entity)node.getUserObject();
		            	loadLazyEntity(entity, false);
		            }

					protected void hadSuccess() {
						replaceLazyChildNodes(node, false);
				        // Re-expand the node because the model was updated
				        expand(node, true);
			            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
					}

					protected void hadError(Throwable error) {
						error.printStackTrace();
						JOptionPane.showMessageDialog(EntityOutline.this, "Error loading folders", 
								"Folder Load Error", JOptionPane.ERROR_MESSAGE);
					}

		        };

		        loadingWorker.execute();
		        
		        return loadingWorker;
            }
        };
        
        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer());
    }

    private void loadLazyDescendants(DefaultMutableTreeNode node) throws Exception {

    	// Fill out the model
        Entity entity = (Entity)node.getUserObject();
        loadLazyEntity(entity, true);
    	
    	// Sync the tree with the model
        replaceLazyChildNodes(node, true);
    	
    }
    private void loadLazyEntity(Entity entity, boolean recurse) {
    	
    	if (!areLoaded(entity.getEntityData())) {
        	Set<Entity> childEntitySet = EJBFactory.getRemoteAnnotationBean().getChildEntities(entity.getId());
        	Map<Long,Entity> childEntityMap = new HashMap<Long,Entity>();
        	for(Entity childEntity : childEntitySet) {
        		childEntityMap.put(childEntity.getId(), childEntity);
        	}
        	
			// Replace the entity data with real objects
			for(EntityData ed : entity.getEntityData()) {
				if (ed.getChildEntity() != null) {
					ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
				}
			}
    	}
		
    	if (recurse) {
			for(EntityData ed : entity.getEntityData()) {
				if (ed.getChildEntity() != null) {
					loadLazyEntity(ed.getChildEntity(), true);
				}
			}
    	}
    }
    
    private void replaceLazyChildNodes(DefaultMutableTreeNode node, boolean recurse) {
    	
    	if (node.getChildCount() == 1 && node.getChildAt(0) instanceof LazyTreeNode) {

        	Entity entity = (Entity)node.getUserObject();
			ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getEntityData());
			
			if (!areLoaded(edList)) {
				return;
			}

			selectedTree.removeLazyChild(node);
	        addChildren(node, edList);
	        
	        int[] childIndices = new int[node.getChildCount()];
	        for(int i=0; i<childIndices.length; i++) {
	        	childIndices[i] = i;
	        }
	        selectedTree.getTreeModel().nodesWereInserted(node, childIndices);
    	}
    	
    	if (recurse) {
    		for (Enumeration e = node.children(); e.hasMoreElements();) {
    			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
    			replaceLazyChildNodes(childNode, true);
            }    	
    	}
    }
    
    private boolean areLoaded(Collection<EntityData> eds) {
        for(EntityData entityData : eds) {
        	if (!Hibernate.isInitialized(entityData.getChildEntity())) {
        		return false;
        	}
        }
        return true;
    }
    
    private void addNodes(DefaultMutableTreeNode parentNode, Entity newEntity) {
		
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEntity);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.rootNode;
        }
        
        Set<EntityData> dataSet = newEntity.getEntityData();
        List<EntityData> childDataList = new ArrayList<EntityData>();
        
        for(EntityData ed : dataSet) {
        	if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) {
        		childDataList.add(ed);
        	}
        }
        
        if (childDataList.isEmpty()) return;
        
        // Test for proxies
        boolean allLoaded = areLoaded(childDataList);

        if (!allLoaded) {
        	selectedTree.addObject(newNode, new LazyTreeNode());
        }
        else {
            addChildren(newNode, childDataList);
        }
        
    }

    private void addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList) {
		
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
        	Entity child = entityData.getChildEntity();
        	if (child != null) {
        		addNodes(parentNode, child);
        	}
        }
    }
    
//    private AnnotationSessionTask createAnnotationSession(Entity targetEntity) {
//        try {
//            Set<String> targetEntityIds = get2DTIFItems(targetEntity, new HashSet<String>());
//            String entityIds = Task.csvStringFromCollection(targetEntityIds);
//            AnnotationSessionTask newSessionTask = new AnnotationSessionTask(null, System.getenv("USER"), null, null);
//            newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationTargets, entityIds);
//            newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationCategories, "");
//            return (AnnotationSessionTask)EJBFactory.getRemoteComputeBean().saveOrUpdateTask(newSessionTask);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    private Entity getSelectedEntity() {
        TreePath tmpPath = selectedTree.getTree().getSelectionPath();
        if (null==tmpPath) return null;
        return (Entity)((DefaultMutableTreeNode)tmpPath.getLastPathComponent()).getUserObject();
    }
    
    //EntityConstants.TYPE_TIF_2D
    
    public List<Entity> getDescendantsOfType(Entity entity, String typeName) {
    	
    	List<Entity> items = new ArrayList<Entity>();
        if (typeName.equals(entity.getEntityType().getName())) {
        	items.add(entity);
        }
        
        for (EntityData entityData : entity.getOrderedEntityData()) {
        	Entity child = entityData.getChildEntity();
        	if (child != null) {
                items.addAll(getDescendantsOfType(child, typeName));
        	}
        }
        
        return items;
    }
}
