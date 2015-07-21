package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.janelia.it.workstation.gui.browser.actions.NamedAction;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Common base class for pop-up context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PopupContextMenu extends JPopupMenu {

    protected static final Browser browser = SessionMgr.getBrowser();
    protected static final Component mainFrame = SessionMgr.getMainFrame();
    
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

    protected JMenuItem getNamedActionItem(final NamedAction action) {
        JMenuItem actionMenuItem = new JMenuItem(action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.doAction();
            }
        });
        return actionMenuItem;
    }

    protected JMenuItem getActionItem(final Action action) {
        String name = (String) action.getValue(Action.NAME);
        JMenuItem actionMenuItem = new JMenuItem(name);
        actionMenuItem.addActionListener(action);
        return actionMenuItem;
    }
}
