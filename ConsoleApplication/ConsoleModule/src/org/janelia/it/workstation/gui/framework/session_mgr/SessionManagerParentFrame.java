/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

import javax.swing.JFrame;
import org.janelia.it.jacs.integration.framework.session_mgr.ParentFrame;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides the parent frame for j-option-pane, etc.
 *
 * @author fosterl
 */
@ServiceProvider(service = ParentFrame.class, path=ParentFrame.LOOKUP_PATH)
public class SessionManagerParentFrame implements ParentFrame {

    /** This implementation returns the main from from session mgr. */
    @Override
    public JFrame getMainFrame() {
        return SessionMgr.getSessionMgr().getMainFrame();
    }
    
}
