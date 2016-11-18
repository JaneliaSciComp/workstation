package org.janelia.jacs2.service;

public class ServerStats {
    private int runningTasks;
    private int waitingTasks;
    private int availableSlots;

    public int getRunningTasks() {
        return runningTasks;
    }

    public void setRunningTasks(int runningTasks) {
        this.runningTasks = runningTasks;
    }

    public int getWaitingTasks() {
        return waitingTasks;
    }

    public void setWaitingTasks(int waitingTasks) {
        this.waitingTasks = waitingTasks;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }
}
