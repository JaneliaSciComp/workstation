package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Date;

/**
 * Created by schauderd on 7/1/15.
 */
public class Prop {
    @JacksonXmlProperty(namespace = "lp1",localName = "resourcetype")
    String resourceType;

    @JacksonXmlProperty(namespace = "lp1",localName = "creationdate")
    Date creationDate;

    @JacksonXmlProperty(namespace = "lp1",localName = "getlastmodified")
    String getLastModified;

    @JacksonXmlProperty(namespace = "lp1",localName = "getetag")
    String getEtag;

    @JacksonXmlProperty(namespace = "D",localName = "getcontenttype")
    String getContentType;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getGetLastModified() {
        return getLastModified;
    }

    public void setGetLastModified(String getLastModified) {
        this.getLastModified = getLastModified;
    }

    public String getGetEtag() {
        return getEtag;
    }

    public void setGetEtag(String getEtag) {
        this.getEtag = getEtag;
    }

    public String getGetContentType() {
        return getContentType;
    }

    public void setGetContentType(String getContentType) {
        this.getContentType = getContentType;
    }
}
