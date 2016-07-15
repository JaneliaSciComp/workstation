package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Search",
        id = "PatternMaskSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_PatternMaskSearchAction"
)
@ActionReference(path = "Menu/Search", position = 1200)
@Messages("CTL_PatternMaskSearchAction=Pattern Mask Search")
public final class PatternMaskSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        SessionMgr.getBrowser().getMaskSearchDialog().showDialog();
    }
}
