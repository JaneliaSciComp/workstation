package org.janelia.it.workstation.gui.framework.tree;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Position.Bias;
import javax.swing.tree.DefaultMutableTreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A toolbar which sits on top of a DynamicTree and provides generic tree-related functions such as
 * expanding/collapsing all nodes in the tree, and refreshing the tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTreeToolbar extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DynamicTreeToolbar.class);
        
    private final DynamicTree tree;
    private JTextField textField;
    private JButton expandAllButton;
    private JButton collapseAllButton;
    private JButton refreshButton;
    private JToolBar toolBar;

    public DynamicTreeToolbar(final DynamicTree tree) {
        super(new BorderLayout());

        this.tree = tree;
        this.toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
        expandAllButton = new JButton(org.janelia.it.workstation.gui.util.Icons.getExpandAllIcon());
        expandAllButton.setToolTipText("Expand all the nodes in the tree.");
        expandAllButton.setFocusable(false);
        expandAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = tree.getCurrentNode();
                if (node==null) node = tree.getRootNode();
                expandAllButton.setEnabled(false);
                collapseAllButton.setEnabled(false);
                tree.expandAll(node, true);
                expandAllButton.setEnabled(true);
                collapseAllButton.setEnabled(true);
            }
        });
        toolBar.add(expandAllButton);

        collapseAllButton = new JButton(org.janelia.it.workstation.gui.util.Icons.getCollapseAllIcon());
        collapseAllButton.setToolTipText("Collapse all the nodes in the tree.");
        collapseAllButton.setFocusable(false);
        collapseAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = tree.getCurrentNode();
                if (node==null) node = tree.getRootNode();
                if (tree.getCurrentNode()==null) return;
                collapseAllButton.setEnabled(false);
                expandAllButton.setEnabled(false);
                tree.expandAll(node, false);
                collapseAllButton.setEnabled(true);
                expandAllButton.setEnabled(true);
            }
        });
        toolBar.add(collapseAllButton);

        refreshButton = new JButton(org.janelia.it.workstation.gui.util.Icons.getRefreshIcon());
        refreshButton.setToolTipText("Refresh the data in the tree.");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tree.totalRefresh();
            }
        });
        toolBar.add(refreshButton);

        toolBar.addSeparator();

        JLabel label = new JLabel("Find:");
        toolBar.add(label);

        textField = new JTextField(); 
        textField.setColumns(10);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                nextMatch(false);
            }
            public void removeUpdate(DocumentEvent e) {
                nextMatch(false);
            }
            public void insertUpdate(DocumentEvent e) {
                nextMatch(false);
            }
          });
        toolBar.add(textField);

        JButton prevButton = new JButton();
        prevButton.setIcon(org.janelia.it.workstation.gui.util.Icons.getIcon("resultset_previous.png"));
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevMatch(true);
            }
        });
        prevButton.setToolTipText("Find the previous occurence of the search terms");
        toolBar.add(prevButton);
        
        JButton nextButton = new JButton();
        nextButton.setIcon(org.janelia.it.workstation.gui.util.Icons.getIcon("resultset_next.png"));
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch(true);
            }
        });
        nextButton.setToolTipText("Find the next occurence of the search terms");
        toolBar.add(nextButton);
        
        add(toolBar, BorderLayout.PAGE_START);
        
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,0,true),"prevMatch");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0,true),"nextMatch");
        
        getActionMap().put("prevMatch",new AbstractAction("prevMatch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevMatch(true);
            }
        });

        getActionMap().put("nextMatch",new AbstractAction("nextMatch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch(true);
            }
        });
    }
    
    private void prevMatch(boolean skipStartingNode) {
        log.trace("Search backward for node with "+textField.getText());
        tree.navigateToNodeStartingWith(textField.getText(), Bias.Backward, skipStartingNode);
    }
    
    private void nextMatch(boolean skipStartingNode) {
        log.trace("Search forward for node with "+textField.getText());
        tree.navigateToNodeStartingWith(textField.getText(), Bias.Forward, skipStartingNode);
    }

	public JTextField getTextField() {
		return textField;
	}
	
    public JToolBar getJToolBar() {
        return toolBar;
    }
}
