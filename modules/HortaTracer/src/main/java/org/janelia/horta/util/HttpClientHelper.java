package org.janelia.horta.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.security.AppAuthorization;

/**
 * Copied from legacy-shared to eliminate dependency on JACSv1.
 * @deprecated
 * TODO: Code using this class should be refactored to use the newer HttpServiceUtils
 */
@Deprecated
public class HttpClientHelper {
    private static final String USERNAME_HEADER = "Username";
    private static final String RUNASUSER_HEADER = "RunAsUser";
    private static final String APPICATION_HEADER = "Application-Id";
    private static final String APPICATION_VALUE = "Workstation";
    private final HttpClient httpClient;

    public HttpClientHelper() {
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        this.httpClient = new HttpClient(multiThreadedHttpConnectionManager);
    }

    public int executeMethod(HttpMethod method, AppAuthorization appAuthorization) throws IOException {
        Iterator var3 = getAuthorizationHeaders(appAuthorization).entrySet().iterator();

        while (var3.hasNext()) {
            Entry<String, String> entry = (Entry) var3.next();
            method.addRequestHeader((String) entry.getKey(), (String) entry.getValue());
        }

        return this.httpClient.executeMethod(method);
    }

    private static Map<String, String> getAuthorizationHeaders(AppAuthorization appAuthorization) {
        Map<String, String> headers = new HashMap();
        headers.put("Application-Id", "Workstation");
        String accessToken = appAuthorization.getAuthenticationToken();
        if (!StringUtils.isBlank(accessToken)) {
            headers.put("Authorization", "Bearer " + accessToken);
        }

        if (appAuthorization.getAuthenticatedSubject() != null) {
            headers.put("Username", appAuthorization.getAuthenticatedSubject().getKey());
        }

        if (appAuthorization.getProxiedSubject() != null) {
            headers.put("RunAsUser", appAuthorization.getProxiedSubject().getKey());
        }

        return headers;
    }
}