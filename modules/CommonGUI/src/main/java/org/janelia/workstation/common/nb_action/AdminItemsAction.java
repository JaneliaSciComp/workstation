package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.integration.spi.actions.AdminActionBuilder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "File",
        id = "AdminItemsAction"
)
@ActionRegistration(
        displayName = "Admin"
)
@ActionReference(path = "Menu/Tools", position = 5000, separatorBefore=4999)
@NbBundle.Messages("CTL_AdminItemsAction=Admin")
public final class AdminItemsAction extends AbstractAction implements Presenter.Menu {

    private static final Logger log = LoggerFactory.getLogger(AdminItemsAction.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing. Action is performed by menu presenter.
    }

    public JMenuItem getMenuPresenter() {
        String name = NbBundle.getBundle(AdminItemsAction.class).getString("CTL_AdminItemsAction");
        return new AdminActionsMenu(name);
    }

    public class AdminActionsMenu extends JMenu implements DynamicMenuContent {

        private List<JComponent> itemCache;

        AdminActionsMenu(String name) {
            super(name);
        }

        private List<JComponent> getItems() {
            // The admin items only need to be created once. If the user's admin status changes, the entire menu
            // will be disabled or enabled
            if (itemCache==null) {

                this.itemCache = new ArrayList<>();
                Collection<? extends AdminActionBuilder> builders = Lookup.getDefault().lookupAll(AdminActionBuilder.class);
                log.debug("Found {} admin action builders", builders.size());

                for (AdminActionBuilder builder : builders) {

                    Action action = builder.getAction();

                    if (action == null) {
                        log.warn("  Action builder accepted object but returned null Action");
                        continue;
                    }

                    if (builder.isPrecededBySeparator()) {
                        log.debug("  Adding pre-separator");
                        itemCache.add(new JSeparator());
                    }

                    if (action instanceof PopupMenuGenerator) {
                        // If the action has a popup generator, use that
                        JMenuItem popupPresenter = ((PopupMenuGenerator) action).getPopupPresenter();
                        if (popupPresenter != null) {
                            log.debug("  Adding popup presenter");
                            itemCache.add(popupPresenter);
                        }
                        else {
                            log.debug("  Popup presenter was null, falling back on wrapping action in menu item");
                            JMenuItem item = new JMenuItem(action);
                            itemCache.add(item);
                        }
                    }
                    else {
                        // Otherwise, just wrap the action
                        log.debug("  Wrapping action in menu item");
                        JMenuItem item = new JMenuItem(action);
                        itemCache.add(item);
                    }


                    if (builder.isSucceededBySeparator()) {
                        log.debug("  Adding post-separator");
                        itemCache.add(new JSeparator());
                    }
                }
            }

            return itemCache;
        }

        @Override
        public JPopupMenu getPopupMenu() {
            JPopupMenu pm = super.getPopupMenu();
            pm.removeAll();
            for (JComponent item : getItems()) {
                pm.add(item);
            }
            boolean enabled = AccessManager.getAccessManager().isAdmin();
            setEnabled(enabled);
            pm.pack();
            return pm;
        }

        public JComponent[] getMenuPresenters() {
            return new JComponent[] { this };
        }

        public JComponent[] synchMenuPresenters(JComponent[] items) {
            getPopupMenu();
            return items;
        }

    }
}
