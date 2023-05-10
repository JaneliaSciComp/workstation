package org.janelia.workstation.core.api.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.workstation.core.api.exceptions.RemoteServiceException;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.model.SplitHalf;
import org.janelia.workstation.core.model.SplitTypeInfo;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.model.domain.enums.SplitHalfType;
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
    private WebTarget service;    

    public SageRestClient() {
        this(ConsoleProperties.getInstance().getProperty("sageResponder.rest.url"));
    }

    public SageRestClient(String serverUrl) {
        this(serverUrl, true);
    }

    public SageRestClient(String serverUrl, boolean auth) {
        super(log);
        log.info("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, auth);
    }
    
    public Collection<String> getPublishingNames(String lineName) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        WebTarget target = service.path("/publishing_names");
        Response response = target.queryParam("line", lineName)
                .request("application/json")
                .get();
        try {
            if (response.getStatus()==404) {
                // SageResponder unfortunately abuses 404 to represent several okay-ish states, so we can't throw an exception in this case
                return names;
            }
            checkBadResponse(target, response);
            JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
            if (data!=null) {
                JsonNode publishingNames = data.get("publishing_names");
                if (publishingNames.isArray()) {
                    for (final JsonNode publishingNameNode : publishingNames) {
                        names.add(publishingNameNode.asText());
                    }
                }
            }
            return names;
        }
        finally {
            response.close();
        }
    }

    /**
     * The SAGE Responder expects frag_halves input like this:
     * Input: {"frags": ["BJD_109A01","BJD_109A03","BJD_109A04"], "usable":1}
     * 
     * This class serializes into that input.
     */
    private class FragHalvesInput {
        private List<String> frags;
        private int usable;
        public List<String> getFrags() {
            return frags;
        }
        public void setFrags(List<String> frags) {
            this.frags = frags;
        }
        public int getUsable() {
            return usable;
        }
        public void setUsable(int usable) {
            this.usable = usable;
        }
    }

    public Map<String, SplitTypeInfo> getSplitTypeInfo(Collection<String> frags) throws Exception {
        return getSplitTypeInfo(frags, true);
    }
    
    public Map<String,SplitTypeInfo> getSplitTypeInfo(Collection<String> frags, boolean usable) throws Exception {

        Map<String,SplitTypeInfo> splitHalfInfos = new HashMap<>();
        if (frags.isEmpty()) {
            return splitHalfInfos;
        }
        
        FragHalvesInput input = new FragHalvesInput();
        input.setFrags(new ArrayList<>(frags));
        input.setUsable(usable ? 1 : 0);
        
        try {
            WebTarget target = service.path("/frag_halves");
            Response response = target
                    .request("application/json")
                    .post(Entity.json(input));
            
            try {
                if (response.getStatus()==404) {
                    throw new RemoteServiceException("SAGE responder returned 404 for frag_halves");
                }
                checkBadResponse(target, response);
                JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
                if (data==null) {
                    throw new RemoteServiceException("SAGE responder returned empty result for frag_halves");
                }
                
                JsonNode splitHalves = data.get("split_halves");
                
                for(Iterator<String> i = splitHalves.fieldNames(); i.hasNext(); ) {
                    String frag = i.next();

                    List<SplitHalf> splitHalfList = new ArrayList<>();
                    
                    JsonNode jsonNode = splitHalves.get(frag);
                    if (jsonNode.isArray()) {
                        for (final JsonNode objNode : jsonNode) {
                            
                            String driver = objNode.get("driver").asText();
                            String flycoreId = objNode.get("flycore_id").asText();
                            String info = objNode.get("info").asText();
                            String line = objNode.get("line").asText();
                            String project = objNode.get("project").asText();
                            String robotId = objNode.get("robot_id").asText();
                            String subcategory = objNode.get("subcategory").asText();
                            String type = objNode.get("type").asText();
                            
                            SplitHalf half = new SplitHalf();
                            half.setDriver(driver);
                            half.setFlycoreId(flycoreId);
                            half.setInfo(info);
                            half.setLine(line);
                            half.setProject(project);
                            half.setRobotId(robotId);
                            half.setSubcategory(subcategory);
                            half.setType(SplitHalfType.valueOf(type));
                            
                            splitHalfList.add(half);
                        }
                    }
                    else {
                        throw new IllegalStateException("Unexpected split_halves node type: "+splitHalves.getNodeType().name());
                    }

                    splitHalfInfos.put(frag, new SplitTypeInfo(frag, splitHalfList));
                }
            }
            finally {
                response.close();
            }
        }
        catch (RemoteServiceException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Error getting split half info", e);
            throw new RemoteServiceException("Could not retrieve split information from SAGE Responder");
        }
        
        return splitHalfInfos;
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
