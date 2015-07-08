package org.janelia.workstation.webdav;

/**
 * Created by schauderd on 6/30/15.
 */
public class BasicAuthToken extends Token {
    String username;
    String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
