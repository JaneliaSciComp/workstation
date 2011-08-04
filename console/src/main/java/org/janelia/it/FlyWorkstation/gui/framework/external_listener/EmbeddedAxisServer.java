package org.janelia.it.FlyWorkstation.gui.framework.external_listener;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.http.SimpleHTTPServer;

public class EmbeddedAxisServer {
    public static void main(String[] args) throws Exception {
        ConfigurationContext context = ConfigurationContextFactory.
        createConfigurationContextFromFileSystem(null, null);
        AxisService service =
        AxisService.createService(Echo.class.getName(), context.getAxisConfiguration());//, RPCMessageReceiver.class, "", "http://samples");
        context.getAxisConfiguration().addService(service);
        SimpleHTTPServer server = new SimpleHTTPServer(context, 8080);
        server.start();
    }
}