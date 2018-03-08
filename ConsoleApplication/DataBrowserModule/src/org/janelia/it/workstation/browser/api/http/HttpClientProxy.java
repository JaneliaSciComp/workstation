package org.janelia.it.workstation.browser.api.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.model.security.Subject;

public class HttpClientProxy {

    private final HttpClient delegate;
    private Subject currentSubject;

    public HttpClientProxy(HttpClient delegate) {
        this.delegate = delegate;
    }

    public int executeMethod(HttpMethod method) throws IOException {
        addHeader(method, "JacsSubject", null, getSubjectKey());
        addHeader(method, "username", null, getSubjectKey());
        addHeader(method, "Authorization", "Bearer ", getAuthToken());
        return delegate.executeMethod(method);
    }

    private void addHeader(HttpMethod httpMethod, String headerAttribute, String headerValuePrefix, String headerValue) {
        if (headerValue != null) {
            httpMethod.addRequestHeader(headerAttribute, headerValuePrefix != null ? headerValuePrefix + headerValue : headerValue);
        }
    }

    public void setCurrentSubject(Subject subject) {
        this.currentSubject = subject;
    }

    public String getSubjectKey() {
        return currentSubject == null ? null : currentSubject.getKey();
    }

    public String getSubjectName() {
        return currentSubject == null ? null : currentSubject.getName();
    }

    private String getAuthToken() {
        return AccessManager.getAccessManager().getToken();
    }
}
