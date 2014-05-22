/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.workstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Call these to implement service actions.
 * 
 * @author fosterl
 */
public class ServicesActionDelegate {

    public void presentNeuronSeparationDialog() {
        final Browser browser = SessionMgr.getBrowser();
        browser.getRunNeuronSeparationDialog().showDialog();
    }

    /**
     * @todo make this one dynamic, like the Tools menu items. 
     */
    public void presentScreenEvalDialog() {
        final Browser browser = SessionMgr.getBrowser();
        final ScreenEvaluationDialog screenEvaluationDialog = browser.getScreenEvaluationDialog();
        // This is only 'accessible' to Arnim Jenett, who will not be using it.
        if (screenEvaluationDialog.isAccessible()) {
            screenEvaluationDialog.showDialog();
        }
    }

    public void presentDataSetListDialog() {
        final Browser browser = SessionMgr.getBrowser();
        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        dataSetListDialog.showDialog();
    }

}
