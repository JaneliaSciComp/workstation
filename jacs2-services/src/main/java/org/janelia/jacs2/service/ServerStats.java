package org.janelia.jacs2.service;

import java.util.List;
import java.util.Set;

public class ServerStats {
    private int runningTasksCount;
    private int waitingTasks;
    private int availableSlots;
    private List<Long> runningTasks;

    public int getRunningTasksCount() {
        return runningTasksCount;
    }

    public void setRunningTasksCount(int runningTasksCount) {
        this.runningTasksCount = runningTasksCount;
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

    public List<Long> getRunningTasks() {
        return runningTasks;
    }

    public void setRunningTasks(List<Long> runningTasks) {
        this.runningTasks = runningTasks;
    }
}
