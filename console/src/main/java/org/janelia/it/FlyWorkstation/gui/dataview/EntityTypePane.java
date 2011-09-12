package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * The left-hand panel which lists the Entity types and their attributes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTypePane extends JScrollPane {

    private SimpleWorker loadTask;
    private JTree tree;

    public EntityTypePane() {
        tree = new JTree(new DefaultMutableTreeNode("Loading..."));
        setViewportView(tree);

        tree.addMouseListener(new MouseHandler() {
			@Override
			protected void popupTriggered(MouseEvent e) {
                tree.setSelectionRow(tree.getRowForLocation(e.getX(), e.getY()));
				showPopupMenu(e);
			}
			@Override
			protected void singleLeftClicked(MouseEvent e) {
                TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof EntityType) {
                    DataviewApp.getMainFrame().getEntityListPane().showEntities((EntityType) node.getUserObject());
                    tree.setSelectionPath(path);
                }
                else if (node.getUserObject() instanceof EntityAttribute) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    DataviewApp.getMainFrame().getEntityListPane().showEntities((EntityType) parent.getUserObject());
                    tree.setSelectionPath(path.getParentPath());
                }
			}
        });
    }
    
    private void showPopupMenu(MouseEvent e) {

        TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        if (node.isRoot()) {

            JMenuItem addTypeMenuItem = new JMenuItem("Add entity type");
            addTypeMenuItem.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    	            String typeName = (String) JOptionPane.showInputDialog(DataviewApp.getMainFrame(), "Name:\n", "Add Entity Type", JOptionPane.PLAIN_MESSAGE, null, null, null);
    	            if (Utils.isEmpty(typeName)) return;
    	            
    	            try {
    	            	ModelMgr.getModelMgr().createEntityType(typeName);	
    	            	refresh();
    	            }
    				catch (Exception x) {
                        JOptionPane.showMessageDialog(DataviewApp.getMainFrame(), "Error adding type "+x.getMessage());
    				}
    			}
    		});
            popupMenu.add(addTypeMenuItem);
        }
        else if (node.getUserObject() instanceof EntityType) {

        	final EntityType entityType = (EntityType)node.getUserObject();
        	
            JMenuItem addAttrMenuItem = new JMenuItem("Add attribute");
            addAttrMenuItem.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    	            String attrName = (String) JOptionPane.showInputDialog(DataviewApp.getMainFrame(), "Name:\n", "Add Entity Attribute", JOptionPane.PLAIN_MESSAGE, null, null, null);
    	            if (Utils.isEmpty(attrName)) return;
    	            
    	            try {
    	            	ModelMgr.getModelMgr().createEntityAttribute(entityType.getName(), attrName);		
    	            	refresh();
    	            }
    				catch (Exception x) {
                        JOptionPane.showMessageDialog(DataviewApp.getMainFrame(), "Error adding attribute "+x.getMessage());
    				}
    			}
    		});
            popupMenu.add(addAttrMenuItem);
        }
        else if (node.getUserObject() instanceof EntityAttribute) {

        }
        
        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }
    
    public void refresh() {

        loadTask = new SimpleWorker() {

            private TreeModel model;

            @Override
            protected void doStuff() throws Exception {
                List<EntityType> entityTypes = ModelMgr.getModelMgr().getEntityTypes();

                DefaultMutableTreeNode root = new DefaultMutableTreeNode("EntityType");

                for (EntityType entityType : entityTypes) {
                    DefaultMutableTreeNode entityTypeNode = new DefaultMutableTreeNode(entityType) {
                        @Override
                        public String toString() {
                            return ((EntityType) getUserObject()).getName();
                        }
                    };
                    root.add(entityTypeNode);

                    for (EntityAttribute entityAttribute : entityType.getOrderedAttributes()) {
                        DefaultMutableTreeNode entityAttrNode = new DefaultMutableTreeNode(entityAttribute) {
                            @Override
                            public String toString() {
                                return ((EntityAttribute) getUserObject()).getName();
                            }
                        };
                        entityTypeNode.add(entityAttrNode);
                    }
                }
                model = new DefaultTreeModel(root);
            }

            @Override
            protected void hadSuccess() {
                tree.setModel(model);
                expandAll();
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    public void expandAll() {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

}