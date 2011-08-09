package org.janelia.it.FlyWorkstation.gui.framework.external_listener;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

public class EmbeddedAxisServer {

    AxisService service;
    SimpleHTTPServer server;

    public EmbeddedAxisServer(int port) {
        try {
            ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            AxisService service = AxisService.createService(ClientInterface.class.getName(), context.getAxisConfiguration());//, RPCMessageReceiver.class, "", "http://samples");
            context.getAxisConfiguration().addService(service);
            server = new SimpleHTTPServer(context, port);
        }
        catch (AxisFault axisFault) {
            SessionMgr.getSessionMgr().handleException(axisFault);
        }
    }

    public void start() {
        try {
            server.start();
        }
        catch (AxisFault axisFault) {
            SessionMgr.getSessionMgr().handleException(axisFault);
        }
    }


    public void stop() {
        server.stop();
    }

}