package org.janelia.jos.scality;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.client.HttpClient;
import org.janelia.utils.FakeInputStream;
import org.janelia.utils.MeasuringInputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the ScalityDAO which throws away all data it receives, and streams back 
 * a correct amount of garbage data when asked. THis class is intended for testing JOS without 
 * any complications from Scality. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FakeScalityDAO extends ScalityDAO {

    private static final Logger log = LoggerFactory.getLogger(FakeScalityDAO.class);
    
    public FakeScalityDAO(ScalityConfiguration configuration, HttpClient httpClient) {
        super(configuration, httpClient);
    }

    @Override
    public long put(InputStream input, String path, boolean compress) throws WebApplicationException {
        try {
            final String url = getUrlFromBPID(path);
            log.info("Fake putting {} (compress={})",url,compress);
            
            MeasuringInputStreamEntity mise = new MeasuringInputStreamEntity(input, compress);
            mise.writeTo(new NullOutputStream());
            return mise.getContentLength();
        }
        catch (IOException e) {
            log.error("Could not fake put "+path,e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InputStream get(String path, Long numBytes, boolean decompress) throws WebApplicationException {

        try {
            final String url = getUrlFromBPID(path);
            log.info("Fake getting {} (decompress={})",url,decompress);
            
            return new FakeInputStream(numBytes);
        }
        catch (Exception e) {
            log.error("Could not fake get: "+path,e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean delete(String path) throws WebApplicationException {
        final String url = getUrlFromBPID(path);
        log.info("Fake deleting {}",url);
        return true;
    }    
}
