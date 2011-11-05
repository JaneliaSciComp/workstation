package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public abstract class EntityOutline extends EntityTree implements Cloneable {
    
    private List<Entity> entityRootList;
    private Entity selectedEntity;
    
    public EntityOutline() {
    	super(true);
        this.setMinimumSize(new Dimension(400, 400));
        showLoadingIndicator();
        

        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void entitySelected(final long entityId, final boolean outline) {
				if (!outline) return;
				selectEntityById(entityId);
			}
        });
    }

    public void init(List<Entity> entityRootList) {
    	this.entityRootList = entityRootList;
        if (null != entityRootList && entityRootList.size() >= 1) {
            initializeTree(entityRootList.get(entityRootList.size()-1).getId(), null);
        }
        else {
        	Entity noDataEntity = new Entity();
        	EntityType type = new EntityType();
        	type.setName("");
        	noDataEntity.setEntityType(type);
        	noDataEntity.setName("No data");
        	initializeTree(noDataEntity);
        }
    }
    
    /**
     * Override this method to load the root list. This method will be called in a worker thread.
     * @return
     */
    public abstract List<Entity> loadRootList();
    
    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {

    	// Clicked on what node?
        final DefaultMutableTreeNode node = selectedTree.getCurrentNode();
        final Entity entity = (Entity) node.getUserObject();
    	if (entity == null) return;

        selectNode(node);
        
        // Create context menus
        final JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem titleItem = new JMenuItem(entity.getName());
        titleItem.setEnabled(false);
        popupMenu.add(titleItem);
        
        // Copy to clipboard
        JMenuItem copyMenuItem = new JMenuItem("  Copy to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getName());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        popupMenu.add(copyMenuItem);

        
        // Change data source (root only)
    	if (node.isRoot()) {
            final JMenu changeDataSourceMenu = new JMenu("  Change data root...");

            changeDataSourceMenu.addMenuListener(new MenuListener() {
    			
    			@Override
    			public void menuSelected(MenuEvent e) {

    				changeDataSourceMenu.removeAll();
    				JMenuItem loadingItem = new JMenuItem("Loading...");
    				loadingItem.setEnabled(false);
	                changeDataSourceMenu.add(loadingItem);
	                
    				SimpleWorker menuWorker = new SimpleWorker() {
						
						@Override
						protected void doStuff() throws Exception {
							entityRootList = loadRootList();
						}
						
						@Override
						protected void hadSuccess() {
							changeDataSourceMenu.removeAll();
				        	for(final Entity commonRoot : entityRootList) {
				                final JMenuItem dataSourceItem = new JCheckBoxMenuItem(
				                		commonRoot.getName() +" ("+commonRoot.getUser().getUserLogin()+")", 
				                		commonRoot.getId().equals(selectedEntity.getId()));
				                dataSourceItem.addActionListener(new ActionListener() {
				                    public void actionPerformed(ActionEvent actionEvent) {
				                    	initializeTree(commonRoot.getId(), null);
				                    }
				                });
				                changeDataSourceMenu.add(dataSourceItem);
				        	}

				        	// A little hack to refresh the submenu. Just calling revalidate/repaint will show the new
				        	// contents but not resize the menu to fit. 
				        	if (changeDataSourceMenu.isSelected()) {
					        	changeDataSourceMenu.setPopupMenuVisible(false);
				        		changeDataSourceMenu.setPopupMenuVisible(true);
				        	}
						}
						
						@Override
						protected void hadError(Throwable error) {
							error.printStackTrace();
						}
					};
					
					menuWorker.execute();
    			}
    			
    			@Override
    			public void menuDeselected(MenuEvent e) {
    			}
    			
    			@Override
    			public void menuCanceled(MenuEvent e) {
    			}
    		});
            
            popupMenu.add(changeDataSourceMenu);
        	
    	}
    
        // Create annotation session
        JMenuItem newFragSessionItem = new JMenuItem("  Create Annotation Session for Neuron Fragments");
        newFragSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                DefaultMutableTreeNode node = selectedTree.getCurrentNode();
                final Entity entity = (Entity) node.getUserObject();

                try {
                    Utils.setWaitingCursor(EntityOutline.this);

                    SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, true) {

                        protected void doneLoading() {
                            Utils.setDefaultCursor(EntityOutline.this);
                            List<Entity> entities = entity.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT);
                            SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyDialog().showForNewSession(entity.getName(), entities);
                            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
                        }

                        @Override
                        protected void hadError(Throwable error) {
                        	error.printStackTrace();
                            Utils.setDefaultCursor(EntityOutline.this);
                            JOptionPane.showMessageDialog(EntityOutline.this, "Error loading nodes", "Internal Error", JOptionPane.ERROR_MESSAGE);
                        }
                    };

                    loadingWorker.execute();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        popupMenu.add(newFragSessionItem);
    	
        if (entity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
            JMenuItem v3dMenuItem = new JMenuItem("  View in V3D (Neuron Annotator)");
            v3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    if (ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(entity.getId())) {
                    	// Success
                    	return;
                    }
                	// Launch V3D if it isn't running
                    // TODO: this should be redone to use the "Tools" configuration
//                    String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
//                    filepath = Utils.convertJacsPathLinuxToMac(filepath);
//                    final File file = new File(filepath);
//                    String tmpCmd = "/Users/" + (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME) + "/Dev/v3d/v3d/v3d64.app/Contents/MacOS/v3d64 -i " + file.getAbsolutePath();
//                    System.out.println("DEBUG: " + tmpCmd);
//                    try {
//                        Runtime.getRuntime().exec(tmpCmd);
//                    }
//                    catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            });
            popupMenu.add(v3dMenuItem);
        }

        popupMenu.show(selectedTree.getTree(), e.getX(), e.getY());
    }
    
    /**
     * Override this method to do something when the user left clicks a node.
     *
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
        selectNode(selectedTree.getCurrentNode());
    }
    
    /**
     * Override this method to do something when the user presses down on a node.
     *
     * @param e
     */
    protected void nodePressed(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user double clicks a node.
     *
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
    }

    /**
     * Reload the data for the current tree.
     */
    protected void refresh() {
    	if (entityRootList == null || entityRootList.isEmpty()) {
            SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

                private List<Entity> rootList;
            	
                protected void doStuff() throws Exception {
                	rootList = loadRootList();
                }

                protected void hadSuccess() {
                	init(rootList);
                }

                protected void hadError(Throwable error) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(EntityOutline.this, 
                    		"Error loading data outline", "Data Load Error", JOptionPane.ERROR_MESSAGE);
                    init(null);
                }
            };
            
            entityOutlineLoadingWorker.execute();
    	}
    	else if (getRootEntity()!=null) {
	        Utils.setWaitingCursor(EntityOutline.this);
	    	final ExpansionState expansionState = new ExpansionState();
	    	expansionState.storeExpansionState(getDynamicTree());
	    	initializeTree(getRootEntity().getId(), new Callable<Void>() {
				@Override
				public Void call() throws Exception {
			    	expansionState.restoreExpansionState(getDynamicTree());
	                Utils.setDefaultCursor(EntityOutline.this);
					return null;
				}
			});
    	}
    }
    
    private void selectNode(DefaultMutableTreeNode node) {
    	selectEntity((Entity)node.getUserObject());
    }
    
    private void selectEntity(Entity entity) {
    	selectEntityById(entity.getId());
    }
    
    private void selectEntityById(long entityId) {
    	
    	DefaultMutableTreeNode node = getNodeByEntityId(entityId);
    	if (node==null) return;
    	
    	Entity entity = (Entity)node.getUserObject();
    	if (entity==null) return;
    	if (Utils.areSame(entity, selectedEntity)) return;
    	
    	DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
    	if (parent!=null && !getTree().isExpanded(new TreePath(parent.getPath()))) {
    		getDynamicTree().expand(parent, true);
    	}
    	
    	getDynamicTree().navigateToNode(node);
    	
    	selectedEntity = entity;
    	
        ModelMgr.getModelMgr().selectEntity(entity.getId(), true);
    	
    	revalidate();
    	repaint();
    	
    	// Load the children in the tree in case the user selects them in the gallery view
    	// TODO: this should pause the UI because it could cause a desync in theory, if it loads slower then 
    	// the user clicks.
        SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, false);
        loadingWorker.execute();
    }
}
