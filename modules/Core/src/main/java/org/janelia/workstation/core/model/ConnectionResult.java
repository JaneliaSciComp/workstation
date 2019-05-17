package org.janelia.workstation.core.model;

import java.util.Properties;

/**
 * When a new server connection is attempted, the result is either a set of remote properties, or an error.
 */
public class ConnectionResult {

    private Properties remoteProperties;
    private String errorText;

    public ConnectionResult(Properties remoteProperties) {
        this.remoteProperties = remoteProperties;
    }

    public ConnectionResult(String errorText) {
        this.errorText = errorText;
    }

    public Properties getRemoteProperties() {
        return remoteProperties;
    }

    public String getErrorText() {
        return errorText;
    }
}