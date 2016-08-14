package org.janelia.it.workstation.gui.browser.gui.tree;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Node;

/**
 * A toolbar for trees, providing basic functionality.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class CustomTreeToolbar extends JPanel {
    
    private final JToolBar toolBar;
    private final JButton expandAllButton;
    private final JButton collapseAllButton;
    private final JButton refreshButton;
    
    public CustomTreeToolbar(final CustomTreeView treeView) {
        super(new BorderLayout());

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
        toolBar.add(expandAllButton);
        toolBar.add(collapseAllButton);
        add(toolBar, BorderLayout.CENTER);
    }

    public JToolBar getJToolBar() {
        return toolBar;
    }

    /**
     * Called when the refresh button is pressed. Override this method to provide refresh functionality. 
     */
    protected abstract void refresh();
}
