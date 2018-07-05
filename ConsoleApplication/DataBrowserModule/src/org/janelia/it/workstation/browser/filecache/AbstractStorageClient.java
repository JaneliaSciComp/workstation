package org.janelia.it.workstation.browser.filecache;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
abstract class AbstractStorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractStorageClient.class);

    final String baseUrl;
    final HttpClientProxy httpClient;
    final ObjectMapper objectMapper;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    AbstractStorageClient(String baseUrl, HttpClientProxy httpClient, ObjectMapper objectMapper) {
        this.baseUrl = validateUrl(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private String validateUrl(String urlString) {
        try {
            new URL(urlString); // attempt to create URL only for validation purposes
            return urlString;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("failed to parse URL: " + urlString, e);
        }
    }

}
