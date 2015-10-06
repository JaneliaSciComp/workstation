package org.janelia.workstation.webdav;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.exception.FileUploadException;
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
 * For now, this delegates mostly to JOSS.  At some point, we should probably merge all this functionality into one web-app.
 */
public class ObjectFileShare extends FileShare {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 2;

    @Override
    public String propFind(UriInfo uriInfo, HttpHeaders headers) throws FileNotFoundException, IOException {
        // there is no hierarchical concept in Scality, so always only return existing file info
        String filepath = "/" + uriInfo.getPath();
        String scalityFilepath = filepath.substring(this.getMapping().length());

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();
        PropfindResponse fileMeta = new PropfindResponse();
        propfindContainer.getResponse().add(fileMeta);
        Propstat propstat = new Propstat();
        Prop prop = new Prop();

        JOSSProvider provider = (JOSSProvider) WebdavContextManager.getProviders().get("scality");
        String url = provider.getObjectUrl() + scalityFilepath;
        System.out.println (url);

        HeadMethod get = new HeadMethod(url);
        try {
            MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
            HttpConnectionManagerParams managerParams = mgr.getParams();
            managerParams.setDefaultMaxConnectionsPerHost(10);
            managerParams.setMaxTotalConnections(20);
            HttpClient httpClient = new HttpClient(mgr);
            Credentials credentials = new UsernamePasswordCredentials(provider.getUser(), provider.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            httpClient.executeMethod(get);
            prop.setGetContentLength(get.getResponseHeader("X-Joss-Size").getValue());
        } catch (Exception e) {
            throw new FileNotFoundException("Problem getting information about file from Scality");
        }
        finally {
            get.releaseConnection();
        }

        fileMeta.setHref("/Webdav" + this.getMapping() + scalityFilepath.toString());
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
        final String qualifiedFilename = filename.substring(this.getMapping().length());
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(100);
        managerParams.setMaxTotalConnections(100);
        HttpClient httpClient = new HttpClient(mgr);

        JOSSProvider provider = (JOSSProvider) WebdavContextManager.getProviders().get("scality");
        String url = provider.getObjectUrl() + qualifiedFilename;
        final GetMethod get = new GetMethod(url);
        try {
            Credentials credentials = new UsernamePasswordCredentials(provider.getUser(), provider.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            int responseCode = httpClient.executeMethod(get);
            System.out.println (responseCode);
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
                    System.out.println ("Content length is " + contentLength);
                    copyBytes(get.getResponseBodyAsStream(), output, contentLength);
                }
                finally {
                    get.releaseConnection();
                }
            }
        };
    }

    @Override
    public void putFile(InputStream binaryStream, String filepath) throws FileUploadException {
        try {
            final String qualifiedFilename = filepath.substring(this.getMapping().length());
            MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
            HttpConnectionManagerParams managerParams = mgr.getParams();
            managerParams.setDefaultMaxConnectionsPerHost(100);
            managerParams.setMaxTotalConnections(100);
            HttpClient httpClient = new HttpClient(mgr);

            JOSSProvider provider = (JOSSProvider) WebdavContextManager.getProviders().get("scality");
            String url = provider.getObjectUrl() + qualifiedFilename;

            final PutMethod put = new PutMethod(url);
            Credentials credentials = new UsernamePasswordCredentials(provider.getUser(), provider.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            put.setRequestEntity(new InputStreamRequestEntity(binaryStream));

            int responseCode = httpClient.executeMethod(put);
            System.out.println (responseCode);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileUploadException("Problem creating new file in joss");
        }
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
