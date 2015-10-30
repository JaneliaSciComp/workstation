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

@ActionID(
        category = "Services",
        id = "DataSetsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_DataSetsMenuAction",
        lazy = false
)
@ActionReference(path = "Menu/Services", position = 100)
@Messages("CTL_DataSetsMenuAction=Data Sets")
public final class DataSetsMenuAction extends AbstractAction implements Presenter.Menu {
    
    public static final String DATA_SETS_ITEM = "Data Sets";
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(DATA_SETS_ITEM);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                SessionMgr.getBrowser().getDataSetListDialog().showDialog();
            }
        });
        return menuItem;
    }
}
