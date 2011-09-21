package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Special tree cell renderer for generic Entity trees.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTreeCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {
    protected static final Color typeLabelColor = new Color(149, 125, 71);
    protected static final Color keybindLabelColor = new Color(128, 128, 128);
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    protected JLabel titleLabel;
    protected JLabel typeLabel;
    protected JLabel keybindLabel;
    protected JPanel cellPanel;
    protected Color foregroundSelectionColor;
    protected Color foregroundNonSelectionColor;
    protected Color backgroundSelectionColor;
    protected Color backgroundNonSelectionColor;
    protected DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

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
                String entityTypeName = entity.getEntityType().getName();
                
                // Set the labels
                titleLabel.setText(entity.getName());
                titleLabel.setIcon(Icons.getIcon(entity));
                titleLabel.setToolTipText(entityTypeName);

                typeLabel.setText("");
                if (entity.getName().equals("Neuron Fragments") && !entity.getEntityData().isEmpty()) {
                	typeLabel.setText("("+entity.getEntityData().size()+")");
                }
                else if (entityTypeName.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                	typeLabel.setText("("+df.format(entity.getCreationDate())+")");
                }
                
//                try {
//                    if (entityTypeName.equals(EntityConstants.TYPE_FOLDER)) {
//                        titleLabel.setIcon(Icons.getIcon("folder.png"));
//                        titleLabel.setToolTipText("Folder");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_LSM_STACK_PAIR)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("folder_image.png"));
//                        titleLabel.setToolTipText("LSM Stack Pair");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("folder_image.png"));
//                        titleLabel.setToolTipText("LSM Stack Pair");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_SAMPLE)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("beaker.png"));
//                        titleLabel.setToolTipText("Sample");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_TIF_2D)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("image.png"));
//                        titleLabel.setToolTipText("TIF 2D Image");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_TIF_3D) || entityTypeName.equals(EntityConstants.TYPE_LSM_STACK) || entityTypeName.equals(EntityConstants.TYPE_TIF_3D_LABEL_MASK)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("images.png"));
//                        titleLabel.setToolTipText("TIF 3D Stack");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_STITCHED_V3D_RAW)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("images.png"));
//                        titleLabel.setToolTipText("Stitched V3D Raw Image");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("brick.png"));
//                        titleLabel.setToolTipText("Neuron Fragment");
//                    }
//                    else if (entityTypeName.equals(EntityConstants.TYPE_SUPPORTING_DATA)) {
//                        titleLabel.setIcon(Utils.getClasspathImage("folder_page.png"));
//                        titleLabel.setToolTipText("Supporting data");
//                    }
//                    else {
//                        titleLabel.setIcon(Utils.getClasspathImage("page.png"));
//                        titleLabel.setToolTipText("Page");
//                    }
//                    
//                }
//                catch (Throwable r) {
//                    r.printStackTrace();
//                }
            }

            returnValue = cellPanel;
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}
