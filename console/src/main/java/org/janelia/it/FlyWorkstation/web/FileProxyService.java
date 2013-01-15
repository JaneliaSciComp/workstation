package org.janelia.it.FlyWorkstation.web;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.lf5.util.StreamUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * A local service for proxying file requests through the WebDAV file cache.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileProxyService extends AbstractHandler {
    
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        System.out.println(target);
        
        String path = request.getParameter("path");
        
        if (path==null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        baseRequest.setHandled(true);
        
        response.setContentType("application/octet-stream");
        response.setStatus(HttpServletResponse.SC_OK);
        
        FileInputStream fis = new FileInputStream(file);
        OutputStream out = response.getOutputStream();
        StreamUtils.copy(fis, out);
        out.close();
        fis.close();
    }
}
      