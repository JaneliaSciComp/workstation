package org.janelia.it.workstation.gui.browser.api;

import java.net.BindException;

import org.janelia.it.workstation.gui.browser.ws.EmbeddedAxisServer;
import org.janelia.it.workstation.gui.browser.ws.ExternalClientMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing services that run in the Workstation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ServiceMgr {

    // Singleton
    private static final ServiceMgr instance = new ServiceMgr();
    public static ServiceMgr getServiceMgr() {
        return instance;
    }

    private static final Logger log = LoggerFactory.getLogger(StateMgr.class);

    private static final int MAX_PORT_TRIES = 20;
    private static final int PORT_INCREMENT = 1000;
    
    private EmbeddedAxisServer axisServer;
    
    public int startAxisServer(int startingPort) {
        int port = startingPort;
        try {
            if (axisServer == null) {
                axisServer = new EmbeddedAxisServer();
            }
            int tries = 0;
            while (true) {
                try {
                    axisServer.start(port);
                    log.info("Started web services on port " + port);
                    ExternalClientMgr.getInstance().setPortOffset(port);
                    break;
                } 
                catch (Exception e) {
                    if (e instanceof BindException || e.getCause() instanceof BindException) {
                        log.info("Could not start web service on port: " + port);
                        port += PORT_INCREMENT;
                        tries++;
                        if (tries >= MAX_PORT_TRIES) {
                            log.error("Tried to start web service on " + MAX_PORT_TRIES + " ports, giving up.");
                            return -1;
                        }
                    } 
                    else {
                        log.error("Could not start web service on port: " + port);
                        throw e;
                    }
                }
            }
            return port;
        } 
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return -1;
        }
    }

    public void initServices() {
        int axisServerPort = startAxisServer(ConsoleProperties.getInt("console.WebService.startingPort"));
        // TODO: this is temporary. we need to inject the port number back into the ConsoleModule for the ToolMgr to use.
        // We will remove this hack once the ToolMgr is ported over.
        SessionMgr.getSessionMgr().setAxisServerPort(axisServerPort);
    }
    
    
}
