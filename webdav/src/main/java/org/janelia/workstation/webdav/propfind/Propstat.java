package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by schauderd on 7/1/15.
 */
public class Propstat {
    @JacksonXmlProperty(namespace = "D",localName = "prop")
    Prop prop;

    @JacksonXmlProperty(namespace = "D",localName = "status")
    String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Prop getProp() {
        return prop;
    }

    public void setProp(Prop prop) {
        this.prop = prop;
    }
}

