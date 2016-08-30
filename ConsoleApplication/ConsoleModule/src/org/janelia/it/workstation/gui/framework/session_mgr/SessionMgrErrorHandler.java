/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

import org.janelia.it.jacs.integration.framework.session_mgr.ErrorHandler;
import org.openide.util.lookup.ServiceProvider;

/**
 * This error-handler impl defers to session manager.
 *
 * @author fosterl
 */
@ServiceProvider(service = ErrorHandler.class, path=ErrorHandler.LOOKUP_PATH)
public class SessionMgrErrorHandler implements ErrorHandler {

    @Override
    public void handleException(Throwable ex) {
        SessionMgr.getSessionMgr().handleException(ex);
    }
    
}
