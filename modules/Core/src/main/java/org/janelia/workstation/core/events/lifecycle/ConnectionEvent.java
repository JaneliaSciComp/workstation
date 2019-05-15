package org.janelia.workstation.core.events.lifecycle;

/**
 * Application has connected to a new data server.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConnectionEvent {

    private String connectionString;

    public ConnectionEvent(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getConnectionString() {
        return connectionString;
    }
}
