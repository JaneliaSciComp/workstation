package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.janelia.messaging.broker.sharedworkspace.HeaderConstants;
import org.janelia.messaging.client.ConnectionManager;
import org.janelia.messaging.client.Receiver;
import org.janelia.messaging.broker.sharedworkspace.MessageType;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author schauderd
 * handler to manage updates coming from the persistence broker 
 */


public class RefreshHandler implements DeliverCallback, CancelCallback {
    
    private static final Logger log = LoggerFactory.getLogger(RefreshHandler.class);
    private static final String MESSAGESERVER_URL = ConsoleProperties.getInstance().getProperty("domain.msgserver.url").trim();
    private static final String MESSAGESERVER_USERACCOUNT = ConsoleProperties.getInstance().getProperty("domain.msgserver.useraccount").trim();
    private static final String MESSAGESERVER_PASSWORD = ConsoleProperties.getInstance().getProperty("domain.msgserver.password").trim();
    private static final String MESSAGESERVER_REFRESHEXCHANGE = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.refresh").trim();
    
    private AnnotationModel annotationModel;
    private Channel msgChannel;
    private Receiver msgReceiver;
    static RefreshHandler handler;
    
    private RefreshHandler() {
        
    }
    
    public static RefreshHandler getInstance() {
        if (handler==null) {
            handler = new RefreshHandler();
            handler.init();
        }
        return handler;
    }
    
    
    private void init() {
        try {
            ConnectionManager connManager = ConnectionManager.getInstance();
            connManager.configureTarget(MESSAGESERVER_URL,  MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD);
            msgChannel = connManager.getConnection();
        
            msgReceiver = new Receiver();
            msgReceiver.init(connManager, "ModelRefresh", true);
            msgReceiver.setupReceiver(this);
            log.info("Established connection to message server " + MESSAGESERVER_URL);
        } catch (Exception e) {
            log.error("Problems initializing connection to message server " + MESSAGESERVER_URL +
                    ", with credentials username/password: " + MESSAGESERVER_USERACCOUNT + "/" + MESSAGESERVER_PASSWORD);
            e.printStackTrace();
        }
    }

    /**
     * Successful refresh update received
     */
    @Override
    public void handle(String string, Delivery dlvr) throws IOException {
        // fire notice to AnnotationModel
        /*Map<String,Object> metadata = dlvr.getProperties().getHeaders();
        if (metadata==null) {
            throw new IOException("Issue trying to process metadata from update");
        }
        MessageType msgType = MessageType.valueOf((String)metadata.get(HeaderConstants.TYPE));
        String[] neuronIds = ((String)metadata.get(HeaderConstants.NEURONIDS)).split(",");
            log.info("Update received - " + msgType + ": Neuron Ids - " + neuronIds);    
        // first filter out messages to current shared workspace
        Long workspaceId = (Long)metadata.get("workspace");
        Long currentWorkspaceId = annotationModel.getCurrentWorkspace().getId();
        if (workspaceId!=currentWorkspaceId) 
            return;
        
        // filter out messages that we ourselves sent 
        String user = (String)metadata.get("user");
        if (user==AccessManager.getSubjectKey() && msgType) {
            // if it's a save neuron for a create neuron, find the neuron with the same name and update
//            annotationModel.getNeuronFromNeuronID(workspaceId)
        } 
            return;

  //      log.info("Update received - " + msgType + ": Neuron Ids - " + neuronIds);
        // hook into main GUI thread to update neuron models and fire events on AnnotationModel
        // 
*/
    }

    /**
     * problem receiving update from queue
     */
    @Override
    public void handle(String string) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    /**
     * @param annModel the annModel to set
     */
    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }
 
    
}
