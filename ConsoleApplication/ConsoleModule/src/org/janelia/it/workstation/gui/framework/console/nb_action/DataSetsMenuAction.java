/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "Services",
        id = "DataSetsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_DataSetsMenuAction"
)
@ActionReference(path = "Menu/Services", position = 100)
@Messages("CTL_DataSetsMenuAction=Data Sets")
public final class DataSetsMenuAction extends AbstractAction implements Presenter.Menu {
    public static final String DATA_SETS_ITEM = "Data Sets";

    public DataSetsMenuAction() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        Browser browser = SessionMgr.getBrowser();
        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        if (dataSetListDialog.isAccessible()) {
            JMenuItem menuItem = new JMenuItem(DATA_SETS_ITEM);
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent ae ) {
                    new ServicesActionDelegate().presentDataSetListDialog();
                }
            });
            return menuItem;
        }
        return null;
    }
}
