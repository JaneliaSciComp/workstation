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

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;

/**
 * Special tree cell renderer for OntologyTerms which displays the term, its type, and its key binding. The icon
 * is customized based on the term type.
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
    private DynamicTree dtree;
    
    public OntologyTreeCellRenderer(DynamicTree dtree) {

    	this.dtree = dtree;
    	
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


                Action action = dtree.getActionForNode(node);
                KeyboardShortcut bind = ConsoleApp.getKeyBindings().getBinding(action);
                if (bind != null) {
                    keybindLabel.setText("(" + KeymapUtil.getShortcutText(bind) + ")");
                }
                else {
                    keybindLabel.setText(" ");
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

                // Set the icon

                cellPanel.setEnabled(tree.isEnabled());
                if (leaf) titleLabel.setIcon(getLeafIcon());
                else if (expanded) titleLabel.setIcon(getOpenIcon());
                else titleLabel.setIcon(getClosedIcon());

                try {
                    // Icons from http://www.famfamfam.com/lab/icons/silk/

                    if (type instanceof Category) {
                        titleLabel.setIcon(Utils.getClasspathImage("folder.png"));
                    }
                    else if (type instanceof org.janelia.it.jacs.model.ontology.types.Enum) {
                        titleLabel.setIcon(Utils.getClasspathImage("folder_page.png"));
                    }
                    else if (type instanceof Interval) {
                        titleLabel.setIcon(Utils.getClasspathImage("page_white_code.png"));
                    }
                    else if (type instanceof Tag) {
                        titleLabel.setIcon(Utils.getClasspathImage("page_white.png"));
                    }
                    else if (type instanceof Text) {
                        titleLabel.setIcon(Utils.getClasspathImage("page_white_text.png"));
                    }
                    else if (type instanceof EnumItem) {
                        titleLabel.setIcon(Utils.getClasspathImage("page.png"));
                    }

                }
                catch (Throwable r) {
                    r.printStackTrace();
                }
                returnValue = cellPanel;
            }
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}
