package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Special tree cell renderer for generic Entity trees.
 */
public class EntityTreeCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {
    private static final Color typeLabelColor = new Color(149, 125, 71);
    private static final Color keybindLabelColor = new Color(128, 128, 128);

    private JLabel titleLabel;
    private JLabel typeLabel;
    private JLabel keybindLabel;
    private JPanel cellPanel;
    private Color foregroundSelectionColor;
    private Color foregroundNonSelectionColor;
    private Color backgroundSelectionColor;
    private Color backgroundNonSelectionColor;
    private DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    
    public EntityTreeCellRenderer() {
    	
        cellPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        titleLabel = new JLabel(" ");
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
        titleLabel.setForeground(Color.black);
        cellPanel.add(titleLabel);

        typeLabel = new JLabel(" ");
        typeLabel.setForeground(EntityTreeCellRenderer.typeLabelColor);
        cellPanel.add(typeLabel);

        keybindLabel = new JLabel(" ");
        keybindLabel.setForeground(EntityTreeCellRenderer.keybindLabelColor);
        cellPanel.add(keybindLabel);

        foregroundSelectionColor = defaultRenderer.getTextSelectionColor();
        foregroundNonSelectionColor = defaultRenderer.getTextNonSelectionColor();
        backgroundSelectionColor = defaultRenderer.getBackgroundSelectionColor();
        backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();

        cellPanel.setBackground(backgroundNonSelectionColor);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) || (value instanceof DefaultMutableTreeNode)) {

            // Set the colors

            if (selected) {
                titleLabel.setForeground(foregroundSelectionColor);
                titleLabel.setBackground(backgroundSelectionColor);
            }
            else {
                titleLabel.setForeground(foregroundNonSelectionColor);
                titleLabel.setBackground(backgroundNonSelectionColor);
            }

            // Set the default icon

            cellPanel.setEnabled(tree.isEnabled());
            if (leaf) titleLabel.setIcon(getLeafIcon());
            else if (expanded) titleLabel.setIcon(getOpenIcon());
            else titleLabel.setIcon(getClosedIcon());
            
            // Set everything else based on the entity properties
            
        	DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (node instanceof LazyTreeNode) {
                titleLabel.setText("");
                typeLabel.setText("");
                titleLabel.setIcon(null);
            }
            if (userObject instanceof Entity) {
            	Entity entity = (Entity) userObject;

                // Set the labels

                titleLabel.setText(entity.getName());

                String entityTypeName = entity.getEntityType().getName();
                
                typeLabel.setText("");
                
                try {
                    if (entityTypeName.equals(EntityConstants.TYPE_FOLDER)) {
                        titleLabel.setIcon(Utils.getClasspathImage("folder.png"));
                    }
                    else if (entityTypeName.equals(EntityConstants.TYPE_LSM_STACK_PAIR) ||
                    		entityTypeName.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        titleLabel.setIcon(Utils.getClasspathImage("folder_image.png"));
                    }
                    else if (entityTypeName.equals(EntityConstants.TYPE_SAMPLE)) {
                        titleLabel.setIcon(Utils.getClasspathImage("beaker.png"));
                    }
                    else if (entityTypeName.equals(EntityConstants.TYPE_TIF_2D)) {
                        titleLabel.setIcon(Utils.getClasspathImage("image.png"));
                    }
                    else if (entityTypeName.equals(EntityConstants.TYPE_TIF_3D) || 
                    		entityTypeName.equals(EntityConstants.TYPE_LSM_STACK) || 
                    		entityTypeName.equals(EntityConstants.TYPE_TIF_3D_LABEL_MASK)) {
                        titleLabel.setIcon(Utils.getClasspathImage("images.png"));
                    }
                    else {
                        titleLabel.setIcon(Utils.getClasspathImage("page.png"));
                    }

                }
                catch (Throwable r) {
                    r.printStackTrace();
                }
            }
            returnValue = cellPanel;
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}
