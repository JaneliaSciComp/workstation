package org.janelia.workstation.webdav;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.propfind.Multistatus;
import org.janelia.workstation.webdav.propfind.Prop;
import org.janelia.workstation.webdav.propfind.PropfindResponse;
import org.janelia.workstation.webdav.propfind.Propstat;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

/**
 * Created by schauderd on 6/26/15.
 * For now, this works only with Scality.  At some point, we can abstract it to work with any object storage
 */
public class ObjectFileShare extends FileShare {
    private static final String SCALITY_PREPEND = "/Scality";
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 2;

    private String getUrlFromBPID(String bpid) {
        ScalityProvider provider = (ScalityProvider) WebdavContextManager.getProviders().get("scality");
        return provider.getUrlFromBPID(bpid);
    }

    @Override
    public String propFind(UriInfo uriInfo, HttpHeaders headers) throws FileNotFoundException, IOException {
        // there is no hierarchical concept in Scality, so always only return existing file info
        String filepath = "/" + uriInfo.getPath();
        String scalityFilepath = filepath.substring(SCALITY_PREPEND.length());

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();
        PropfindResponse fileMeta = new PropfindResponse();
        propfindContainer.getResponse().add(fileMeta);
        Propstat propstat = new Propstat();
        Prop prop = new Prop();
        String url = getUrlFromBPID(scalityFilepath);
        GetMethod get = new GetMethod(url);
        try {
            MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
            HttpConnectionManagerParams managerParams = mgr.getParams();
            managerParams.setDefaultMaxConnectionsPerHost(10);
            managerParams.setMaxTotalConnections(20);
            HttpClient httpClient = new HttpClient(mgr);
            httpClient.executeMethod(get);
            prop.setGetContentLength(get.getResponseHeader("Content-length").getValue());
        } catch (Exception e) {
            throw new FileNotFoundException("Problem getting information about file from Scality");
        }
        finally {
            get.releaseConnection();
        }

        // TO DO: point to mongo and get meta-data
        //prop.setCreationDate(Files.getAttribute(file, "creationTime").toString());
        //prop.setGetContentType(Files.probeContentType(file));
        //prop.setGetLastModified(Files.getLastModifiedTime(file).toString());
        fileMeta.setHref("/Webdav/Scality" + scalityFilepath.toString());
        propstat.setProp(prop);
        propstat.setStatus("HTTP/1.1 200 OK");
        fileMeta.setPropstat(propstat);

        ObjectMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = xmlMapper.writeValueAsString(propfindContainer);
            System.out.println (xml);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IOException("Problem parsing out proper meta content");
        }
        return xml;

    }

    @Override
    public StreamingOutput getFile (HttpServletResponse response, String filename) throws FileNotFoundException {
        // strip off the Scality prepend
        final String qualifiedFilename = filename.substring(SCALITY_PREPEND.length());
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(10);
        managerParams.setMaxTotalConnections(20);
        HttpClient httpClient = new HttpClient(mgr);

        String url = getUrlFromBPID(qualifiedFilename);
        final GetMethod get = new GetMethod(url);
        try {
            int responseCode = httpClient.executeMethod(get);
        } catch (IOException e) {
            throw new FileNotFoundException("problem getting data from scality");
        }
        final String contentLengthStr = get.getResponseHeader("Content-length").getValue();
        response.setHeader("Content-Length", contentLengthStr);

        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    copyBytes(get.getResponseBodyAsStream(), output, contentLength);
                }
                finally {
                    get.releaseConnection();
                }
            }
        };
    }

    protected static long copyBytes(InputStream input, OutputStream output, long length) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            if (count>length) {
                n = (int)(count - length);
                output.write(buffer, 0, n);
                count += n;
                return count;
            }
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
