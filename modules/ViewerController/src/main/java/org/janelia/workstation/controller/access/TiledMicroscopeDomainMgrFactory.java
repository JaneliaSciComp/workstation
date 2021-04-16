package org.janelia.workstation.controller.access;

import org.janelia.workstation.controller.access.rest.RestTiledMicroscopeDomainMgr;

public class TiledMicroscopeDomainMgrFactory {
    public static TiledMicroscopeDomainMgr getDomainMgr() {
        return RestTiledMicroscopeDomainMgr.getDomainMgr();
    }
}
