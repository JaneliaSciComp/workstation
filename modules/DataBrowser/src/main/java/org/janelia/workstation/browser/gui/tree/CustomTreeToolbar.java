package org.janelia.workstation.browser.gui.tree;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.janelia.workstation.common.gui.support.Icons;
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
                for (Node node : treeView.getSelectedNodes()) {
                    treeView.expandOrCollapseAll(treeView.getTreePath(node), true);
                }
                expandAllButton.setEnabled(true);
                collapseAllButton.setEnabled(true);
            }
        });

        collapseAllButton = new JButton(Icons.getCollapseAllIcon());
        collapseAllButton.setToolTipText("Collapse the selected nodes.");
        collapseAllButton.setFocusable(false);
        collapseAllButton.addActionListener(e -> {
            collapseAllButton.setEnabled(false);
            expandAllButton.setEnabled(false);
            for(Node node : treeView.getSelectedNodes()) {
                treeView.expandOrCollapseAll(treeView.getTreePath(node), false);
            }
            collapseAllButton.setEnabled(true);
            expandAllButton.setEnabled(true);
        });

        refreshButton = new JButton(Icons.getRefreshIcon());
        refreshButton.setToolTipText("Refresh the data in the tree.");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(e -> refresh());
        
        toolBar.add(refreshButton);
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
