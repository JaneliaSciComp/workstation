package org.janelia.jacs2.service;

import java.util.List;

public class ServerStats {
    private int runningServicesCount;
    private int waitingServices;
    private int availableSlots;
    private List<Long> runningServices;

    public int getRunningServicesCount() {
        return runningServicesCount;
    }

    public void setRunningServicesCount(int runningServicesCount) {
        this.runningServicesCount = runningServicesCount;
    }

    public int getWaitingServices() {
        return waitingServices;
    }

    public void setWaitingServices(int waitingServices) {
        this.waitingServices = waitingServices;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public List<Long> getRunningServices() {
        return runningServices;
    }

    public void setRunningServices(List<Long> runningServices) {
        this.runningServices = runningServices;
    }
}
