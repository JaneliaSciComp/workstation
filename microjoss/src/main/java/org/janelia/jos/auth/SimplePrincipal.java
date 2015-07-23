package org.janelia.jos.auth;

public class SimplePrincipal {

    private String username;

    public SimplePrincipal(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}