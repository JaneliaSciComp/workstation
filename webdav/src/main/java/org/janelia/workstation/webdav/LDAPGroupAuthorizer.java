package org.janelia.workstation.webdav;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
/**
 * Created by schauderd on 6/26/15.
 */
public class LDAPGroupAuthorizer implements Authorizer {
    private Dn groupDN;

    public LDAPGroupAuthorizer() {
    }

    public Dn getGroupDN() {
        return groupDN;
    }

    public void setGroupDN(Dn groupDN)  {
        this.groupDN = groupDN;
    }

    public boolean checkAccess(Token credentials) {
        // logic for basic auth
        String username;
        if (credentials instanceof BasicAuthToken) {
            username = ((BasicAuthToken) credentials).getUsername();
        } else {
            return false;
        }

        // open connection to LDAP Provider
        LDAPProvider provider = (LDAPProvider) WebdavContextManager.getProviders().get("ldap");
        if (provider==null) {
            throw new RuntimeException ("LDAP Provider resource doesn't exist");
        }
        try {
            provider.openConnection();
            boolean member = provider.hasGroupMembership(groupDN, username);
            provider.closeConnection();
            return member;
        } catch (LdapException le) {
            le.printStackTrace();
            throw new RuntimeException ("Problems connecting to LDAP Resource");
        } catch (CursorException e) {
            e.printStackTrace();
            throw new RuntimeException ("Problems checking LDAP authorization to resource " + groupDN + " for user " + username);
        }
    }
}
