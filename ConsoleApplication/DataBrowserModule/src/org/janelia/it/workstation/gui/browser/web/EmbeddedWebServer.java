package org.janelia.it.workstation.gui.browser.web;

import org.eclipse.jetty.server.Server;

/**
 * An embedded web server in the console.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmbeddedWebServer {

	private int port;
    private Server server;
    
    public EmbeddedWebServer() {
    }
    
    public void start(int port) throws Exception {
    	this.port = port;
        this.server = new Server(port);
        server.setHandler(new FileProxyService());
        server.start();
    }

    public int getPort() {
    	return port;
    }
    
    public void stop() throws Exception {
        if (server!=null) server.stop();
    }
}
