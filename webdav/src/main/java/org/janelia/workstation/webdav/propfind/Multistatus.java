package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;

/**
 * Created by schauderd on 7/1/15.
 */
@JacksonXmlRootElement(localName = "D:multistatus")
public class Multistatus {
    @JacksonXmlProperty(localName = "xmlns:D" , isAttribute = true)
    final String davNamespace = "DAV:";

    @JacksonXmlProperty(localName = "xmlns:lp1" , isAttribute = true)
    final String apacheNamespace = "http://apache.org/dav/props/";

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "D:response")
    ArrayList<PropfindResponse> response = new ArrayList<>();

    public ArrayList<PropfindResponse> getResponse() {
        return response;
    }

    public void setResponse(ArrayList<PropfindResponse> response) {
        this.response = response;
    }
}
