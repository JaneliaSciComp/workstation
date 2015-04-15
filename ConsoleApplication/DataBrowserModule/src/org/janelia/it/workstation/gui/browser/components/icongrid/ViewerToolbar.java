package org.janelia.it.workstation.gui.browser.components.icongrid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;

/**
 * Generic toolbar for viewer panels supporting basic navigation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ViewerToolbar extends JPanel {

    protected JToolBar toolbar;
    protected JButton refreshButton;

    public ViewerToolbar() {
        super(new BorderLayout());

        toolbar = new JToolBar();
        toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        add(toolbar);

        refreshButton = new JButton();
        refreshButton.setIcon(Icons.getRefreshIcon());
        refreshButton.setFocusable(false);
        refreshButton.setToolTipText("Refresh the current view");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        refreshButton.addMouseListener(new MouseForwarder(toolbar, "RefreshButton->JToolBar"));
        toolbar.add(refreshButton);

        toolbar.addSeparator();

        toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->ViewerToolbar"));
    }
    
    protected abstract void refresh();

    public JToolBar getToolbar() {
        return toolbar;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }
}
