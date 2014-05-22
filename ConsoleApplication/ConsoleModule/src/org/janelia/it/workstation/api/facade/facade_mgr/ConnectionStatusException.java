package org.janelia.it.workstation.api.facade.facade_mgr;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 2:56 PM
 */
public class ConnectionStatusException extends Exception {

    private boolean notifyUser;

    ConnectionStatusException(String msg, boolean notifyUser) {
        super(msg);
        this.notifyUser = notifyUser;
    }

    public boolean notifyUser() {
        return notifyUser;
    }
}