package org.janelia.workstation.webdav;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.api.ldap.model.name.Dn;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

/**
 * Created by schauderd on 6/26/15.
 */
public class LDAPProvider extends Provider {
    private String url;
    private String groupAttribute;
    private HashMap<String,Set<String>> cachedMembership = new HashMap<>();

    DefaultPoolableLdapConnectionFactory factory;
    LdapConnectionPool pool;


    public LDAPProvider() {
        super();
    }

    @Override
    public void init() {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( url );
        config.setLdapPort(389);
        factory = new DefaultPoolableLdapConnectionFactory( config );
        pool = new LdapConnectionPool( factory );
    }

    public void openConnection() throws LdapException {

    }

    public boolean hasGroupMembership(Dn value, String username) throws IOException,LdapException, CursorException {
        LdapConnection connection = pool.getConnection();
        try {
            connection.bind();
            SearchRequest req = new SearchRequestImpl();
            req.setScope(SearchScope.SUBTREE);
            req.setTimeLimit(0);
            req.setBase(value);
            req.setFilter("(" + groupAttribute + "=" + username + ")");
            SearchCursor searchCursor = connection.search(req);

            if (searchCursor.next()) {
                return true;
            } else return false;
        } finally {
            connection.unBind();
            pool.releaseConnection(connection);
        }
    }

    public void closeConnection() throws LdapException, IOException {

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
