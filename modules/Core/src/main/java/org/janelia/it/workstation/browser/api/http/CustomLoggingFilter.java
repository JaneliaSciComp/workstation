package org.janelia.it.workstation.browser.api.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.glassfish.jersey.message.internal.ReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When very deep debugging is necessary, uncomment this in the RestJsonClientManager.
 */
public class CustomLoggingFilter implements ClientRequestFilter, ClientResponseFilter {
    
    private static final Logger log = LoggerFactory.getLogger(CustomLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(requestContext.getMethod());
        sb.append(" ").append(requestContext.getUri());
        sb.append(" (headers: ").append(requestContext.getHeaders()).append("");
        log.info(sb.toString());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Request method: ").append(requestContext.getMethod());
        sb.append("\nRequest URI: ").append(requestContext.getUri());
        sb.append("\nResponse headers: ").append(responseContext.getHeaders());
        sb.append("\nResponse body: ");
        sb.append("\n").append(getEntityBody(responseContext));
        writeToTemp(sb.toString());
    }
    
    private void writeToTemp(String str) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile("response", ".tmp");
            tmpFile.deleteOnExit();
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(str);
            writer.close();
            log.info("Wrote response log to "+tmpFile);
        }
        catch (IOException e) {
            log.error("Error writing response log file", e);
        }
    }
    
    private String getEntityBody(ClientResponseContext responseContext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = responseContext.getEntityStream();

        final StringBuilder b = new StringBuilder();
        try {
            ReaderWriter.writeTo(in, out);

            byte[] requestEntity = out.toByteArray();
            if (requestEntity.length == 0) {
                b.append("").append("\n");
            }
            else {
                b.append(new String(requestEntity)).append("\n");
            }
            // Reset the response so that it can be processed by Jackson
            responseContext.setEntityStream(new ByteArrayInputStream(requestEntity));

        }
        catch (IOException ex) {
            log.error("Error logging entity body");
        }
        return b.toString();
    }

}