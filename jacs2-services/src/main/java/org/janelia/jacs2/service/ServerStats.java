package org.janelia.jacs2.service;

public class ServerStats {
    private int runningServices;
    private int waitingServices;
    private int availableSlots;

    public int getRunningServices() {
        return runningServices;
    }

    public void setRunningServices(int runningServices) {
        this.runningServices = runningServices;
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
}
