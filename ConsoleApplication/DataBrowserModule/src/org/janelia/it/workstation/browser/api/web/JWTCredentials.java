package org.janelia.it.workstation.browser.api.web;

import org.apache.commons.httpclient.Credentials;

public class JWTCredentials implements Credentials {
    private final String jwtToken;

    public JWTCredentials(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }
}
