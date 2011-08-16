package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Position.Bias;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * A toolbar which sits on top of a DynamicTree and provides generic tree-related functions such as
 * expanding/collapsing all nodes in the tree, and searching in the tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTreeToolbar extends JPanel implements ActionListener {

    private static final String EXPAND_ALL = "expand_all";
    private static final String COLLAPSE_ALL = "collapse_all";
    private static final String NEXT_MATCH = "next_match";
    private static final String PREVIOUS_MATCH = "previous_match";

    private final DynamicTree tree;
    private JTextField textField;
    private JButton expandAllButton;
    private JButton collapseAllButton;

    public DynamicTreeToolbar(final DynamicTree tree) {
        super(new BorderLayout());

        this.tree = tree;
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        expandAllButton = new JButton(Icons.expandAllIcon);
        expandAllButton.setActionCommand(EXPAND_ALL);
        expandAllButton.setToolTipText("Expand all the nodes in the tree.");
        expandAllButton.addActionListener(this);
        expandAllButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        toolBar.add(expandAllButton);

        collapseAllButton = new JButton(Icons.collapseAllIcon);
        collapseAllButton.setActionCommand(COLLAPSE_ALL);
        collapseAllButton.setToolTipText("Collapse all the nodes in the tree.");
        collapseAllButton.addActionListener(this);
        collapseAllButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        toolBar.add(collapseAllButton);

        toolBar.addSeparator();

        JLabel label = new JLabel("Find:");
        toolBar.add(label);

        textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(this);
        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        innerPanel.add(textField);
        toolBar.add(innerPanel);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                tree.navigateToNodeStartingWith(textField.getText(), null);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }
        });

        JButton button = new JButton("Next");
        button.setActionCommand(NEXT_MATCH);
        button.setToolTipText("Find the next occurence of the phrase.");
        button.addActionListener(this);
        innerPanel.add(button);


        button = new JButton("Previous");
        button.setActionCommand(PREVIOUS_MATCH);
        button.setToolTipText("Find the previous occurence of the phrase.");
        button.addActionListener(this);
        innerPanel.add(button);

        add(toolBar, BorderLayout.PAGE_START);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (EXPAND_ALL.equals(cmd)) {
        	if (tree.isLazyLoading()) {
                int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getSessionMgr().getActiveBrowser(), 
                		"Expanding the entire tree may take a long time. Are you sure you want to do this?", 
                		"Expand All", JOptionPane.YES_NO_OPTION);
                if (deleteConfirmation != 0) {
                    return;
                }
        	}
            expandAllButton.setEnabled(false);
            collapseAllButton.setEnabled(false);
            tree.expandAll(true);
            expandAllButton.setEnabled(true);
            collapseAllButton.setEnabled(true);
        }
        else if (COLLAPSE_ALL.equals(cmd)) {
            collapseAllButton.setEnabled(false);
            expandAllButton.setEnabled(false);
            tree.expandAll(false);
            collapseAllButton.setEnabled(true);
            expandAllButton.setEnabled(true);
        }
        else if (NEXT_MATCH.equals(cmd)) {
            tree.navigateToNodeStartingWith(textField.getText(), Bias.Forward);

        }
        else if (PREVIOUS_MATCH.equals(cmd)) {
            tree.navigateToNodeStartingWith(textField.getText(), Bias.Backward);
        }
    }

}
