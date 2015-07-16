package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;

/**
 * Created by schauderd on 7/1/15.
 */
public class PropfindResponse {
    @JacksonXmlProperty(localName = "D:href")
    String href;

    @JacksonXmlProperty(localName = "D:propstat")
    Propstat propstat;

    public Propstat getPropstat() {
        return propstat;
    }

    public void setPropstat(Propstat propstat) {
        this.propstat = propstat;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}

