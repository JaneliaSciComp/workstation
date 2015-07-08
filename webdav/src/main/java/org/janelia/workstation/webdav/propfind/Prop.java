package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Date;

/**
 * Created by schauderd on 7/1/15.
 */
public class Prop {
    @JacksonXmlProperty(localName = "D:resourcetype")
    String resourceType;

    @JacksonXmlProperty(localName = "D:creationdate")
    String creationDate;

    @JacksonXmlProperty(localName = "D:getlastmodified")
    String getLastModified;

    @JacksonXmlProperty(localName = "D:getetag")
    String getEtag;

    @JacksonXmlProperty(localName = "D:getcontenttype")
    String getContentType;

    @JacksonXmlProperty(localName = "D:getcontentlength")
    String getContentLength;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
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

    public String getGetContentLength() {
        return getContentLength;
    }

    public void setGetContentLength(String getContentLength) {
        this.getContentLength = getContentLength;
    }
}
