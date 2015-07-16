package org.janelia.workstation.webdav;


/**
 * Created by schauderd on 6/26/15.
 */
public class ScalityProvider extends Provider {
    private String url;
    private String driver;

    public ScalityProvider() {
        super();
    }

    public String getUrlFromBPID(String bpid) {
        StringBuilder sb = new StringBuilder(url);
        sb.append("/");
        sb.append(driver);
        sb.append("/");
        sb.append(bpid);
        return sb.toString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
}
