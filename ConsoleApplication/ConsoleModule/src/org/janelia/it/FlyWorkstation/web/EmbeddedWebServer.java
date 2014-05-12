package org.janelia.it.FlyWorkstation.web;

import org.eclipse.jetty.server.Server;

/**
 * An embedded web server in the console.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmbeddedWebServer {

    private final int port;
    private Server server;
    
    public EmbeddedWebServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        this.server = new Server(port);
        server.setHandler(new FileProxyService());
        server.start();
    }
    
    public void stop() throws Exception {
        if (server!=null) server.stop();
    }
}
