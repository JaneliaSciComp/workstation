package org.janelia.workstation.core.api.http;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.security.Subject;

import com.google.common.collect.ImmutableMap;

/**
 * Utilities for dealing with HTTP services, especially those behind the API Gateway.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class HttpServiceUtils {

    public static final String USERNAME_HEADER = "Username";
    public static final String RUNASUSER_HEADER = "RunAsUser";
    public static final String APPICATION_HEADER = "Application-Id";
    public static final String APPICATION_VALUE = "Workstation";
    
    public static Map<String,String> getExtraHeaders(boolean auth) {
        
        AccessManager accessManager = AccessManager.getAccessManager();
        Map<String,String> headers = new HashMap<>();

        // Identify ourselves
        headers.put(APPICATION_HEADER, APPICATION_VALUE);

        if (auth) {
            // Add the JWT for services behind the API Gateway, and file services like Jade
            String accessToken = accessManager.getToken();
            if (accessToken!=null) {
                headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
            
            // Username/RunAsUser headers for using services directly without the API Gateway
            Subject authSubject = accessManager.getAuthenticatedSubject();
            if (authSubject != null) {
    
                String authKey = authSubject.getKey();
                headers.put(USERNAME_HEADER, authKey);
                
                Subject actualSubject = accessManager.getActualSubject();
                if (actualSubject != null) {
                    String runAsKey = actualSubject.getKey();
                    if (!runAsKey.equals(authKey)) {
                        headers.put(RUNASUSER_HEADER, runAsKey);
                    }
                }
            }
        }
        
        return ImmutableMap.copyOf(headers);
    }
    
}
