package org.janelia.workstation.core.api.http;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.security.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientProxy {

    private static final Logger log = LoggerFactory.getLogger(HttpClientProxy.class);
    
    private final HttpClient delegate;

    public HttpClientProxy(HttpClient delegate) {
        this.delegate = delegate;
    }

    public int executeMethod(HttpMethod method) throws IOException {

        log.trace("{} {}", method.getName(), method.getURI());
        
        for (Entry<String, String> entry : HttpServiceUtils.getExtraHeaders(true).entrySet()) {
            addHeader(method, entry.getKey(), entry.getValue());
        }

        Subject actualSubject = AccessManager.getAccessManager().getActualSubject();
        if (actualSubject != null) {
            String subjectKey = actualSubject.getKey();
            // Backwards compatibility with older Jade versions
            addHeader(method, "JacsSubject", subjectKey);
        }
        
        return delegate.executeMethod(method);
    }

    private void addHeader(HttpMethod httpMethod, String headerAttribute, String headerValue) {
        if (headerValue != null) {
            httpMethod.addRequestHeader(headerAttribute, headerValue);
        }
    }
}
