package org.janelia.it.workstation.ab2.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2RestClient {

    private static final Logger log = LoggerFactory.getLogger(AB2RestClient.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");
    private static final String REMOTE_AB2_DATA_PREFIX = "ab2/data";

    private final Client client;

    public AB2RestClient() {
        log.info("Using server URL: {}", REMOTE_API_URL);
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                log.error("Failed to deserialize property which does not exist in model: {}.{}", beanOrClass.getClass().getName(), propertyName);
                return true;
            }
        });
        this.client = ClientBuilder.newClient();
        client.register(provider);
        client.register(MultiPartFeature.class);
    }

    public WebTarget getAB2Endpoint(String suffix) {
        return client.target(REMOTE_API_URL + REMOTE_AB2_DATA_PREFIX + suffix)
                .queryParam("subjectKey", AccessManager.getSubjectKey());
    }

    public byte[] getSampleDefault3DImageXYZRGBA8(Long sampleId) throws Exception {
        Response response = getAB2Endpoint("/sampleDefault3DImage")
                .queryParam("sampleId", sampleId)
                .request("multipart/mixed")
                .get();
        if (checkBadResponse(response, "getSampleDefault3DImageXYZRGBA8: "+sampleId)) {
            throw new WebApplicationException(response);
        }
        MultiPart multipart = response.readEntity(MultiPart.class);
        byte[] data=null;
        for (BodyPart bodyPart : multipart.getBodyParts()) {
            data=bodyPart.getEntityAs(byte[].class);
            log.info("Received "+data.length+" bytes");
            byte[] dimBytes=new byte[12];
            for (int i=0;i<12;i++) { dimBytes[i]=data[i]; }
            IntBuffer intBuf = ByteBuffer.wrap(dimBytes).asIntBuffer();
            int[] dimArray = new int[intBuf.remaining()];
            intBuf.get(dimArray);
            for (int j=0;j<dimArray.length;j++) {
                log.info("dim="+j+" value="+dimArray[j]);
            }
        }
        return data;
    }

    protected boolean checkBadResponse(Response response, String failureError) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Problem making request for {}", failureError);
            log.error("Server responded with error code: {} {}",response.getStatus(), status);
            return true;
        }
        return false;
    }

}
