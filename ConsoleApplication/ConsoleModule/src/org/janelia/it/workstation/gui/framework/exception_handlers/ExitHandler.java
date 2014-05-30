package org.janelia.it.workstation.gui.framework.exception_handlers;

import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.FatalCommError;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import java.util.MissingResourceException;

public class ExitHandler implements ExceptionHandler {

    public void handleException(Throwable throwable) {
        String msg = throwable.getMessage();
        if (throwable instanceof FatalCommError || throwable instanceof MissingResourceException || ((msg != null) && msg.indexOf("No Species is not a known species") > -1)) {
            SessionMgr.getSessionMgr().systemExit(1);
        }
    }
}