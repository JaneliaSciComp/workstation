package org.janelia.it.FlyWorkstation.shared.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction for remote files, which allows for remote file access via mount, WebDAV, or 
 * local file access in the case of cached files. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkstationFile {
    
    private static final Logger log = LoggerFactory.getLogger(WorkstationFile.class);
    
    private String standardPath;
    private URL effectiveURL;
    private Long length;
    private Integer statusCode;
    private String contentType;
    private InputStream stream;
    
    public WorkstationFile(String standardPath) {
        this.standardPath = standardPath;
    }
    
    public void get() throws Exception {
        get(false);
    }
    
    public void get(boolean headOnly) throws Exception {
        
        File standardFile = new File(standardPath);
        if (standardFile.canRead()) {
            // File is either local or mounted
            this.effectiveURL = standardFile.toURI().toURL();
            getFile(standardFile);
            return;
        }
        
        this.effectiveURL = SessionMgr.getURL(standardPath);
        if (effectiveURL.getProtocol().equals("file")) {
            getFile(new File(effectiveURL.getPath()));
        }
        else {
            HttpClient client = SessionMgr.getSessionMgr().getWebDavClient().getHttpClient();
            HttpMethodBase method = null;
            if (headOnly) {
                HeadMethod head = new HeadMethod(effectiveURL.toString());  
                client.executeMethod(head);
                method = head;
            }
            else {
                GetMethod get = new GetMethod(effectiveURL.toString());
                client.executeMethod(get);
                method = get;
                this.stream = get.getResponseBodyAsStream();
            }
            
            this.statusCode = method.getStatusCode();
            
            Header contentTypeHeader = method.getResponseHeader("Content-Type");
            if (contentTypeHeader!=null) {
                this.contentType = contentTypeHeader.getValue();
            }
            
            Header contentLengthHeader = method.getResponseHeader("Content-Length");
            if (contentLengthHeader!=null) {
                this.length = Long.parseLong(contentLengthHeader.getValue());
            }
            
            log.debug("Opened remote file: "+effectiveURL);
            log.debug("  Length: "+length);
            log.debug("  Content-type: "+contentType);
            log.debug("  Status code: "+statusCode);
        }
    }
    
    private void getFile(File file) throws Exception {
        this.length = file.length();
        this.contentType = "application/octet-stream";
        this.statusCode = 200;
        this.stream = effectiveURL.openStream();
        
        log.debug("Opened local file: "+file.getAbsolutePath());
        log.debug("  Length: "+length);
        log.debug("  Content-type: "+contentType);
        log.debug("  Status code: "+statusCode);
    }

    /**
     * The standard UNIX path to the file.
     * @return
     */
    public String getStandardPath() {
        return standardPath;
    }

    /**
     * The effective URL which represents the actual file access. Only available after get() is called.
     * @return
     */
    public URL getEffectiveURL() {
        return effectiveURL;
    }

    /**
     * The length of the file in bytes. Only available after get() is called.
     * @return
     */
    public Long getLength() {
        return length;
    }

    /**
     * The length of the file in bytes. Only available after get() is called.
     * @return
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * The length of the file in bytes. Only available after get() is called.
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The length of the file in bytes. Only available after get(false) is called.
     * @return
     */
    public InputStream getStream() {
        return stream;
    }

}
