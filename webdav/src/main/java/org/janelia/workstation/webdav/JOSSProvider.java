package org.janelia.workstation.webdav;


/**
 * Created by schauderd on 6/26/15.
 */
public class JOSSProvider extends Provider {
    private String objectUrl;
    private String metaUrl;
    private String user;
    private String password;

    public JOSSProvider() {
        super();
    }

    public String getObjectUrl() {
        return objectUrl;
    }

    public void setObjectUrl(String objectUrl) {
        this.objectUrl = objectUrl;
    }

    public String getMetaUrl() {
        return metaUrl;
    }

    public void setMetaUrl(String metaUrl) {
        this.metaUrl = metaUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
