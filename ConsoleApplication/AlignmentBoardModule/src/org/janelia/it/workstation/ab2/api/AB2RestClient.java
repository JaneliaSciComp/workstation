package org.janelia.it.workstation.ab2.api;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Date;
import java.util.List;

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
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.janelia.it.jacs.shared.ffmpeg.H5JLoader;
import org.janelia.it.jacs.shared.ffmpeg.ImageStack;
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
            if (bodyPart.getMediaType().equals(MediaType.TEXT_PLAIN_TYPE)) {
                String typeString = bodyPart.getEntityAs(String.class);
                log.info("Received type="+typeString);
            } else if (bodyPart.getMediaType().equals(MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
                byte[] h5jData=bodyPart.getEntityAs(byte[].class);
                data = getDataFromH5JBytes(h5jData);
                log.info("Received " + data.length + " bytes");
                byte[] dimBytes = new byte[12];
                for (int i = 0; i < 12; i++) {
                    dimBytes[i] = data[i];
                }
                IntBuffer intBuf = ByteBuffer.wrap(dimBytes).asIntBuffer();
                int[] dimArray = new int[intBuf.remaining()];
                intBuf.get(dimArray);
                for (int j = 0; j < dimArray.length; j++) {
                    log.info("dim=" + j + " value=" + dimArray[j]);
                }
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

    byte[] getDataFromH5JBytes(byte[] h5jBytes) throws Exception {
        Long uniqueTime=new Date().getTime();
        File tempFile=new File("~/tmp/"+"file_"+uniqueTime+".h5j");
        FileUtils.writeByteArrayToFile(tempFile, h5jBytes);
        byte[] dataBytes=h5jGet3DBytesUsingList(tempFile);
        tempFile.delete();
        return dataBytes;
    }

    private static synchronized byte[] h5jGet3DBytesUsingList(File file) throws IOException {
        log.info("h5jInspector() start - file="+file.getAbsolutePath());
        try {
            H5JLoader h5JLoader = new H5JLoader(file.getAbsolutePath());
            List<ImageStack> images=h5JLoader.extractAllChannelsAsList();
            List<String> channelNames = h5JLoader.channelNames();
            log.info("Number of channels="+channelNames.size());
            int i=0;
            for (String channelName : channelNames) {
                log.info("channel "+i+" name="+channelName);
                i++;
            }
            ImageStack firstImage=images.get(0);
            log.info("Getting h5j image size");
            log.info("getting maxFrames");
            log.info("maxFrames="+firstImage.getNumFrames());
            log.info("getting width and height");
            log.info("width="+firstImage.width()+" height="+firstImage.height());
            log.info("getting bytesPerPixel");
            log.info("bytesPerPixel="+firstImage.getBytesPerPixel());
            byte[] channel0Bytes=getSingleChannelH5JDataFromImage(images.get(0));
            byte[] channel1Bytes=getSingleChannelH5JDataFromImage(images.get(1));
            byte[] channel2Bytes=getSingleChannelH5JDataFromImage(images.get(2));

            int xDim=firstImage.width();
            int yDim=firstImage.height();
            int zDim=firstImage.getNumFrames();

            byte[] image3DBytes=new byte[4 * xDim * yDim * zDim + 12];

            ByteBuffer bb = ByteBuffer.allocate(12);
            bb.putInt(xDim);
            bb.putInt(yDim);
            bb.putInt(zDim);

            byte[] header = bb.array();

            int p = 0;
            for (; p < 12; p++) {
                image3DBytes[p] = header[p];
            }

            int totalLength=xDim*yDim*zDim;

            for (i=0;i<totalLength;i++) {
                byte r=channel0Bytes[i];
                byte g=channel1Bytes[i];
                byte b=channel2Bytes[i];
                image3DBytes[p++]=r;
                image3DBytes[p++]=g;
                image3DBytes[p++]=b;
                image3DBytes[p++]=maxByteValueFromRgb(r, g, b);
            }
            return image3DBytes;
        } catch (Exception ex) {
            log.error("Exception ex="+ex.getMessage());
            throw new IOException(ex);
        }
    }


    private static byte maxByteValueFromRgb(byte r, byte g, byte b) {
        int rI=r;
        int gI=g;
        int bI=b;
        if (rI<0) rI+=256;
        if (gI<0) gI+=256;
        if (bI<0) bI+=256;
        int mI=rI;
        if (gI>mI) mI=gI;
        if (bI>mI) mI=bI;
        byte m=(byte)mI;
        return m;
    }

    private static synchronized byte[] getSingleChannelH5JDataFromImage(ImageStack image) throws IOException {
        try {
            int maxFrames = image.getNumFrames();
            int startingFrame = 0;
            int endingFrame = startingFrame + maxFrames;
            int sliceSize=image.width()*image.height();
            int dataSize=sliceSize*maxFrames;
            byte[] result=new byte[dataSize];
            for (int i = startingFrame; i < endingFrame; i++) {
                int startIndex=sliceSize*i;
                byte[] sliceData=image.interleave(i, 0, 1);
                System.arraycopy(sliceData, 0, result, startIndex, sliceSize);
            }
            return result;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

}
