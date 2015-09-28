package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.util.actions.Presenter;

//TODO: screen evaluation is no longer supported, it was only accessible by Arnim anyway. 
//@ActionID(
//        category = "Services",
//        id = "ScreenEvaluationMenuAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_ScreenEvaluationMenuAction",
//        lazy = false
//)
//@ActionReference(path = "Menu/Services", position = 110)
//@Messages("CTL_ScreenEvaluationMenuAction=Screen Evaluation")
public final class ScreenEvaluationMenuAction extends AbstractAction implements Presenter.Menu {
    public static final String SCREEN_EVAL_ITEM = "Screen Evaluation";

    public ScreenEvaluationMenuAction() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        if (isAccessible()) {
            JMenuItem menuItem = new JMenuItem(SCREEN_EVAL_ITEM);
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent ae ) {
                    SessionMgr.getBrowser().getScreenEvaluationDialog().showDialog();
                }
            });
            return menuItem;
        }
        return null;
    }

    public static boolean isAccessible() {
        return "user:jenetta".equals(SessionMgr.getSubjectKey());
    }
}
