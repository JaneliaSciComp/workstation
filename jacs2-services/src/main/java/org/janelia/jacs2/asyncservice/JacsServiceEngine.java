package org.janelia.jacs2.asyncservice;

public interface JacsServiceEngine {
    void setProcessingSlotsCount(int nProcessingSlots);
    ServerStats getServerStats();
}
