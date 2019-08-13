package org.janelia.workstation.common.actions;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.openide.awt.DynamicMenuContent;

/**
 * Base class for actions which feature a popup menu of items.
 *
 * Takes care of the popup and menu presenters so that they reflect the isVisible/isEnabled state of the action,
 * and update when the item list changes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BaseContextualPopupAction extends BaseContextualNodeAction {

    @Override
    public void performAction() {
        // Actions are performed by the popup
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (!isVisible()) return null;
        JMenu newFolderMenu = new JMenu(getName());
        for (JComponent item : getItems()) {
            newFolderMenu.add(item);
        }
        newFolderMenu.setEnabled(BaseContextualPopupAction.this.isEnabled());
        return newFolderMenu;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return new Menu(getName());
    }

    private class Menu extends JMenu implements DynamicMenuContent {

        Menu(String name) {
            super(name);
        }

        @Override
        public JPopupMenu getPopupMenu() {
            JPopupMenu pm = super.getPopupMenu();
            pm.removeAll();
            for (JComponent item : getItems()) {
                if (item==null) {
                    pm.addSeparator();
                }
                else {
                    pm.add(item);
                }
            }
            setEnabled(BaseContextualPopupAction.this.isEnabled());
            pm.pack();
            return pm;
        }

        @Override
        public JComponent[] getMenuPresenters() {
            return new JComponent[] { this };
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] items) {
            getPopupMenu();
            return items;
        }
    }

    /**
     * Implement this method to return a list of components which appear in the popup list. These should generally be
     * JMenuItems, JMenus, and JSeparators.
     * @return list of components to show in the popup menu
     */
    protected abstract List<JComponent> getItems();
}
