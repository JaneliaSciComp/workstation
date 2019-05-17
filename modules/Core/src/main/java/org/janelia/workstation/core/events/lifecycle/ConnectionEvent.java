package org.janelia.workstation.core.events.lifecycle;

import java.util.Properties;

/**
 * Application has connected to a new data server.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConnectionEvent {

    private String connectionString;
    private Properties remoteProperties;

    public ConnectionEvent(String connectionString, Properties remoteProperties) {
        this.connectionString = connectionString;
        this.remoteProperties = remoteProperties;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public Properties getRemoteProperties() {
        return remoteProperties;
    }
}
