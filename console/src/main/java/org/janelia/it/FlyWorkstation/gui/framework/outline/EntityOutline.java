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

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
    }

    public void init(List<Entity> entityRootList) {
    	this.entityRootList = entityRootList;
        if (null != entityRootList && entityRootList.size() >= 1) {
            initializeTree(entityRootList.get(0).getId(), null);
        }
        else {
        	showNothing();
            updateUI();
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
				        	changeDataSourceMenu.setPopupMenuVisible(false);
				        	changeDataSourceMenu.setPopupMenuVisible(true);
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
    
    private void viewImageEntities(Entity entity) {
    	// Try to get neuron fragments
        List<Entity> entities = entity.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT);
        // Settle for 2d tifs
        if (entities.isEmpty()) {
        	entities.addAll(entity.getDescendantsOfType(EntityConstants.TYPE_TIF_2D));
        }
    	if (entities.isEmpty()) return; // Nothing found
    	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(entities);
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
    	if (getRootEntity()!=null) {
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
        if (node instanceof LazyTreeNode) return;
    	final Entity entity = (Entity) node.getUserObject();
    	
    	if (Utils.areSame(entity, selectedEntity)) return;
    	selectedEntity = entity;
    	
        ModelMgr.getModelMgr().selectEntity(entity.getId());
        
        // TODO: eventually, everything below here should be moved into a listener on the viewer panel which 
        // listens to the entitySelected event
        
        String type = entity.getEntityType().getName();

        if (type.equals(EntityConstants.TYPE_TIF_2D) 
        		|| type.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
        	List<Entity> entities = entity.getDescendantsOfType(type);
        	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(entities);
        }
        else if (type.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) 
        		|| type.equals(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)) {
            
        	if (getDynamicTree().descendantsAreLoaded(node)) {
        		viewImageEntities(entity);
        		return;
        	}
        	
            Utils.setWaitingCursor(EntityOutline.this);
            
            SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, true) {

                protected void doneLoading() {
                    Utils.setDefaultCursor(EntityOutline.this);
                    viewImageEntities(entity);
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
        else {
        	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().clear();
        }
    }
}
