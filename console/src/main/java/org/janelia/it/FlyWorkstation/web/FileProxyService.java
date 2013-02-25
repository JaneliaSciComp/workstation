package org.janelia.it.FlyWorkstation.web;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.lf5.util.StreamUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.filecache.LocalFileCache;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A local service for proxying file requests through the WebDAV file cache.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileProxyService extends AbstractHandler {

    private static final Logger log = LoggerFactory.getLogger(FileProxyService.class);
    
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        Pattern pattern = Pattern.compile("/(\\w+)(/.+)");
        Matcher matcher = pattern.matcher(request.getPathInfo());
        matcher.find();
        
        String proxyType = matcher.group(1);
        String standardPath = matcher.group(2);
        
        if (proxyType.equals("webdav")) {
            log.info("Proxying file: "+standardPath);

            baseRequest.setHandled(true);
            
            if (standardPath==null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            InputStream input = null;
            OutputStream output = null;
            
            try {
                URL effectiveUrl = SessionMgr.getURL(standardPath);
                response.setContentType("application/octet-stream");
                response.setStatus(HttpServletResponse.SC_OK);
                input = effectiveUrl.openStream();
                output = response.getOutputStream();
                StreamUtils.copy(input, output);
            } 
            catch (FileNotFoundException e) {
                log.error("File not found: "+standardPath);
                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print("File not found\n");
            }
            catch (Exception e) {
                log.error("Error proxying file: "+standardPath,e);
                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print("Error proxying file\n");
                e.printStackTrace(response.getWriter());
            } 
            finally {
                if (input != null) {
                    try {
                        input.close();
                    } 
                    catch (IOException e) {
                        log.warn("Failed to close input stream", e);
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } 
                    catch (IOException e) {
                        log.warn("Failed to close output stream", e);
                    }
                }
            }
        }
        else {
            baseRequest.setHandled(true);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().print("Invalid proxy type: '"+proxyType+"'\nValid proxy types include: ['webdav']\n");
        }
    }
    
    public static URL getProxiedFileUrl(String standardPath) throws MalformedURLException {
        return new URL("http://localhost:40001/webdav"+standardPath);
    }
}
      