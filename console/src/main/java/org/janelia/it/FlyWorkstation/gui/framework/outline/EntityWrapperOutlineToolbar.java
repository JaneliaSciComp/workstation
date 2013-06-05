package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * A toolbar which sits on top of a EntityWrapperOutline and provides functions for refreshing and customizing the view.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityWrapperOutlineToolbar extends JPanel {

    protected JButton expandAllButton;
    protected JButton collapseAllButton;
    protected JButton refreshButton;
    protected JButton configureViewButton;

    public EntityWrapperOutlineToolbar(final EntityWrapperOutline ewo) {
        super(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        toolBar.add(Box.createRigidArea(new Dimension(1,25)));
        
        expandAllButton = new JButton(Icons.getExpandAllIcon());
        expandAllButton.setToolTipText("Expand all the nodes in the tree.");
        expandAllButton.setFocusable(false);
        expandAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DynamicTree tree = ewo.getDynamicTree();
                DefaultMutableTreeNode node = tree.getCurrentNode();
                if (node==null) node = tree.getRootNode();
                if (tree.isLazyLoading() && node==tree.getRootNode()) {
                    int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getSessionMgr().getActiveBrowser(), 
                            "Expanding the entire tree may take a long time. Are you sure you want to do this?", 
                            "Expand All", JOptionPane.YES_NO_OPTION);
                    if (deleteConfirmation != 0) {
                        return;
                    }
                }
                expandAllButton.setEnabled(false);
                collapseAllButton.setEnabled(false);
                tree.expandAll(node, true);
                expandAllButton.setEnabled(true);
                collapseAllButton.setEnabled(true);
            }
        });
        toolBar.add(expandAllButton);

        collapseAllButton = new JButton(Icons.getCollapseAllIcon());
        collapseAllButton.setToolTipText("Collapse all the nodes in the tree.");
        collapseAllButton.setFocusable(false);
        collapseAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DynamicTree tree = ewo.getDynamicTree();
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

        refreshButton = new JButton(Icons.getRefreshIcon());
        refreshButton.setToolTipText("Refresh the data in the tree.");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DynamicTree tree = ewo.getDynamicTree();
                tree.totalRefresh();
            }
        });
        toolBar.add(refreshButton);
        

        configureViewButton = new JButton("View");
        configureViewButton.setIcon(Icons.getIcon("cog.png"));
        configureViewButton.setFocusable(false);
        configureViewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPopupConfigureViewMenu();
            }
        });
        toolBar.add(configureViewButton);
        
        add(toolBar, BorderLayout.PAGE_START);
    }

    private void showPopupConfigureViewMenu() {
        JPopupMenu menu = getPopupConfigureViewMenu();
        if (menu==null) return;
        menu.show(configureViewButton, 0, configureViewButton.getHeight());
    }
    
    protected abstract JPopupMenu getPopupConfigureViewMenu();
}
