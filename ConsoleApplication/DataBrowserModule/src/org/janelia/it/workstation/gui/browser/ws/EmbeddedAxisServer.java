package org.janelia.it.workstation.gui.browser.ws;

import javax.xml.ws.Endpoint;

/**
 * This Axis server contains the web service end-points for both the console
 * observer service and the data service. It publishes the end-points using
 * JAX-WS.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmbeddedAxisServer {

    private int port;
    private Endpoint obs;
    private Endpoint cds;

    public EmbeddedAxisServer() throws Exception {
    }

    public void start(int port) {
        this.port = port;
        String baseUrl = "http://localhost:" + port;
        obs = Endpoint.publish(baseUrl + "/axis2/services/obs", new ConsoleObserverImpl());
        cds = Endpoint.publish(baseUrl + "/axis2/services/cds", new ConsoleDataServiceImpl());
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        if (obs != null) {
            obs.stop();
        }
        if (cds != null) {
            cds.stop();
        }
    }

    public Endpoint getObserverEndpoint() {
        return obs;
    }

    public Endpoint getDataServiceEndpoint() {
        return cds;
    }
}
