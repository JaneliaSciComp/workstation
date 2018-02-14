package org.janelia.it.workstation.browser.api.http;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.janelia.it.workstation.browser.api.AccessManager;

import com.google.common.collect.ImmutableMap;

/**
 * Utilities for dealing with HTTP services, especially those behind the API Gateway.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class HttpServiceUtils {

    private static final String APPICATION_ID_HEADER = "Application-Id";
    private static final String APPICATION_ID_VALUE = "Workstation";
    
    public static Map<String,String> getExtraHeaders() {

        Map<String,String> headers = new HashMap<>();
        
        String accessToken = AccessManager.getAccessManager().getToken();
        if (accessToken!=null) {
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        
        headers.put(APPICATION_ID_HEADER, APPICATION_ID_VALUE);
        
        return ImmutableMap.copyOf(headers);
    }
    
}
