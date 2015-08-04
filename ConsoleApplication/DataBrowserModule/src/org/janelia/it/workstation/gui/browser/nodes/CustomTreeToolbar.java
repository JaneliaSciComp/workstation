package org.janelia.it.workstation.gui.browser.nodes;

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
import javax.swing.text.Position;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A toolbar for trees, providing basic expansion, refresh, and search functions.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class CustomTreeToolbar extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(CustomTreeToolbar.class);
    
    private final JToolBar toolBar;
    private final JButton expandAllButton;
    private final JButton collapseAllButton;
    private final JButton refreshButton;
    private final JTextField textField;
    private final CustomTreeView beanTreeView;
    
    public CustomTreeToolbar(final CustomTreeView treeView) {
        super(new BorderLayout());
    
        this.beanTreeView = treeView;

        this.toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        expandAllButton = new JButton(Icons.getExpandAllIcon());
        expandAllButton.setToolTipText("Expand the selected nodes.");
        expandAllButton.setFocusable(false);
        expandAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandAllButton.setEnabled(false);
                collapseAllButton.setEnabled(false);
                for(Node node : treeView.getSelectedNodes()) {
                    treeView.expandNode(node);
                }
                expandAllButton.setEnabled(true);
                collapseAllButton.setEnabled(true);
            }
        });
        toolBar.add(expandAllButton);

        collapseAllButton = new JButton(Icons.getCollapseAllIcon());
        collapseAllButton.setToolTipText("Collapse the selected nodes.");
        collapseAllButton.setFocusable(false);
        collapseAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapseAllButton.setEnabled(false);
                expandAllButton.setEnabled(false);
                for(Node node : treeView.getSelectedNodes()) {
                    treeView.collapseNode(node);
                }
                collapseAllButton.setEnabled(true);
                expandAllButton.setEnabled(true);
            }
        });
        toolBar.add(collapseAllButton);

        refreshButton = new JButton(Icons.getRefreshIcon());
        refreshButton.setToolTipText("Refresh the data in the tree.");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
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
        prevButton.setIcon(Icons.getIcon("resultset_previous.png"));
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevMatch(true);
            }
        });
        prevButton.setToolTipText("Find the previous occurrence of the search terms");
        toolBar.add(prevButton);

        JButton nextButton = new JButton();
        nextButton.setIcon(Icons.getIcon("resultset_next.png"));
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch(true);
            }
        });
        nextButton.setToolTipText("Find the next occurrence of the search terms");
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

    public JToolBar getJToolBar() {
        return toolBar;
    }

    private void prevMatch(boolean skipStartingNode) {
        log.trace("Search backward for node with "+textField.getText());
        beanTreeView.navigateToNodeStartingWith(textField.getText(), Position.Bias.Backward, skipStartingNode);
    }

    private void nextMatch(boolean skipStartingNode) {
        log.trace("Search forward for node with "+textField.getText());
        beanTreeView.navigateToNodeStartingWith(textField.getText(), Position.Bias.Forward, skipStartingNode);
    }

    /**
     * Called when the refresh button is pressed. Override this method to provide refresh functionality. 
     */
    protected abstract void refresh();
}
