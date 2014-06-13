package org.janelia.it.workstation.api.facade.facade_mgr;

public class ConnectionStatus {

    private String statusMessage;
    private boolean notifyUser;

    public ConnectionStatus(String statusMessage, boolean notifyUser) {
        this.statusMessage = statusMessage;
        this.notifyUser = notifyUser;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean notifyUser() {
        return notifyUser;
    }
}