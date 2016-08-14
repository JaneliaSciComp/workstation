package org.janelia.it.workstation.gui.browser.ws;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An external program that registers in hopes of receiving events from the workstation. Registration is a two-step 
 * process. First, the client asks us for a port that it can use. The workstation reserves a port for that client and
 * sends it back. Then the client starts its web service on that port, with an interface matching the ConsoleObserver. 
 * Finally, the client registers its active endpoint with the workstation and begins receiving any events that are
 * generated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExternalClient {
	
	private static final Logger log = LoggerFactory.getLogger(ExternalClient.class);
	
	private static HttpClient httpClient;
	static {
		// Strange threading/connection issues were resolved by reusing a single HTTP Client with a high connection count.
		// The solution was found here:
		// http://amilachinthaka.blogspot.com/2010/01/improving-axis2-http-transport-client.html
	    MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
	    HttpConnectionManagerParams params = new HttpConnectionManagerParams();
	    params.setDefaultMaxConnectionsPerHost(20);
	    multiThreadedHttpConnectionManager.setParams(params);
	    httpClient = new HttpClient(multiThreadedHttpConnectionManager);
	}
	
	protected static final int CONNECTION_TIME_OUT_MILLIS = 5000;
	private final String name;
    private final int clientPort;

    private EndpointReference targetEPR;

    public ExternalClient(int clientPort, String name) {
        this.clientPort = clientPort;
        this.name = name;
    }
    
    public void init(String endpointUrl) throws Exception {
    	if ((new URL(endpointUrl)).getPort() != clientPort) {
    		throw new Exception("Endpoint's port does not match the reserved port for this client");
    	}
		this.targetEPR = new EndpointReference(endpointUrl);
    }

    private ServiceClient getNewClient() throws Exception {
    	
        Options options = new Options();
        options.setTo(targetEPR);
        options.setTimeOutInMilliSeconds(CONNECTION_TIME_OUT_MILLIS);
        
        ServiceClient client = new ServiceClient();
        client.setOptions(options); 
    	client.getServiceContext().getConfigurationContext().setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
    	
    	httpClient.getHttpConnectionManager().closeIdleConnections(0);
    	
        return client;
    }
    
    public void sendMessage(String operationName, Map<String,Object> parameters) throws Exception {

    	final ServiceClient client = getNewClient();
    	
    	if (targetEPR == null) {
            log.error("External client {} on port {} has no end point",name,clientPort);
    		throw new IllegalStateException("init(String endpointUrl) must be called on the ExternalClient before any other methods.");
    	}
    	
    	log.info("Sending {} message to endpoint {}",operationName,targetEPR.getAddress());
    	
        OMFactory fac = OMAbstractFactory.getOMFactory();
        String namespace = "http://ws.FlyWorkstation.it.janelia.org/";
        OMNamespace ns = fac.createOMNamespace(namespace, "ns");
        final OMElement operation = fac.createOMElement(operationName, ns);
        
        for(String parameterName : parameters.keySet()) {
            OMElement param = fac.createOMElement(parameterName, ns);
            param.setText(parameters.get(parameterName).toString());
            operation.addChild(param);
        }
        
        // We have to use a worker thread because even though it doesn't wait for a reply, fireAndForget blocks until 
        // it connects and sends. 
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
                client.fireAndForget(operation);
		        client.cleanupTransport();
			}
			
			@Override
			protected void hadSuccess() {
			}
			
			@Override
			protected void hadError(Throwable error) {
				log.error("Error sending message to "+targetEPR.getAddress(),error);
                log.info("Removing client "+targetEPR.getAddress()+
                        " because of communication failures");
                ExternalClientMgr.getInstance().removeExternalClientByPort(clientPort);
			}
			
		};
		
		worker.execute();
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getName() {
        return name;
    }

    /**
     * Method to check existence of the SOAP server
     * Could be much smarter
     * @return returns true if the server responds intelligently
     */
    public boolean isConnected(){
        try {
            URL test = new URL("http://localhost:"+clientPort);
            HttpURLConnection conn = (HttpURLConnection)test.openConnection();
            String response = conn.getResponseMessage();
            return ("Method Not Allowed".equals(response));
        }
        catch (Exception e) {
            log.error("Error performing the health check on external client "+name);
        }
        return false;
    }

}
