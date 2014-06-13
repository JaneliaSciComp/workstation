/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.janelia.it.workstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.workstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Services",
        id = "ServicesMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_ServicesMenuAction"
)
@ActionReference(path = "Menu/Services")
@Messages("CTL_ServicesMenuAction=Services")
public final class ServicesMenuAction extends AbstractAction implements Presenter.Menu {
    public static final String DATA_SETS_ITEM = "Data Sets";
    public static final String SCREEN_EVAL_ITEM = "Screen Evaluation";

    private JMenu subMenu = new JMenu("Optional Services");

    public ServicesMenuAction() {
        Browser browser = SessionMgr.getBrowser();
        final ScreenEvaluationDialog screenEvaluationDialog = browser.getScreenEvaluationDialog();
        if (screenEvaluationDialog.isAccessible()) {
            JMenuItem menuItem = new JMenuItem(SCREEN_EVAL_ITEM);
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent ae ) {
                    new ServicesActionDelegate().presentScreenEvalDialog();
                }
            });
            subMenu.add(menuItem);
        }
        
        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        if (dataSetListDialog.isAccessible()) {
            JMenuItem menuItem = new JMenuItem(DATA_SETS_ITEM);
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent ae ) {
                    new ServicesActionDelegate().presentDataSetListDialog();
                }
            });
            subMenu.add(menuItem);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return subMenu;
    }
}
