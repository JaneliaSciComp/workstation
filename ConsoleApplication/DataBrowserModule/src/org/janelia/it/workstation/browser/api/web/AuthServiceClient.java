package org.janelia.it.workstation.browser.api.web;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.workstation.browser.api.facade.impl.rest.RESTClientImpl;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * RESTful client for retrieving JWS tokens from the authentication service.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AuthServiceClient extends RESTClientImpl {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    
    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("auth.rest.url");
    
    private WebTarget service;    

    public AuthServiceClient() {
        this(REMOTE_API_URL);
    }
    
    public AuthServiceClient(String serverUrl) {
        super(log);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl);
        log.info("Using server URL: {}",serverUrl);
    }
    
    public String obtainToken(String username, String password) {
        AuthBody body = new AuthBody();
        body.setUsername(username);
        body.setPassword(password);
        WebTarget target = service.path("authenticate");
        Response response = target
                .request("application/json")
                .post(Entity.json(body));
        
        if (response.getStatus()!=200) {
            log.error("Auth service returned status {} for user {}", response.getStatus(), username);
            return null;
        }
        
        JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
        if (data!=null) {
            JsonNode tokenNode = data.get("token");
            if (tokenNode!=null) {
                return tokenNode.asText();
            }
            else {
                log.error("Auth service returned status OK, but an empty token for user {}", username);
            }
        }
        else {
            log.error("Auth service returned status OK, but an empty response for user {}", username);
        }
        
        return null;
    }
    
    /**
     * Serializes into the JSON expected by the AuthenticationService as input.
     */
    private static class AuthBody {
        
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

}
