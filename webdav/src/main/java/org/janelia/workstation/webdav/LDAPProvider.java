package org.janelia.workstation.webdav;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.api.ldap.model.name.Dn;


/**
 * Created by schauderd on 6/26/15.
 */
public class LDAPProvider extends Provider {
    private String url;
    private String groupAttribute;
    private LdapConnection connection;

    public LDAPProvider() {
        super();
    }

    @Override
    public void init() {
        connection = new LdapNetworkConnection( url, 389 );
    }

    public void openConnection() throws LdapException {
        connection.bind();
    }

    public boolean hasGroupMembership(Dn value, String username) throws LdapException, CursorException {
        SearchRequest req = new SearchRequestImpl();
        req.setScope(SearchScope.SUBTREE);
        req.setTimeLimit(0);
        req.setBase(value);
        req.setFilter( "("+groupAttribute+"=" + username + ")" );
        SearchCursor searchCursor = connection.search( req );

        if (searchCursor.next()) {
            return true;
        } else return false;
    }

    public void closeConnection() throws LdapException {
        connection.unBind();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGroupAttribute() {
        return groupAttribute;
    }

    public void setGroupAttribute(String groupAttribute) {
        this.groupAttribute = groupAttribute;
    }

}
