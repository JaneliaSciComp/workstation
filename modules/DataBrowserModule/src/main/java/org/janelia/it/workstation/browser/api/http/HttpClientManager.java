package org.janelia.it.workstation.browser.api.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

public class HttpClientManager {

    // Singleton
    private static HttpClientManager instance;
    public static HttpClientManager getInstance() {
        if (instance==null) {
            instance = new HttpClientManager();
        }
        return instance;
    }

    private final HttpClient httpClient;

    private HttpClientManager() {
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        httpClient = new HttpClient(multiThreadedHttpConnectionManager);
    }

    public static HttpClient getHttpClient() {
        return getInstance().httpClient;
    }    
}
