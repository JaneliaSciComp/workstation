package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;
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
    
    private String convertLongString (LongString data) {
        return LongStringHelper.asLongString(data.getBytes()).toString();
    }

    /**
     * Successful refresh update received
     */
    @Override
    public void handle(String string, Delivery message) throws IOException {
        // fire notice to AnnotationModel
        Map<String,Object> msgHeaders = message.getProperties().getHeaders();
        if (msgHeaders==null) {
            throw new IOException("Issue trying to process metadata from update");
        }
        byte[] msgBody = message.getBody();
        String user = convertLongString((LongString) msgHeaders.get(HeaderConstants.USER));
        Long workspace = Long.parseLong(convertLongString((LongString) msgHeaders.get(HeaderConstants.WORKSPACE)));
        MessageType action =  MessageType.valueOf(convertLongString((LongString) msgHeaders.get(HeaderConstants.TYPE)));
        String metadata = convertLongString((LongString) msgHeaders.get(HeaderConstants.METADATA));
        
        switch (action) {
            case NEURON_SAVE_NEURONDATA:
            case NEURON_SAVE_METADATA:
            case NEURON_DELETE:
                // refresh client model, fireModelUpdates, etc.
                if (user==AccessManager.getSubjectKey() && action==MessageType.NEURON_SAVE_METADATA) {
                    // if created neuron, merge model, otherwise ignore
                    
                } else {
                    // update neuron model and refresh
                }
                break;
                
            case REQUEST_NEURON_OWNERSHIP:
                // some other user is asking for ownership of this neuron... process accordingly
                break;
            case NEURON_OWNERSHIP_DECISION:
                // result of ownership request, check decision and use neuron metadata object attached to this message
        }

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
