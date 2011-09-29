package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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
public class EntityOutline extends EntityTree implements Cloneable {
    
    private List<Entity> entityRootList;
    
    public EntityOutline() {
    	super(true);
        this.setMinimumSize(new Dimension(400, 400));

        showLoadingIndicator();

        SimpleWorker loadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                entityRootList = ModelMgr.getModelMgr().getCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER);
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
    	
        // Create context menus
        JPopupMenu popupMenu = new JPopupMenu();
        
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
            JMenu changeDataSourceMenu = new JMenu("  Change data source...");

        	for(final Entity commonRoot : entityRootList) {
        		if (!"system".equals(commonRoot.getUser().getUserLogin()) && !SessionMgr.getUsername().equals(commonRoot.getUser().getUserLogin())) continue;

                JMenuItem dataSourceItem = new JMenuItem(commonRoot.getName() +" ("+commonRoot.getUser().getUserLogin()+")");
                dataSourceItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                    	initializeTree(commonRoot.getId());
                    }
                });
                changeDataSourceMenu.add(dataSourceItem);
        	}
        	
            popupMenu.add(changeDataSourceMenu);
        	
    	}
    	    	
    	// Create annotation session (2d images)
    	// TODO: this is deprecated and should be removed once we normalize all the results to use Neuron Fragments
        JMenuItem newSessionItem = new JMenuItem("  Create Annotation Session for 2D Images");
        newSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                DefaultMutableTreeNode node = selectedTree.getCurrentNode();
                final Entity entity = (Entity) node.getUserObject();

                try {
                    Utils.setWaitingCursor(EntityOutline.this);

                    SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, true) {

                        protected void doneLoading() {
                            Utils.setDefaultCursor(EntityOutline.this);
                            List<Entity> entities = getDescendantsOfType(entity, EntityConstants.TYPE_TIF_2D);
                            SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyPanel().showForNewSession(entity.getName(), entities);
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
        popupMenu.add(newSessionItem);
    	
        // Create annotation session (neuron fragments)
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
                            List<Entity> entities = getDescendantsOfType(entity, EntityConstants.TYPE_NEURON_FRAGMENT);
                            SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyPanel().showForNewSession(entity.getName(), entities);
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
                    selectNode(node);
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
        List<Entity> entities = getDescendantsOfType(entity, EntityConstants.TYPE_TIF_2D);
    	if (entities.isEmpty()) return;
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
    

    private void selectNode(DefaultMutableTreeNode node) {

        if (node instanceof LazyTreeNode) return;
    	final Entity entity = (Entity) node.getUserObject();
        ModelMgr.getModelMgr().notifyEntitySelected(entity.getId());
        
        String type = entity.getEntityType().getName();
        List<Entity> entities = new ArrayList<Entity>();

        if (type.equals(EntityConstants.TYPE_TIF_2D)) {
            entities.add(entity);
        	if (entities.isEmpty()) return;
        	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(entities);
        }
        else if (type.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
            
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
    }
}
