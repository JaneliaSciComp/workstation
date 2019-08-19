package org.janelia.workstation.browser.gui.support;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;

import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.common.gui.support.ScrollablePanel;

/**
 * A vertical list of SelectablePanels which can be added to a scroll pane.
 * 
 *  Also manages key strokes and mouse clicks to allow the panels to be interacted with,
 *  as well as integration with the HUD.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SelectablePanelListPanel extends ScrollablePanel {

    private final List<SelectablePanel> resultPanels = new ArrayList<>();
    private int currResultIndex = -1;

    public SelectablePanelListPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        addKeyListener(keyListener);
    }
    
    /**
     * Add a panel at the end of the layout.
     * @param resultPanel
     */
    public void addPanel(SelectablePanel resultPanel) {
        resultPanels.add(resultPanel);
        add(resultPanel);
        resultPanel.addKeyListener(keyListener);
        resultPanel.addMouseListener(resultMouseListener);
    }

    /**
     * Remove all existing panels.
     */
    public void clearPanels() {
        resultPanels.clear();
        removeAll();
    }
    
    // Listen for key strokes and execute the appropriate key bindings
    protected KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {

            if (KeymapUtil.isModifier(e)) {
                return;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return;
            }
            
            // No keybinds matched, use the default behavior
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                updateHud(true);
                e.consume();
                return;
            }
            else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                enterKeyPressed();
                return;
            }
            else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                deleteKeyPressed();
                e.consume();
                return;
            }

            SelectablePanel object = null;
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                if (e.isShiftDown()) {
                    object = getPreviousPanel();
                }
                else {
                    object = getNextPanel();
                }
            }
            else {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    object = getPreviousPanel();
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    object = getNextPanel();
                }
                else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    object = getPreviousPanel();
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    object = getNextPanel();
                }
            }

            if (object != null) {
                selectPanel(object, true);
                updateHud(false);
            }

            revalidate();
            repaint();
        }
    };
    
    private void updateHud(boolean toggle) {
        if (!toggle && !Hud.isInitialized()) return;
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
        updateHud(getSelectedPanel(), toggle);
    }
    
    /**
     * Override this method to update the HUD when a certain panel is selected.
     * @param resultPanel the selected panel
     * @param toggle is the user attempting to toggle the HUD?
     */
    protected void updateHud(SelectablePanel resultPanel, boolean toggle) {}

    /**
     * Override this method if you want to know when ENTER has been pressed.
     */
    protected void enterKeyPressed() {}

    /**
     * Override this method if you want to know when DELETE has been pressed.
     */
    protected void deleteKeyPressed() {}
    
    /**
     * Override this method if you want to know when a user has triggered a popup in a panel.
     * @param e mouse event
     * @param resultPanel panel that was clicked on
     */
    protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {}
    
    /**
     * Override this method if you want to know when a user has double clicked a panel.
     * @param e mouse event
     * @param resultPanel panel that was clicked on
     */
    protected void doubleLeftClicked(MouseEvent e, SelectablePanel resultPanel) {}

    /**
     * Override this method if you want to know when a panel has been selected.
     * @param resultPanel the selected panel
     * @param isUserDriven was the selection done explicitly by the user?
     */
    protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {}
    
    // Listener for clicking on result panels
    protected MouseListener resultMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            super.popupTriggered(e);
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            selectPanel(resultPanel, true);
            SelectablePanelListPanel.this.popupTriggered(e, resultPanel);
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            super.doubleLeftClicked(e);
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            selectPanel(resultPanel, true);
            SelectablePanelListPanel.this.doubleLeftClicked(e, resultPanel);
            e.consume();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
                return;
            }
            selectPanel(resultPanel, true);
            e.consume();
        }
    };

    public void selectFirst(boolean isUserDriven) {
        if (!resultPanels.isEmpty()) {
            selectPanel(resultPanels.get(0), isUserDriven);
        }
    }
    
    public void selectPanel(SelectablePanel resultPanel, boolean isUserDriven) {
        if (resultPanel==null) return;
        for(SelectablePanel otherResultPanel : resultPanels) {
            if (resultPanel != otherResultPanel) {
                otherResultPanel.setSelected(false);
            }
        }
        currResultIndex = resultPanels.indexOf(resultPanel);
        resultPanel.setSelected(true);
        panelSelected(resultPanel, isUserDriven);
        
        if (isUserDriven) {
            // Only make this the focused component if the user actually clicked on it. The main thing this does is change the 
            // active key listener, which we don't want to do if the selection is the result of some selection cascade. 
            resultPanel.requestFocus();
            // Update the lightbox if necessary
            updateHud(false);
        }
    }

    public SelectablePanel getSelectedPanel() {
        return resultPanels.get(currResultIndex);
    }
    
    private SelectablePanel getPreviousPanel() {
        if (resultPanels == null) {
            return null;
        }
        int i = currResultIndex;
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return resultPanels.get(i - 1);
    }

    private SelectablePanel getNextPanel() {
        if (resultPanels == null) {
            return null;
        }
        int i = currResultIndex;
        if (i > resultPanels.size() - 2) {
            // Already at the end
            return null;
        }
        return resultPanels.get(i + 1);
    }
    
    private SelectablePanel getSelectablePanelAncestor(Component component) {
        Component c = component;
        while (c!=null) {
            if (c instanceof SelectablePanel) {
                return (SelectablePanel)c;
            }
            c = c.getParent();
        }
        return null;
    }
}
