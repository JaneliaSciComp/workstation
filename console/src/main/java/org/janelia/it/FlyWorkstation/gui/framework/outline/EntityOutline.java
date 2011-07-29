package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeExpansionWorker;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
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
public class EntityOutline extends EntityTree implements Cloneable {

    private static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    private static final String JACS_DATA_PATH_LINUX = ConsoleProperties.getString("remote.defaultLinuxPath");

    public EntityOutline() {
        this.setMinimumSize(new Dimension(400,400));

        showLoadingIndicator();

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

    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {
        // Create context menus
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem newSessionItem = new JMenuItem("Create Annotation Session for 2D Images");
        newSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

            	DefaultMutableTreeNode node = selectedTree.getCurrentNode();
        		final Entity entity = (Entity)node.getUserObject();

            	try {
            		Utils.setWaitingCursor(EntityOutline.this);

            		SimpleWorker loadingWorker = new LazyTreeNodeExpansionWorker(selectedTree, node, true) {

            			protected void doneExpanding() {
            				Utils.setDefaultCursor(EntityOutline.this);
                    		List<Entity> entities = getDescendantsOfType(entity, EntityConstants.TYPE_TIF_2D);
                    		SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyPanel().showForNewSession(entity.getName(), entities);
        		            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
            			}

						@Override
						protected void hadError(Throwable error) {
							Utils.setDefaultCursor(EntityOutline.this);
							JOptionPane.showMessageDialog(EntityOutline.this, "Error expanding tree", "Internal Error", JOptionPane.ERROR_MESSAGE);
						}
                    };

                    loadingWorker.execute();
            	}
            	catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        });
        DefaultMutableTreeNode node = selectedTree.getCurrentNode();
        final Entity entity = (Entity)node.getUserObject();
        String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        filepath = convertPath(filepath);
        final File file = new File(filepath);
        if (file.isFile() && (file.getAbsolutePath().toLowerCase().endsWith(".tif") || file.getAbsolutePath().toLowerCase().endsWith(".lsm"))) {
            JMenuItem v3dMenuItem = new JMenuItem("Show in V3D");
            v3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    String tmpCmd = "/Users/"+(String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME)
                                +"/Dev/v3d/v3d/v3d64.app/Contents/MacOS/v3d64 -i "+ file.getAbsolutePath();
                    System.out.println("DEBUG: "+tmpCmd);
                    try {
                        Runtime.getRuntime().exec(tmpCmd);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            popupMenu.add(v3dMenuItem);
        }

        popupMenu.add(newSessionItem);
        popupMenu.show(selectedTree.getTree(), e.getX(), e.getY());
    }

    public String convertPath(String filepath) {
    	return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);
    }

    /**
     * Override this method to do something when the user left clicks a node.
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
    	DefaultMutableTreeNode node = selectedTree.getCurrentNode();
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

    	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(new GlobalSession(entities));
    }

    /**
     * Override this method to do something when the user presses down on a node.
     * @param e
     */
    protected void nodePressed(MouseEvent e) {
    }
    
    /**
     * Override this method to do something when the user double clicks a node.
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
    }
    
}
