package org.janelia.jos.scality;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.janelia.utils.MeasuringInputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for CRUD operations against the Scality key store. 
 *  
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityDAO {

    private static final Logger log = LoggerFactory.getLogger(ScalityDAO.class);

    private String baseUrl;
    private HttpClient httpClient;
	
	public ScalityDAO(ScalityConfiguration configuration, HttpClient httpClient) {
        StringBuilder sb = new StringBuilder(configuration.getUrl());
        sb.append("/");
        sb.append(configuration.getDriver());
        sb.append("/");
        this.baseUrl = sb.toString();
        this.httpClient = httpClient;
	}

    protected String getUrlFromBPID(String bpid) {
        return baseUrl+bpid;
    }

    public Map<String,String> head(String path) throws WebApplicationException {

        try {
            final String url = getUrlFromBPID(path);
            log.debug("Getting headers for {}",url);
            
            HttpHead head = new HttpHead(url);
            
            HttpResponse res = httpClient.execute(head);
            EntityUtils.consumeQuietly(res.getEntity());
            
            int statusCode = res.getStatusLine().getStatusCode();
            log.info("Got headers from Scality for {}",path);

            switch (Response.Status.fromStatusCode(statusCode)) {
            case OK: 
                break;
            case NOT_FOUND: 
                return null;
            default: 
                log.error("Scality returned unexpected status code ({}) for {}",statusCode,url);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }

            Map<String,String> headerMap = new HashMap<>();
            for(Header header : res.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }
            
            return headerMap;
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Could not get headers from Scality: "+path,e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public long put(InputStream input, String path, boolean compress) throws WebApplicationException {

        try {
            final String url = getUrlFromBPID(path);
            log.debug("Putting {} (compress={})",url,compress);
            
            HttpPut put = new HttpPut(url);
            put.addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
                        
            MeasuringInputStreamEntity mise = new MeasuringInputStreamEntity(input, compress);
            put.setEntity(mise);
            HttpResponse res = httpClient.execute(put);
            EntityUtils.consumeQuietly(res.getEntity());
            
            int statusCode = res.getStatusLine().getStatusCode();
            log.info("Uploaded {} bytes to Scality for {}",mise.getContentLength(),path);

            switch (Response.Status.fromStatusCode(statusCode)) {
            case OK: 
                break;
            default: 
                log.error("Scality returned unexpected status code ({}) for {}",statusCode,url);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            
            return mise.getContentLength();
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Could not put file to Scality: "+path,e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public InputStream get(String path, Long numBytes, boolean decompress) throws WebApplicationException {

        try {
            final String url = getUrlFromBPID(path);
            log.debug("Getting {} (decompress={})",url,decompress);
            
            HttpGet get = new HttpGet(url);
            final HttpResponse res = httpClient.execute(get);
            int statusCode = res.getStatusLine().getStatusCode();
            log.info("Received status {} from Scality for {}",statusCode,path);
            
            switch (Response.Status.fromStatusCode(statusCode)) {
            case OK: 
                break;
            case NOT_FOUND: 
                throw new WebApplicationException(Status.NOT_FOUND);
            default: 
                log.error("Scality returned unexpected status code ({}) for {}",statusCode,url);
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            
            InputStream input = res.getEntity().getContent();
            if (decompress) {
                input = new BZip2CompressorInputStream(input); 
            }
            
            return input;
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Could not get file from Scality: "+path,e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public boolean delete(String path) throws WebApplicationException {
        
        try {
            final String url = getUrlFromBPID(path);
            log.debug("Deleting {}",url);
            
            HttpDelete get = new HttpDelete(url);
            final HttpResponse res = httpClient.execute(get);
            
            // Have to consume the entity, otherwise the connection is not returned back to the pool 
            EntityUtils.consumeQuietly(res.getEntity());
            
            int statusCode = res.getStatusLine().getStatusCode();
            log.info("Received status {} from Scality for deletion of {}",statusCode,path);
            
            switch (Response.Status.fromStatusCode(statusCode)) {
            case OK:
                return true;
            case NOT_FOUND: 
                // Scality always returns OK, even if the object doesn't exist, but maybe in the future that could change. 
                throw new WebApplicationException(Status.NOT_FOUND);
            default:
                log.error("Scality returned unexpected status code ({}) for {}",statusCode,url);
                return false;
            }
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            // Fail silently because we can always retry the deletion. 
            log.error("Could not delete file from Scality: "+path,e);
            return false;
        }
    }
}
