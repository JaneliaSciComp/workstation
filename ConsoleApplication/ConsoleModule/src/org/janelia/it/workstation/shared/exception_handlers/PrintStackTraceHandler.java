package org.janelia.it.workstation.shared.exception_handlers;

import org.janelia.it.workstation.api.facade.concrete_facade.xml.XMLSecurityException;
import org.janelia.it.workstation.api.facade.facade_mgr.ConnectionStatusException;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;

public class PrintStackTraceHandler implements ExceptionHandler {
    public void handleException(Throwable throwable) {
        if (!(throwable instanceof ConnectionStatusException || throwable instanceof XMLSecurityException)) {
            throwable.printStackTrace();
        }

        if (throwable instanceof XMLSecurityException) {
            System.out.println(throwable.getMessage());
        }
    }
}