package org.janelia.workstation.core.api.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;

public class HttpClientManager {

    private static final int HTTP_TIMEOUT_SEC = 5;

    // Singleton
    private static HttpClientManager instance;

    private static HttpClientManager getInstance() {
        if (instance==null) {
            instance = new HttpClientManager();
        }
        return instance;
    }

    private final HttpClient httpClient;

    private HttpClientManager() {

        HttpConnectionParams httpParams = new HttpConnectionParams();
        httpParams.setConnectionTimeout(HTTP_TIMEOUT_SEC*1000);
        httpParams.setSoTimeout(HTTP_TIMEOUT_SEC*1000);

        HttpClientParams clientParams = new HttpClientParams(httpParams);

        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        httpClient = new HttpClient(clientParams, multiThreadedHttpConnectionManager);
    }

    public static HttpClient getHttpClient() {
        return getInstance().httpClient;
    }    
}
