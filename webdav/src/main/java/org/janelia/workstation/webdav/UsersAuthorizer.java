package org.janelia.workstation.webdav;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by schauderd on 6/30/15.
 */

public class UsersAuthorizer implements Authorizer {
    private Map<String, String> userPasswords = new HashMap<>();

    public boolean checkAccess(Token credentials) {
        if (credentials instanceof BasicAuthToken) {
            String username = ((BasicAuthToken) credentials).getUsername();
            String password = ((BasicAuthToken) credentials).getPassword();
            if (userPasswords.containsKey(username)) {
                if (userPasswords.get(username).equals(password)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, String> getUserPasswords() {
        return userPasswords;
    }

    public void setUserPasswords(Map<String, String> userPasswords) {
        this.userPasswords = userPasswords;
    }
}
