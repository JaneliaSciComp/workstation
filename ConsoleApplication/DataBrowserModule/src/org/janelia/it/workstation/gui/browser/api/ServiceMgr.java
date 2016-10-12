package org.janelia.it.workstation.gui.browser.api;

import java.net.BindException;

import org.janelia.it.workstation.gui.browser.web.EmbeddedWebServer;
import org.janelia.it.workstation.gui.browser.ws.EmbeddedAxisServer;
import org.janelia.it.workstation.gui.browser.ws.ExternalClientMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.util.ConsoleProperties;
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
    private EmbeddedWebServer webServer;

    private int axisServerPort;

    private int webServerPort;
    
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

    public int startWebServer(int startingPort) {
        int port = startingPort;
        try {
            if (webServer == null) {
                webServer = new EmbeddedWebServer();
            }
            int tries = 0;
            while (true) {
                try {
                    webServer.start(port);
                    log.info("Started web server on port " + port);
                    break;
                } 
                catch (Exception e) {
                    if (e instanceof BindException || e.getCause() instanceof BindException) {
                        log.info("Could not start web server on port: " + port);
                        port += PORT_INCREMENT;
                        tries++;
                        if (tries >= MAX_PORT_TRIES) {
                            log.error("Tried to start web server on " + MAX_PORT_TRIES + " ports, giving up.");
                            return -1;
                        }
                    } 
                    else {
                        log.error("Could not start web server on port: " + port);
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
        this.axisServerPort = startAxisServer(ConsoleProperties.getInt("console.WebService.startingPort"));
        this.webServerPort = startWebServer(ConsoleProperties.getInt("console.WebServer.startingPort"));
    }

    public int getAxisServerPort() {
        return axisServerPort;
    }

    public int getWebServerPort() {
        return webServerPort;
    }
    
}
