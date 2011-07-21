package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
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
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;


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
                System.out.println("DEBUG: Creating new Annotation Session Task");
                AnnotationSessionTask newTask = createAnnotationSession(getSelectedEntity());
                consoleFrame.getOutlookBar().setVisibleBarByName(ConsoleFrame.BAR_SESSION);
                consoleFrame.getAnnotationSessionOutline().rebuildDataModel();
                consoleFrame.getAnnotationSessionOutline().selectSession(newTask.getObjectId().toString());
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

    	selectedTree = new DynamicTree(root) {
    		
            protected void showPopupMenu(MouseEvent e) {
                popupMenu.show(tree, e.getX(), e.getY());
            }

            protected void nodeClicked(MouseEvent e) {

            	DefaultMutableTreeNode node = getCurrentNode();
            	if (node.getUserObject() instanceof LazyEntity) return;
            	
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

        selectedTree.getTree().addTreeWillExpandListener(new TreeWillExpandListener() {
			
			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
				
		        boolean allLoaded = true;
				for(int i=0; i<node.getChildCount(); i++) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getChildAt(i);
					if (childNode.getUserObject() instanceof LazyEntity) {
		        		allLoaded = false;
		        		break;
					}
				}
				
				if (allLoaded) return;
				
				final Entity entity = (Entity)node.getUserObject();

				SimpleWorker loadingWorker = new SimpleWorker() {

					private Map<Long,Entity> childEntityMap;

		            protected void doStuff() throws Exception {
		            	Set<Entity> childEntitySet = EJBFactory.getRemoteAnnotationBean().getChildEntities(entity.getId());
		            	childEntityMap = new HashMap<Long,Entity>();
		            	for(Entity childEntity : childEntitySet) {
		            		childEntityMap.put(childEntity.getId(), childEntity);
		            	}
		            }

					protected void hadSuccess() {
						
						// Replace the entity data  with real objects
						final Entity entity = (Entity)node.getUserObject();
						for(EntityData ed : entity.getEntityData()) {
							if (ed.getChildEntity() != null) {
								ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
							}
						}

						// Replace the children nodes
						int[] childIndices = { 0 };
						Object[] removedChildren = { node.getChildAt(0) };
						node.remove(0);
						selectedTree.getTreeModel().nodesWereRemoved(node, childIndices, removedChildren);
						
						ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getEntityData());
				        addChildren(node, edList);
				        
				        childIndices = new int[node.getChildCount()];
				        for(int i=0; i<childIndices.length; i++) {
				        	childIndices[i] = i;
				        }
				        selectedTree.getTreeModel().nodesWereInserted(node, childIndices);
				        
				        // Re-expand the node because the model was updated
				        selectedTree.expand(node, true);
			            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
					}

					protected void hadError(Throwable error) {
						error.printStackTrace();
						JOptionPane.showMessageDialog(EntityOutline.this, "Error loading folders", 
								"Folder Load Error", JOptionPane.ERROR_MESSAGE);
					}

		        };

		        loadingWorker.execute();
//		        throw new ExpandVetoException(event);
			}
			
			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
				// We don't care
			}
		});
        
        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer(selectedTree));
    }

    private void addNodes(DefaultMutableTreeNode parentNode, Entity newEntity) {
		
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEntity, null);
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
        boolean allLoaded = true;
        for(EntityData entityData : childDataList) {
        	if (!Hibernate.isInitialized(entityData.getChildEntity())) {
        		allLoaded = false;
        		break;
        	}
        }

        if (!allLoaded) {
        	selectedTree.addObject(newNode, new LazyEntity(null), null);
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
    
    private AnnotationSessionTask createAnnotationSession(Entity targetEntity) {
        try {
            Set<String> targetEntityIds = get2DTIFItems(targetEntity, new HashSet<String>());
            String entityIds = Task.csvStringFromCollection(targetEntityIds);
            AnnotationSessionTask newSessionTask = new AnnotationSessionTask(null, System.getenv("USER"), null, null);
            newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationTargets, entityIds);
            newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationCategories, "");
            return (AnnotationSessionTask)EJBFactory.getRemoteComputeBean().saveOrUpdateTask(newSessionTask);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Entity getSelectedEntity() {
        TreePath tmpPath = selectedTree.getTree().getSelectionPath();
        if (null==tmpPath) return null;
        return (Entity)((DefaultMutableTreeNode)tmpPath.getLastPathComponent()).getUserObject();
    }
    
    public Set<String> get2DTIFItems(Entity parentEntity, Set<String> entitySet) {
        for (EntityData entityData : parentEntity.getEntityData()) {
    		// The tree was fetched with getEntityTree, so the child entities have already been prepopulated
        	Entity child = entityData.getChildEntity();
        	if (child != null) {
                if (EntityConstants.TYPE_TIF_2D.equals(child.getEntityType().getName())) {
                    entitySet.add(child.getId().toString());
                }
        		get2DTIFItems(child, entitySet);
        	}
        }
        return entitySet;
    }
}
