package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/9/11
 * Time: 9:00 AM
 */
public class ExternalClient {
    private String name;
    private int clientPort;

    public ExternalClient(int clientPort, String name) {
        this.clientPort = clientPort;
        this.name = name;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
