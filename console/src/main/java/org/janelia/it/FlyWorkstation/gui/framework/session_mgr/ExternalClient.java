/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/9/11
 * Time: 9:00 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import java.net.URL;
import java.util.Map;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;

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
	
    private final String name;
    private final int clientPort;
    
    private String namespace = "http://ws.FlyWorkstation.it.janelia.org/";
    private EndpointReference targetEPR;
    private ServiceClient client;
    
    public ExternalClient(int clientPort, String name) {
        this.clientPort = clientPort;
        this.name = name;
    }
    
    public void init(String endpointUrl) throws Exception {
        
    	URL endpointURL = new URL(endpointUrl);

    	if (endpointURL.getPort() != clientPort) {
    		throw new Exception("Endpoint's port does not match the reserved port for this client");
    	}
    	
		targetEPR = new EndpointReference(endpointUrl);
		
        Options options = new Options();
        options.setTo(targetEPR);
        
        client = new ServiceClient();
        client.setOptions(options); 
    }
    
    public void sendMessage(String operationName, Map<String,Object> parameters) throws Exception {

        System.out.println("Sending "+operationName+" message to: "+ targetEPR.getAddress());
        
    	if (targetEPR == null){
    		throw new IllegalStateException("init(String endpointUrl) must be called on the ExternalClient before any other methods.");
    	}
    	
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace ns = fac.createOMNamespace(namespace, "ns");
        final OMElement operation = fac.createOMElement(operationName, ns);
        
        for(String parameterName : parameters.keySet()) {
            OMElement param = fac.createOMElement(parameterName, ns);
            param.setText(parameters.get(parameterName).toString());
            operation.addChild(param);
        }
        
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
		        client.fireAndForget(operation);
			}
			
			@Override
			protected void hadSuccess() {
				System.out.println("Delivered message to "+targetEPR.getAddress());
			}
			
			@Override
			protected void hadError(Throwable error) {
				System.out.println("Error sending message to "+targetEPR.getAddress()+" : "+error.getClass().getName()+"/"+error.getMessage());
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

}
