package org.janelia.workstation.webdav.propfind;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;

/**
 * Created by schauderd on 7/1/15.
 */
@JacksonXmlRootElement(namespace = "D", localName = "multistatus")
public class Multistatus {
    @JacksonXmlProperty(namespace = "D",localName="response")
    ArrayList<PropfindResponse> response;

    public ArrayList<PropfindResponse> getResponse() {
        return response;
    }

    public void setResponse(ArrayList<PropfindResponse> response) {
        this.response = response;
    }
}
