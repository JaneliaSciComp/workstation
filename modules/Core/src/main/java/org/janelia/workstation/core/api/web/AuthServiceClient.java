package org.janelia.workstation.core.api.web;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.workstation.core.api.exceptions.AuthenticationException;
import org.janelia.workstation.core.api.exceptions.ServiceException;
import org.janelia.workstation.core.api.http.HttpServiceUtils;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * RESTful client for retrieving JWS tokens from the authentication service.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AuthServiceClient extends RESTClientBase {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private WebTarget service;    

    public AuthServiceClient(String serverUrl) {
        super(log);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, false);
        log.info("Using server URL: {}",serverUrl);
    }
    
    public String obtainToken(String username, String password) throws AuthenticationException, ServiceException {
        AuthBody body = new AuthBody();
        body.setUsername(username);
        body.setPassword(password == null ? "" : password);
        WebTarget target = service.path("authenticate");
        Response response = target
                .request("application/json")
                .header(HttpServiceUtils.USERNAME_HEADER, username)
                .post(Entity.json(body));
        
        if (response.getStatus()==401) {
            throw new AuthenticationException("Invalid username or password");
        }
        
        if (response.getStatus()!=200) {
            throw new ServiceException("Auth service returned status "+response.getStatus());
        }
        
        JsonNode data = response.readEntity(new GenericType<JsonNode>() {});
        if (data!=null) {
            JsonNode tokenNode = data.get("token");
            if (tokenNode!=null) {
                return tokenNode.asText();
            }
            else {
                throw new ServiceException("Auth service returned status OK, but an empty token for user "+username);
            }
        }
        else {
            throw new ServiceException("Auth service returned status OK, but an empty response for user "+username);
        }
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
