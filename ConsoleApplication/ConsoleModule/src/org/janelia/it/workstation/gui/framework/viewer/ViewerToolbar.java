package org.janelia.it.workstation.gui.framework.viewer;

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
    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton pathButton;
    protected JButton refreshButton;

    protected int currImageSize;

    public ViewerToolbar() {
        super(new BorderLayout());

        toolbar = new JToolBar();
        toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        add(toolbar);

        prevButton = new JButton();
        prevButton.setIcon(Icons.getIcon("arrow_back.gif"));
        prevButton.setToolTipText("Go back in your browsing history");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        });
        prevButton.addMouseListener(new MouseForwarder(toolbar, "PrevButton->JToolBar"));
        toolbar.add(prevButton);

        nextButton = new JButton();
        nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
        nextButton.setFocusable(false);
        nextButton.setToolTipText("Go forward in your browsing history");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goForward();
            }
        });
        nextButton.addMouseListener(new MouseForwarder(toolbar, "NextButton->JToolBar"));
        toolbar.add(nextButton);

        pathButton = new JButton();
        pathButton.setIcon(Icons.getIcon("path-blue.png"));
        pathButton.setFocusable(false);
        pathButton.setToolTipText("See the current location");
        pathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPopupPathMenu();
            }
        });
        pathButton.addMouseListener(new MouseForwarder(toolbar, "ParentButton->JToolBar"));
        toolbar.add(pathButton);

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

    protected abstract void goBack();

    protected abstract void goForward();

    protected abstract void refresh();

    protected abstract JPopupMenu getPopupPathMenu();

    private void showPopupPathMenu() {
        JPopupMenu menu = getPopupPathMenu();
        if (menu == null) {
            return;
        }
        menu.show(pathButton, 0, pathButton.getHeight());
    }

    public JToolBar getToolbar() {
        return toolbar;
    }

    public JButton getPrevButton() {
        return prevButton;
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public JButton getPathButton() {
        return pathButton;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public int getCurrImageSize() {
        return currImageSize;
    }
}
