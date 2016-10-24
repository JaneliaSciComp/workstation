package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.border.Border;

import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;

/**
 * The main viewer panel that contains two viewers: a main viewer and a secondary viewer that may be closed (and begins
 * in a closed state). Also implements the concept of an "active" viewer and paints a selection border around the
 * currently active viewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerSplitPanel extends JPanel implements ViewerContainer {

    private final Border normalBorder;
    private final Border focusBorder;

    private boolean mainViewerOnly = true;
    private JSplitPane mainSplitPane;
    private ViewerPane mainViewerPane;
    private ViewerPane secViewerPane;
    private ViewerPane activeViewerPane;

    public ViewerSplitPanel() {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());

        Color panelColor = (Color) UIManager.get("Panel.background");
        Color normalColor = (Color) UIManager.get("windowBorder");
        Color focusColor = (Color) UIManager.get("Focus.color");

        normalBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(normalColor, 1), BorderFactory.createLineBorder(panelColor, 1));
        focusBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focusColor, 1), BorderFactory.createLineBorder(focusColor, 1));

        mainViewerPane = new ViewerPane(this, EntitySelectionModel.CATEGORY_MAIN_VIEW, false);
        mainViewerPane.setTitle(" ");
        add(mainViewerPane, BorderLayout.CENTER);

        secViewerPane = new ViewerPane(this, EntitySelectionModel.CATEGORY_SEC_VIEW, true) {
            @Override
            protected void closeButtonPressed() {
                setSecViewerVisible(false);
            }
        };
        secViewerPane.setTitle(" ");

        this.mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        mainSplitPane.setOneTouchExpandable(false);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        mainSplitPane.setDividerSize(10);

        Dimension minimumSize = new Dimension(0, 0);
        mainViewerPane.setMinimumSize(minimumSize);
        secViewerPane.setMinimumSize(minimumSize);

        setActiveViewerPane(mainViewerPane);
    }

    public JSplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    @Override
    public void setActiveViewerPane(ViewerPane viewerPane) {

        activeViewerPane = viewerPane;

        // Update borders
        if (activeViewerPane == mainViewerPane) {
            secViewerPane.setBorder(normalBorder);
            mainViewerPane.setBorder(focusBorder);
        }
        else if (activeViewerPane == secViewerPane) {
            mainViewerPane.setBorder(normalBorder);
            secViewerPane.setBorder(focusBorder);
        }
        else {
            throw new IllegalArgumentException("Unknown ViewerPane with class " + viewerPane.getClass().getName());
        }
    }

    @Override
    public ViewerPane getActiveViewerPane() {
        return activeViewerPane;
    }

    public ViewerPane getMainViewerPane() {
        return mainViewerPane;
    }

    public ViewerPane getSecViewerPane() {
        return secViewerPane;
    }

    public boolean isSecViewerVisible() {
        return !mainViewerOnly;
    }

    public void setSecViewerVisible(boolean visible) {

        if (visible) {
            if (mainViewerOnly) {
                remove(mainViewerPane);
                mainSplitPane.setLeftComponent(mainViewerPane);
                mainSplitPane.setRightComponent(secViewerPane);
                add(mainSplitPane, BorderLayout.CENTER);
                revalidate();
                repaint();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mainSplitPane.setDividerLocation(0.5);
                    }
                });
                mainViewerOnly = false;
            }
        }
        else {
            secViewerPane.closeViewer();
            mainViewerOnly = true;
            activeViewerPane = mainViewerPane;
            remove(mainSplitPane);
            add(mainViewerPane, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }
}
