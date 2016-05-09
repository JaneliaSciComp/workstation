package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

//@ActionID(
//        category = "Services",
//        id = "FlyLineReleasesMenuAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_FlyLineReleasesMenuAction",
//        lazy = false
//)
//@ActionReference(path = "Menu/Services", position = 200)
//@Messages("CTL_FlyLineReleasesMenuAction=Fly Line Releases")
public final class FlyLineReleasesMenuAction extends AbstractAction implements Presenter.Menu {
    
    public static final String RELEASES_ITEM = "Fly Line Releases";

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(RELEASES_ITEM);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                SessionMgr.getBrowser().getFlyLineReleaseListDialog().showDialog();
            }
        });
        return menuItem;
    }
}
