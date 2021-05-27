package org.janelia.workstation.controller.access;

import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.access.local.LocalTiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.access.rest.RestTiledMicroscopeDomainMgr;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;

public class TiledMicroscopeDomainMgrFactory {
    private static TiledMicroscopeDomainMgr domainMgr;

    public static TiledMicroscopeDomainMgr initDomainMgr(boolean isLocal) {
        if (isLocal)
            domainMgr =  LocalTiledMicroscopeDomainMgr.getDomainMgr();
        else
            domainMgr = RestTiledMicroscopeDomainMgr.getDomainMgr();
        return domainMgr;
    }

    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (domainMgr==null) {
            String connectionType = FrameworkAccess.getLocalPreferenceValue(ConnectionMgr.class, ConnectionMgr.CONNECTION_STRING_PREF, null);
            if (connectionType!=null && connectionType.equals("local")) {
                initDomainMgr(true);
            } else {
                initDomainMgr(false);
            }
        }
        return domainMgr;
    }
}
