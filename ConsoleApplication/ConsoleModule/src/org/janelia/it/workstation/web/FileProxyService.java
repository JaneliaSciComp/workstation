package org.janelia.it.workstation.web;
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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.util.WorkstationFile;
import org.perf4j.StopWatch;
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

    	String method = request.getMethod();

        StopWatch stopwatch = new StopWatch("stream");
        
        Pattern pattern = Pattern.compile("/(\\w+)(/.+)");
        Matcher matcher = pattern.matcher(request.getPathInfo());
        
        if (!matcher.matches()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        String proxyType = matcher.group(1);
        String standardPath = matcher.group(2);

        log.debug("Client requested: {}",standardPath);
        
        if (proxyType.equals("webdav")) {

            baseRequest.setHandled(true);
            
            if (standardPath==null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            WorkstationFile wfile = null;
            OutputStream output = null;
            
            try {
                wfile = new WorkstationFile(standardPath);
                
                // Read from WebDav
                wfile.get("HEAD".equals(method));
                log.info("Proxying {} for: {}",method,wfile.getEffectiveURL());
                if (wfile.getStatusCode()!=null) {
                    response.setStatus(wfile.getStatusCode());
                }
                else {
                    response.setStatus(500);
                }
                
                if (wfile.getContentType()!=null) {
                    response.setContentType(wfile.getContentType());
                }
                if (wfile.getLength()!=null) {
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
            
            stopwatch.stop();
            
//            if (log.isDebugEnabled() && length!=null) {
//	            double timeMs = (double)stopwatch.getElapsedTime();
//	            double timeSec = (timeMs/1000);
//	            long bytesPerSec = Math.round((double)length / timeSec);
//	            log.debug("buffer="+BUFFER_SIZE+" length="+length+" timeMs="+timeMs+" timeSec="+timeSec+" bytesPerSec="+bytesPerSec);
//            }
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
      