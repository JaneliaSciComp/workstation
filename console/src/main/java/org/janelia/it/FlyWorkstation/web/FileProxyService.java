package org.janelia.it.FlyWorkstation.web;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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
    	log.info("Method: "+method);
    	
        StopWatch stopwatch = new StopWatch("stream");
        Long length = null;
        
        Pattern pattern = Pattern.compile("/(\\w+)(/.+)");
        Matcher matcher = pattern.matcher(request.getPathInfo());
        
        if (!matcher.matches()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
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
                // Read from WebDav
                URL effectiveUrl = SessionMgr.getURL(standardPath);
                log.info("Requesting URL: "+effectiveUrl);
                
                HttpClient client = SessionMgr.getSessionMgr().getWebDavClient().getHttpClient();
                
                if ("HEAD".equals(method)) {
                	HeadMethod head = new HeadMethod(effectiveUrl.toString());	
                    client.executeMethod(head);

                    Header contentType = head.getResponseHeader("Content-Type");
                    if (contentType==null) {
                        response.setContentType("application/octet-stream");    
                    }
                    else {
                        response.setContentType(contentType.getValue());
                    }

                    Header contentLength = head.getResponseHeader("Content-Length");
                    if (contentLength!=null) {
                    	response.addHeader("Content-length", contentLength.getValue());
                    }
                    
                    response.setStatus(head.getStatusCode());
                }
                else if ("GET".equals(method)) {

                    GetMethod get = new GetMethod(effectiveUrl.toString());
                    client.executeMethod(get);
                    
                    Header contentType = get.getResponseHeader("Content-Type");
                    if (contentType==null) {
                        response.setContentType("application/octet-stream");    
                    }
                    else {
                        response.setContentType(contentType.getValue());
                    }

                    Header contentLength = get.getResponseHeader("Content-Length");
                    if (contentLength!=null) {
                    	response.addHeader("Content-length", contentLength.getValue());
                    	length = Long.parseLong(contentLength.getValue());
                    }
                    
                    response.setStatus(get.getStatusCode());
               
                    log.info("Writing "+contentLength.getValue()+" bytes");
                    input = get.getResponseBodyAsStream();
                    output = response.getOutputStream();
                    copyNio(input, output, BUFFER_SIZE);
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
            
            stopwatch.stop();
            double timeMs = (double)stopwatch.getElapsedTime();
            double timeSec = (timeMs/1000);
            long bytesPerSec = Math.round((double)length / timeSec);
            log.info("buffer="+BUFFER_SIZE+" length="+length+" timeMs="+timeMs+" timeSec="+timeSec+" bytesPerSec="+bytesPerSec);
        }
        else {
            baseRequest.setHandled(true);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().print("Invalid proxy type: '"+proxyType+"'\nValid proxy types include: ['webdav']\n");
        }
    }
	  
	public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
		byte[] buf = new byte[bufferSize];
		int bytesRead = input.read(buf);
		while (bytesRead != -1) {
			output.write(buf, 0, bytesRead);
			bytesRead = input.read(buf);
		}
		output.flush();
	}

    public static void copyNio(InputStream input, OutputStream output, int bufferSize) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(input);
		final WritableByteChannel outputChannel = Channels.newChannel(output);
		fastChannelCopy(inputChannel, outputChannel, bufferSize);
		inputChannel.close();
		outputChannel.close();
	}

	/**
	 * Adapted from http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest, int bufferSize) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}
    
    public static URL getProxiedFileUrl(String standardPath) throws MalformedURLException {
        return new URL("http://localhost:40001/webdav"+standardPath);
    }
    
    // Test harness for debugging slowness issue which turned out to be log4j. This can be deleted later.
    public static final void main(String[] args) throws Exception {
    	String url = "http://jacs.int.janelia.org/WebDAV/archive/scicomp/jacsData/devstore/asoy/Separation/656/939/1859489873178656939/separate/archive/fastLoad/ConsolidatedSignal2.v3dpbd";

    	String userName = args[0];
    	String password = args[1];
    	
    	MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2); 
        managerParams.setMaxTotalConnections(20); 
        HttpClient client = new HttpClient(mgr);

        if ((userName != null) && (password != null)) {

            final UsernamePasswordCredentials credentials =
                    new UsernamePasswordCredentials(userName,password);
            final HttpState clientState = client.getState();
            clientState.setCredentials(AuthScope.ANY, credentials);
        }

        StopWatch stopwatch = new StopWatch("stream");
        
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);
        Header contentLength = get.getResponseHeader("Content-Length");
        InputStream input = get.getResponseBodyAsStream();
        
        System.out.println("GOT STATUS CODE "+get.getStatusCode());
        
        Long length = null;
        if (contentLength!=null) {
            try {
            	length = Long.parseLong(contentLength.getValue());
                // Does not support longs:
                //response.setContentLength(length);
            }
            catch (NumberFormatException e) {
                log.error("Could not parse content length: "+contentLength.getValue());
            }
        }
        
        File file = new File("/tmp/tempFile.v3dpbd");
        FileOutputStream fios = new FileOutputStream(file);
        
        System.out.println("Copying");
        
        copyNio(input, fios, BUFFER_SIZE);
        
        fios.close();
        System.out.println("Wrote "+length+" bytes to "+file);
        stopwatch.stop();
        double timeMs = (double)stopwatch.getElapsedTime();
        double timeSec = (timeMs/1000);
        long bytesPerSec = Math.round((double)length / timeSec);
        System.out.println("buffer="+BUFFER_SIZE+" length="+length+" timeMs="+timeMs+" timeSec="+timeSec+" bytesPerSec="+bytesPerSec);
    }

    // File copied with "time cp": real    0m43.189s
    
    // Streaming (and caching) file from archive using httpclient:
    
    // buffer=1024 length=5223881 timeMs=469.0 timeSec=0.469 bytesPerSec=11138339
    // buffer=1024 length=5223881 timeMs=459.0 timeSec=0.459 bytesPerSec=11381004
    // buffer=1024 length=5223881 timeMs=481.0 timeSec=0.481 bytesPerSec=10860459
    // buffer=1024 length=5223881 timeMs=479.0 timeSec=0.479 bytesPerSec=10905806 (stream only)
    // buffer=1024 length=5223881 timeMs=451.0 timeSec=0.451 bytesPerSec=11582885 (stream only)
    // buffer=1024 length=5223881 timeMs=524.0 timeSec=0.524 bytesPerSec=9969239 (nio stream only)
    // buffer=1024 length=5223881 timeMs=548.0 timeSec=0.548 bytesPerSec=9532630 (nio stream only)
    // buffer=2048 length=5223881 timeMs=696.0 timeSec=0.696 bytesPerSec=7505576
    // buffer=2048 length=5223881 timeMs=648.0 timeSec=0.648 bytesPerSec=8061545
    // buffer=2048 length=5223881 timeMs=635.0 timeSec=0.635 bytesPerSec=8226584
    // buffer=4096 length=5223881 timeMs=517.0 timeSec=0.517 bytesPerSec=10104219
    
    // buffer=1024 length=43892497 timeMs=3678.0 timeSec=3.678 bytesPerSec=11933795
    // buffer=1024 length=43892497 timeMs=4170.0 timeSec=4.17  bytesPerSec=10525779
    // buffer=1024 length=43892497 timeMs=3594.0 timeSec=3.594 bytesPerSec=12212715
    // buffer=1024 length=43892497 timeMs=3576.0 timeSec=3.576 bytesPerSec=12274188 (stream only)
    // buffer=1024 length=43892497 timeMs=3720.0 timeSec=3.72  bytesPerSec=11799058 (stream only)
    // buffer=1024 length=43892497 timeMs=4309.0 timeSec=4.309 bytesPerSec=10186237 (nio stream only)
    // buffer=1024 length=43892497 timeMs=4237.0 timeSec=4.237 bytesPerSec=10359334 (nio stream only)
    // buffer=1024 length=43892497 timeMs=659.0  timeSec=0.659 bytesPerSec=66604700 (log4j disabled)
    // buffer=2048 length=43892497 timeMs=4634.0 timeSec=4.634 bytesPerSec=9471838
    // buffer=2048 length=43892497 timeMs=5159.0 timeSec=5.159 bytesPerSec=8507947
    // buffer=2048 length=43892497 timeMs=4531.0 timeSec=4.531 bytesPerSec=9687154
    
    // buffer=512  length=2241943652 timeMs=43740.0  timeSec=43.74   bytesPerSec=51256142 (log4j disabled)
    // buffer=1024 length=2241943652 timeMs=167802.0 timeSec=167.802 bytesPerSec=13360649
    // buffer=1024 length=2241943652 timeMs=165927.0 timeSec=165.927 bytesPerSec=13511627
    // buffer=1024 length=2241943652 timeMs=169730.0 timeSec=169.73  bytesPerSec=13208883
    // buffer=1024 length=2241943652 timeMs=169794.0 timeSec=169.794 bytesPerSec=13203904 (stream only)
    // buffer=1024 length=2241943652 timeMs=193599.0 timeSec=193.599 bytesPerSec=11580347 (nio stream only)
    // buffer=1024 length=2241943652 timeMs=196741.0 timeSec=196.741 bytesPerSec=11395406 (nio stream only)
    // buffer=1024 length=2241943652 timeMs=185354.0 timeSec=185.354 bytesPerSec=12095469 (nio to file)
    // buffer=1024 length=2241943652 timeMs=44051.0  timeSec=44.051  bytesPerSec=50894274 (log4j disabled)
    // buffer=1024 length=2241943652 timeMs=196019.0 timeSec=196.019 bytesPerSec=11437379 (log4j enabled again)
    // buffer=1024 length=2241943652 timeMs=21198.0  timeSec=21.198  bytesPerSec=105762037 (log4j disabled, file deleted)
    // buffer=1024 length=2241943652 timeMs=21267.0  timeSec=21.267  bytesPerSec=105418896 (log4j disabled, proxy)
    // buffer=2048 length=2241943652 timeMs=222272.0 timeSec=222.272 bytesPerSec=10086487
    // buffer=2048 length=2241943652 timeMs=222436.0 timeSec=222.436 bytesPerSec=10079050
    // buffer=2048 length=2241943652 timeMs=48232.0  timeSec=48.232  bytesPerSec=46482494 (log4j disabled)
    // buffer=4096 length=2241943652 timeMs=179426.0 timeSec=179.426 bytesPerSec=12495088
    // buffer=16384 lngth=2241943652 timeMs=170626.0 timeSec=170.626 bytesPerSec=13139519
}
      