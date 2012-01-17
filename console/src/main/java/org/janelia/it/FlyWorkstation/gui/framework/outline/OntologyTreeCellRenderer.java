package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

/**
 * Special tree cell renderer for OntologyTerms which displays the term, its type, and its key binding. The icon
 * is customized based on the term type.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyTreeCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {
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
    private OntologyOutline ontologyOutline;

    public OntologyTreeCellRenderer() {
        this(null);
    }

    public OntologyTreeCellRenderer(OntologyOutline ontologyOutline) {

        this.ontologyOutline = ontologyOutline;

        cellPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        titleLabel = new JLabel(" ");
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
        titleLabel.setForeground(Color.black);
        cellPanel.add(titleLabel);

        typeLabel = new JLabel(" ");
        typeLabel.setForeground(OntologyTreeCellRenderer.typeLabelColor);
        cellPanel.add(typeLabel);

        keybindLabel = new JLabel(" ");
        keybindLabel.setForeground(OntologyTreeCellRenderer.keybindLabelColor);
        cellPanel.add(keybindLabel);

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
            if (userObject instanceof OntologyElement) {
                OntologyElement element = (OntologyElement) userObject;
                Entity entity = element.getEntity();

                // Enable panel?
                
                cellPanel.setEnabled(tree.isEnabled());
                
                // Set the labels
                
                titleLabel.setText(element.getEntity().getName());

                OntologyElementType type = element.getType();
                if (type != null) {
                    if (type instanceof Interval) {
                        Interval interval = (Interval) type;
                        typeLabel.setText("" + element.getType().getName() + " (" + interval.getLowerBound() + "-" + interval.getUpperBound() + ")");
                    }
                    else {
                        typeLabel.setText("" + element.getType().getName() + "");
                    }
                }
                else {
                    typeLabel.setText("[Unknown]");
                }


                // Set the key bind hint

                keybindLabel.setText(" ");

                if (ontologyOutline != null) {
                    Action action = ontologyOutline.getActionForNode(node);
                    if (action != null) {
                        KeyboardShortcut bind = SessionMgr.getKeyBindings().getBinding(action);
                        if (bind != null) {
                            keybindLabel.setText("(" + KeymapUtil.getShortcutText(bind) + ")");
                        }
                    }
                    else {
                        System.out.println("Node has a null action: " + element.getName());
                    }
                }

                // Set the colors

                if (selected) {
                    titleLabel.setForeground(foregroundSelectionColor);
                    titleLabel.setBackground(backgroundSelectionColor);
                }
                else {
                    titleLabel.setForeground(foregroundNonSelectionColor);
                    titleLabel.setBackground(backgroundNonSelectionColor);
                }

                // Support drag and drop 
                
                JTree.DropLocation dropLocation = tree.getDropLocation();
                if (dropLocation != null
                        && dropLocation.getChildIndex() == -1
                        && tree.getRowForPath(dropLocation.getPath()) == row) {
                    titleLabel.setForeground(foregroundSelectionColor);
                    titleLabel.setBackground(backgroundSelectionColor);
                }
                
                // Set the icon
                
                if (leaf) titleLabel.setIcon(getLeafIcon());
                else if (expanded) titleLabel.setIcon(getOpenIcon());
                else titleLabel.setIcon(getClosedIcon());
                ImageIcon ontologyIcon = Icons.getOntologyIcon(entity);
                if (ontologyIcon != null) titleLabel.setIcon(ontologyIcon);
                
                returnValue = cellPanel;
            }
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}
