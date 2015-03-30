/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Call these to implement service actions.
 * 
 * @author fosterl
 */
public class ServicesActionDelegate {

    /**
     * @todo make this one dynamic, like the Tools menu items. 
     */
    public void presentScreenEvalDialog() {
        SessionMgr.getBrowser().getScreenEvaluationDialog().showDialog();
    }

    public void presentDataSetListDialog() {
        SessionMgr.getBrowser().getDataSetListDialog().showDialog();
    }

}
