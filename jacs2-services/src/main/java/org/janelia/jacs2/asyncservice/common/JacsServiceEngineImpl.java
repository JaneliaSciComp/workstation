package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.ServerStats;

import javax.inject.Inject;

public class JacsServiceEngineImpl implements JacsServiceEngine {

    private final JacsServiceDispatcher jacsServiceDispatcher;

    @Inject
    JacsServiceEngineImpl(JacsServiceDispatcher jacsServiceDispatcher) {
        this.jacsServiceDispatcher = jacsServiceDispatcher;
    }

    @Override
    public ServerStats getServerStats() {
        return jacsServiceDispatcher.getServerStats();
    }

    @Override
    public void setProcessingSlotsCount(int nProcessingSlots) {
        jacsServiceDispatcher.setAvailableSlots(nProcessingSlots);
    }
}
