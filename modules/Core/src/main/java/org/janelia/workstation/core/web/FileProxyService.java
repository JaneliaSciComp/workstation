package org.janelia.workstation.core.web;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.core.util.WorkstationFile;
import org.janelia.workstation.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A local service for proxying file requests through the WebDAV file cache.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileProxyService extends AbstractHandler {

    private static final Logger log = LoggerFactory.getLogger(FileProxyService.class);
    
    private static final int BUFFER_SIZE = 1024;
    
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        Pattern pattern = Pattern.compile("/(\\w+)(/.+)");
        Matcher matcher = pattern.matcher(request.getPathInfo());
        
        if (!matcher.matches()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        String proxyType = matcher.group(1);
        String standardPath = matcher.group(2);

        log.debug("Client requested: {}",standardPath);
        baseRequest.setHandled(true);
        
        if (proxyType.equals("webdav")) {
            stream(request.getMethod(), response, standardPath);
        }
        else {
            log.warn("Client requested bad proxy type: "+proxyType);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().print("Invalid proxy type: '"+proxyType+"'\nValid proxy types include: ['webdav']\n");
        }
    }
	  
    private void stream(String method, HttpServletResponse response, String standardPath) throws IOException {

        if (standardPath==null) {
            log.warn("Client requested null path");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        WorkstationFile wfile = null;
        OutputStream output = null;
        
        try {
            wfile = new WorkstationFile(standardPath);
            
            // Read from WebDav
            wfile.get("HEAD".equals(method), true);
            if (wfile.getStatusCode()!=null) {
                response.setStatus(wfile.getStatusCode());
            }
            else {
                response.setStatus(500);
            }
            
            log.info("Proxying {} ({}) for: {}",method,response.getStatus(), wfile.getEffectiveURL());
            
            if (wfile.getContentType() != null) {
                response.setContentType(wfile.getContentType());
            }
            if (wfile.getLength() != null) {
                response.addHeader("Content-length", wfile.getLength().toString());
            }

            if ("HEAD".equals(method)) {
                // This method is supported, but there is nothing more to do
            }
            else if ("GET".equals(method)) {
                log.debug("Writing {} bytes", wfile.getLength());
                InputStream input = wfile.getStream();
                output = response.getOutputStream();
                Utils.copyNio(input, output, BUFFER_SIZE);
            }
            else {
                throw new IllegalStateException("Unsupported method for Workstation file proxy service: "+method);
            }
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
            FrameworkImplProvider.handleExceptionQuietly(e);
        } 
        finally {
            if (wfile != null) {
                wfile.close();
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
}
      