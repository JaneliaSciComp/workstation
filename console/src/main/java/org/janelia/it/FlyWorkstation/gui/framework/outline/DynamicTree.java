package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyTermType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:55 PM
 */
public class DynamicTree extends JPanel {

	private static final Color typeLabelColor = new Color(149, 125, 71);
	private static final Color keybindLabelColor = new Color(128, 128, 128);
	
    protected EntityMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
    protected JTree tree;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();

   
    public DynamicTree(OntologyTerm rootTerm) {
        super(new GridLayout(1, 0));

        rootNode = new EntityMutableTreeNode(rootTerm);
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setRowHeight(25);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new EntityCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(300,800));
        add(scrollPane);
    }

    public JTree getTree() {
        return tree;
    }

    public EntityMutableTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultTreeModel getTreeModel() {
		return treeModel;
	}

	/**
     * Remove the currently selected node.
     */
    public void removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            EntityMutableTreeNode currentNode = (EntityMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                return;
            }
        }

        // Either there was no selection, or the root was selected.
        toolkit.beep();
    }

    /**
      * Get the currently selected node name.
      */
     public EntityMutableTreeNode getCurrentNode() {
         TreePath currentSelection = tree.getSelectionPath();
         if (currentSelection != null) {
             return (EntityMutableTreeNode) (currentSelection.getLastPathComponent());
         }
         return null;
     }

    /**
      * Get the currently selected node name.
      */
     public String getCurrentNodeName() {
         EntityMutableTreeNode treeNode = getCurrentNode();
         if (treeNode != null) return treeNode.getEntityName();
         return null;
     }

    /**
      * Get the currently selected node id.
      */
     public String getCurrentNodeId() {
         EntityMutableTreeNode treeNode = getCurrentNode();
         if (treeNode != null) return treeNode.getEntityId().toString();
         return null;
     }

     /**
     * Add child to the currently selected node.
     */
    public EntityMutableTreeNode addObject(Object child) {
        EntityMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;
        }
        else {
            parentNode = (EntityMutableTreeNode) (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public EntityMutableTreeNode addObject(EntityMutableTreeNode parent, Object child) {
        return addObject(parent, child, false);
    }


    public EntityMutableTreeNode addObject(EntityMutableTreeNode parent, Object child, boolean shouldBeVisible) {
        EntityMutableTreeNode childNode = new EntityMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        // It is key to invoke this on the TreeModel, and NOT EntityMutableTreeNode
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        // Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    public void removeRootChildren() {
        try {
            TreePath currentSelection = tree.getSelectionPath();
            if (currentSelection != null) {
                EntityMutableTreeNode rootNode = (EntityMutableTreeNode) (currentSelection.getPathComponent(0));
                Enumeration enumeration = rootNode.children();
                while(enumeration.hasMoreElements()) {
                    EntityMutableTreeNode currentNode = (EntityMutableTreeNode)enumeration.nextElement();
                    MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
                    if (parent != null) {
                        treeModel.removeNodeFromParent(currentNode);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void expandAll() {
        // expand to the last leaf from the root
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    public EntityMutableTreeNode refreshDescendants(EntityMutableTreeNode currentNode) {
        treeModel.nodeChanged(currentNode);
        Enumeration enumeration = currentNode.children();
        while(enumeration.hasMoreElements()) {
            refreshDescendants((EntityMutableTreeNode)enumeration.nextElement());
        }

        return null;
    }

    public void navigateToNextRow() {

        int[] selection = tree.getSelectionRows();
        if (selection != null && selection.length > 0) {
            int nextRow = selection[0]+1;
            if (nextRow >= tree.getRowCount()) {
                tree.setSelectionRow(0);
            }
            else {
                tree.setSelectionRow(nextRow);
            }
        }
    }

    public void navigateToEntityNode(Entity entity) {

        EntityMutableTreeNode node = getNodeForEntity(entity, (EntityMutableTreeNode)treeModel.getRoot());
        if (node == null) return;
        
        TreePath treePath = new TreePath(node.getPath());
        tree.expandPath(treePath);
        tree.setSelectionPath(treePath);
    }

    private EntityMutableTreeNode getNodeForEntity(Entity entity, EntityMutableTreeNode currentNode) {

        if (currentNode.getEntityId().equals(entity.getId())) {
            return currentNode;
        }

        Enumeration enumeration = currentNode.children();
        while(enumeration.hasMoreElements()) {
            EntityMutableTreeNode childNode = (EntityMutableTreeNode)enumeration.nextElement();
            EntityMutableTreeNode foundNode = getNodeForEntity(entity, childNode);
            if (foundNode != null) return foundNode;
        }

        return null;
    }


    /**
     * Special tree cell renderer for OntologyTerms which displays the term, its type, and its key binding. The icon
     * is customized based on the term type.
     */
    private class EntityCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {
    	
        private JLabel titleLabel;
        private JLabel typeLabel;
        private JLabel keybindLabel;
        private JPanel cellPanel;
        private Color foregroundSelectionColor;
        private Color foregroundNonSelectionColor;
        private Color backgroundSelectionColor;
        private Color backgroundNonSelectionColor;
        private DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

        public EntityCellRenderer() {
        	
            cellPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            titleLabel = new JLabel(" ");
            titleLabel.setOpaque(true);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
            titleLabel.setForeground(Color.black);
            cellPanel.add(titleLabel);
            
            typeLabel = new JLabel(" ");
            typeLabel.setForeground(typeLabelColor);
            cellPanel.add(typeLabel);
            
            keybindLabel = new JLabel(" ");
            keybindLabel.setForeground(keybindLabelColor);
            cellPanel.add(keybindLabel);
            
            foregroundSelectionColor = defaultRenderer.getTextSelectionColor();
            foregroundNonSelectionColor = defaultRenderer.getTextNonSelectionColor();
            backgroundSelectionColor = defaultRenderer
                    .getBackgroundSelectionColor();
            backgroundNonSelectionColor = defaultRenderer
                    .getBackgroundNonSelectionColor();

            cellPanel.setBackground(backgroundNonSelectionColor);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component returnValue = null;
            if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof OntologyTerm) {
                    OntologyTerm term = (OntologyTerm)userObject;

                    // Set the labels
                    
                    titleLabel.setText(term.getEntity().getName());
                    
                    OntologyTermType type = term.getType(); 
                    if (type != null) {
                    	if (type instanceof org.janelia.it.jacs.model.ontology.Interval) {
                    		org.janelia.it.jacs.model.ontology.Interval interval = (org.janelia.it.jacs.model.ontology.Interval)type;
                    		typeLabel.setText(""+term.getType().getName()+" ("+interval.getLowerBound() +"-"+interval.getUpperBound()+")");
                    	}
                    	else {
                    		typeLabel.setText(""+term.getType().getName()+"");	
                    	}
                    }
                    else {
                        typeLabel.setText("[Unknown]");
                    }
                    
                    KeyboardShortcut bind = ConsoleApp.getKeyBindings().getBinding(term.getAction());
                    if (bind != null) {
                        keybindLabel.setText("("+KeymapUtil.getShortcutText(bind)+")");
                    }
                    else {
                        keybindLabel.setText(" ");
                    }

                    // Set the colors 
                    
                    if (selected) {
                        titleLabel.setForeground(foregroundSelectionColor);
                        titleLabel.setBackground(backgroundSelectionColor);
                    } else {
                        titleLabel.setForeground(foregroundNonSelectionColor);
                        titleLabel.setBackground(backgroundNonSelectionColor);
                    }
                    
                    // Set the icon
                    
                    cellPanel.setEnabled(tree.isEnabled());
                    if (leaf)
                      titleLabel.setIcon(getLeafIcon());
                    else if (expanded)
                      titleLabel.setIcon(getOpenIcon());
                    else
                      titleLabel.setIcon(getClosedIcon());

                    try {
                    	// Icons from http://www.famfamfam.com/lab/icons/silk/
                    	
                    	if (type instanceof org.janelia.it.jacs.model.ontology.Category) {
							titleLabel.setIcon(Utils.getImage("folder.png"));
						}
                    	else if (type instanceof org.janelia.it.jacs.model.ontology.Enum) {
							titleLabel.setIcon(Utils.getImage("folder_page.png"));
						}
                    	else if (type instanceof org.janelia.it.jacs.model.ontology.Interval) {
							titleLabel.setIcon(Utils.getImage("page_white_code.png"));
						}
						else if (type instanceof org.janelia.it.jacs.model.ontology.Tag) {
							titleLabel.setIcon(Utils.getImage("page_white.png"));
						}
						else if (type instanceof org.janelia.it.jacs.model.ontology.Text) {
							titleLabel.setIcon(Utils.getImage("page_white_text.png"));
						}
						else if (type instanceof org.janelia.it.jacs.model.ontology.EnumItem) {
							titleLabel.setIcon(Utils.getImage("page.png"));
						}
						
					} catch (Throwable r) {
						r.printStackTrace();
					}
                    returnValue = cellPanel;
                }
            }
            if (returnValue == null) {
                returnValue = defaultRenderer.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
            }
            return returnValue;
        }
    }

    
}
