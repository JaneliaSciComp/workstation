/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.console.nb_action;

/**
 * Action backing for searches of all types.
 * @author fosterl
 */
public class SearchActionDelegate {
    public void generalSearch() {
        getBrowser().getGeneralSearchDialog().showDialog();

    }

    public void patternSearch() {
        getBrowser().getPatternSearchDialog().showDialog();

    }

    public void maskSearch() {
        getBrowser().getMaskSearchDialog().showDialog();

    }

    public void giantFiberSearch() {
        getBrowser().getGiantFiberSearchDialog().showDialog();

    }

    public void maaSearch() {
        org.janelia.it.workstation.gui.dialogs.MAASearchDialog maaSearchDialog = getBrowser().getMAASearchDialog();
        if (maaSearchDialog!=null && maaSearchDialog.isAccessible()) {
            maaSearchDialog.showDialog();
        }

    }

    private org.janelia.it.workstation.gui.framework.console.Browser getBrowser() {
        return org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser();
    }
}
