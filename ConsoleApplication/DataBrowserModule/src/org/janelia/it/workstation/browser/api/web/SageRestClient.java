package org.janelia.it.workstation.browser.api.web;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.workstation.browser.api.http.RESTClientBase;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RESTful client for getting data from the SAGE Responder web service. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageRestClient extends RESTClientBase {

    private static final Logger log = LoggerFactory.getLogger(SageRestClient.class);
    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("sageResponder.rest.url");
    private WebTarget service;    

    public SageRestClient() {
        this(REMOTE_API_URL);
    }

    public SageRestClient(String serverUrl) {
        super(log);
        log.info("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }

    public Collection<String> getPublishingNames(String lineName) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        WebTarget target = service.path("/publishing");
        Response response = target.queryParam("line", lineName)
                .request("application/json")
                .get();
        try {
            checkBadResponse(target, response);
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
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getImageProperties(Integer sageImageId) throws Exception {
        WebTarget target = service.path("/images").path(sageImageId.toString());
        Response response = target
                .request("application/json")
                .get();
        try {
            checkBadResponse(target, response);
            JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
            if (data!=null) {
                JsonNode imageData = data.get("image_data");
                if (imageData.isArray()) {
                    for (final JsonNode objNode : imageData) {
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.convertValue(objNode, Map.class);
                    }
                }
            }
            return null;
        }
        finally {
            response.close();
        }
    }
}
