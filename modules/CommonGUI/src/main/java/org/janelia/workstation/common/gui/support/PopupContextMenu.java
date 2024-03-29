package org.janelia.workstation.common.gui.support;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * Common base class for pop-up context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PopupContextMenu extends JPopupMenu {

    protected static final Component mainFrame = FrameworkAccess.getMainFrame();
    
    // Internal state
    protected boolean nextAddRequiresSeparator = false;
    
    @Override
    public JMenuItem add(JMenuItem menuItem) {

        if (menuItem == null)
            return null;

        if ((menuItem instanceof JMenu)) {
            JMenu menu = (JMenu) menuItem;
            if (menu.getItemCount() == 0)
                return null;
        }

        if (nextAddRequiresSeparator) {
            addSeparator();
            nextAddRequiresSeparator = false;
        }

        return super.add(menuItem);
    }

    public JMenuItem add(JMenu menu, JMenuItem menuItem) {
        if (menu == null || menuItem == null)
            return null;
        return menu.add(menuItem);
    }

    public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
        this.nextAddRequiresSeparator = nextAddRequiresSeparator;
    }

    protected JMenuItem getNamedActionItem(Action action) {
        return new JMenuItem(action);
    }

    protected JMenuItem getNamedActionItem(String name, ActionListener action) {
        return new JMenuItem(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        });
    }

    protected JMenuItem getActionItem(final Action action) {
        String name = (String) action.getValue(Action.NAME);
        JMenuItem actionMenuItem = new JMenuItem(name);
        actionMenuItem.addActionListener(action);
        return actionMenuItem;
    }

    protected JMenuItem getDisabledItem(String title) {
        JMenuItem titleMenuItem = new JMenuItem(title);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    /**
     * Shortcut to show the menu at the correct location for a MouseEvent.
     * @param e mouse click that requested the menu
     */
    public void show(MouseEvent e) {
        show(e.getComponent(), e.getX(), e.getY());
    }
}
