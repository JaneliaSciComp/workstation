package org.janelia.jacs2.asyncservice;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import java.util.List;


public class ServerStats {
    private int runningServicesCount;
    private int availableSlots;
    private int waitingCapacity;
    private List<JacsServiceData> waitingServices;
    private List<JacsServiceData>  runningServices;

    public int getRunningServicesCount() {
        return runningServicesCount;
    }

    public void setRunningServicesCount(int runningServicesCount) {
        this.runningServicesCount = runningServicesCount;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public int getWaitingCapacity() {
        return waitingCapacity;
    }

    public void setWaitingCapacity(int waitingCapacity) {
        this.waitingCapacity = waitingCapacity;
    }

    public List<JacsServiceData>  getRunningServices() {
        return runningServices;
    }

    public void setRunningServices(List<JacsServiceData>  runningServices) {
        this.runningServices = runningServices;
    }

    public List<JacsServiceData> getWaitingServices() {
        return waitingServices;
    }

    public void setWaitingServices(List<JacsServiceData>  waitingServices) { this.waitingServices = waitingServices; }
}
