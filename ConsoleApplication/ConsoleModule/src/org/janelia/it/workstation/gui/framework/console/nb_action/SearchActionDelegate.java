package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.dialogs.MAASearchDialog;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Action backing for searches of all types.
 *
 * @author fosterl
 */
public class SearchActionDelegate {

    public void generalSearch() {
        getBrowser().getGeneralSearchDialog().showDialog();
    }


    public void maskSearch() {
        getBrowser().getMaskSearchDialog().showDialog();

    }

    public void maaSearch() {
        MAASearchDialog maaSearchDialog = getBrowser().getMAASearchDialog();
        if (maaSearchDialog != null && MAASearchDialog.isAccessible()) {
            maaSearchDialog.showDialog();
        }
    }

    private Browser getBrowser() {
        return SessionMgr.getBrowser();
    }
}
