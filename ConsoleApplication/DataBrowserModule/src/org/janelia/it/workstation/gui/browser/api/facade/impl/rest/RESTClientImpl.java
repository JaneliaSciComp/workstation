package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTClientImpl {

    private static final Logger log = LoggerFactory.getLogger(RESTClientImpl.class);
    
    protected boolean checkBadResponse(int responseStatus, String failureError) {
        if (responseStatus<200 || responseStatus>=300) {
            log.error("ERROR RESPONSE: " + responseStatus);
            log.error(failureError);
            return true;
        }
        return false;
    }
}
