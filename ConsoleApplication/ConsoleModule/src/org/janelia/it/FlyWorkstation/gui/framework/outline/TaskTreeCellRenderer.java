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

import org.janelia.it.jacs.model.tasks.Task;

/**
 * Special tree cell renderer for Tasks.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskTreeCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {

    private static final Color typeLabelColor = new Color(72, 92, 168);
    private static final Color statusLabelColor = new Color(217, 182, 250);
    
    private JLabel titleLabel;
    private JLabel typeLabel;
    private JLabel statusLabel;
    private JPanel cellPanel;
    private Color foregroundSelectionColor;
    private Color foregroundNonSelectionColor;
    private Color backgroundSelectionColor;
    private Color backgroundNonSelectionColor;
    private DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    
    public TaskTreeCellRenderer() {

        cellPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        titleLabel = new JLabel(" ");
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
        titleLabel.setForeground(Color.black);
        cellPanel.add(titleLabel);

        typeLabel = new JLabel(" ");
        typeLabel.setForeground(TaskTreeCellRenderer.typeLabelColor);
        cellPanel.add(typeLabel);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(TaskTreeCellRenderer.statusLabelColor);
        cellPanel.add(statusLabel);

        foregroundSelectionColor = defaultRenderer.getTextSelectionColor();
        foregroundNonSelectionColor = defaultRenderer.getTextNonSelectionColor();
        backgroundSelectionColor = defaultRenderer.getBackgroundSelectionColor();
        backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();

        cellPanel.setBackground(backgroundNonSelectionColor);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            if (userObject instanceof Task) {
                Task task = (Task) userObject;

                // Enable panel?
                
                cellPanel.setEnabled(tree.isEnabled());
                
                // Set the labels
                
                titleLabel.setText(task.getJobName());
                typeLabel.setText("["+task.getLastEvent().getEventType()+"]");
                statusLabel.setText(" ");

                // Set the colors

                if (selected) {
                    titleLabel.setForeground(foregroundSelectionColor);
                    titleLabel.setBackground(backgroundSelectionColor);
                }
                else {
                    titleLabel.setForeground(foregroundNonSelectionColor);
                    titleLabel.setBackground(backgroundNonSelectionColor);
                }

                // Set the icon
                
                if (leaf) titleLabel.setIcon(getLeafIcon());
                else if (expanded) titleLabel.setIcon(getOpenIcon());
                else titleLabel.setIcon(getClosedIcon());
//                ImageIcon taskIcon = Icons.getOntologyIcon(task);
//                if (taskIcon != null) titleLabel.setIcon(taskIcon);
                
                returnValue = cellPanel;
            }
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}
