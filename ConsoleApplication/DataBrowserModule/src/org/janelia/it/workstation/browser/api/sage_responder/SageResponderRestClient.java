package org.janelia.it.workstation.browser.api.sage_responder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.workstation.browser.api.facade.impl.rest.RESTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * RESTful client for getting data from the SAGE Responder web service. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageResponderRestClient extends RESTClientImpl {

    private static final Logger log = LoggerFactory.getLogger(SageResponderRestClient.class);
    private RESTClientManager manager;
    
    public SageResponderRestClient() {
        super(log);
        this.manager = RESTClientManager.getInstance();
    }

    public Collection<String> getPublishingNames(String lineName) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        Response response = manager.getPublishingInfoLineEndpoint().path(lineName)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getPublishingNames from server")) {
            throw new WebApplicationException(response);
        }
        
        JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
        if (data!=null) {
            JsonNode publishingData = data.get("publishing_data");
            if (publishingData.isArray()) {
                for (final JsonNode objNode : publishingData) {
                    String publishingName = objNode.get("publishing_name").asText();
                    names.add(publishingName);
                }
            }
        }
        return names;
    }
}
