package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.janelia.messaging.broker.sharedworkspace.HeaderConstants;
import org.janelia.messaging.client.ConnectionManager;
import org.janelia.messaging.client.Receiver;
import org.janelia.messaging.broker.sharedworkspace.MessageType;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
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
            AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
            String error = "Problems initializing connection to message server " + MESSAGESERVER_URL +
                    ", with credentials username/password: " + MESSAGESERVER_USERACCOUNT + "/" + MESSAGESERVER_PASSWORD;
            annotationMgr.presentError(error, "Problem connecting to Message Server");
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
        Map<String, Object> msgHeaders = message.getProperties().getHeaders();
        if (msgHeaders == null) {
            throw new IOException("Issue trying to process metadata from update");
        }
        MessageType action = MessageType.valueOf(convertLongString((LongString) msgHeaders.get(HeaderConstants.TYPE)));
        String user = convertLongString((LongString) msgHeaders.get(HeaderConstants.USER));
        Long workspace = Long.parseLong(convertLongString((LongString) msgHeaders.get(HeaderConstants.WORKSPACE)));
        if (action==MessageType.ERROR_PROCESSING) {
             byte[] msgBody = message.getBody();
             handle (new String(msgBody));
             return;
        }
        
        String metadata = convertLongString((LongString) msgHeaders.get(HeaderConstants.METADATA));
        ObjectMapper mapper = new ObjectMapper();
        TmNeuronMetadata neuron = mapper.readValue(metadata, TmNeuronMetadata.class);
        
        if (neuron!=null) {
            // note that sometimes the neuron doesn't exist locally, as when we've deleted it, so check for null
            TmNeuronMetadata localNeuron = annotationModel.getNeuronManager().getNeuronById(neuron.getId());// decrease the sync level
            if (localNeuron != null) {
                localNeuron.decrementSyncLevel();
                if (localNeuron.getSyncLevel() == 0) {
                    localNeuron.setSynced(true);
                }
            }
        }
        
       
        // if not this workspace, filter out message
        if (workspace.longValue() != annotationModel.getCurrentWorkspace().getId().longValue()) {
            return;
        }
        
        if (action==MessageType.NEURON_OWNERSHIP_DECISION) {
             boolean decision = Boolean.parseBoolean(convertLongString((LongString) msgHeaders.get(HeaderConstants.DECISION)));
             annotationModel.getNeuronManager().completeOwnershipRequest(decision);
             annotationModel.fireBackgroundNeuronOwnershipChanged(neuron);
        } else if (action == MessageType.REQUEST_NEURON_OWNERSHIP) {
            // some other user is asking for ownership of this neuron... process accordingly
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
// fire notice to AnnotationModel
                        Map<String, Object> msgHeaders = message.getProperties().getHeaders();
                        if (msgHeaders == null) {
                            throw new IOException("Issue trying to process metadata from update");
                        }

                        // change relevant to this workspace and not execute don this client, so update model or process request
                        switch (action) {
                            case NEURON_CREATE:
                                TmProtobufExchanger exchanger = new TmProtobufExchanger();
                                byte[] msgBody = message.getBody();
                                exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
                                if (user.equals(AccessManager.getSubjectKey())) {
                                    annotationModel.getNeuronManager().mergeCreatedNeuron(neuron);
                                } else {
                                    annotationModel.getNeuronManager().addNeuron(neuron);
                                }
                                annotationModel.fireBackgroundNeuronCreated(neuron);
                                break;
                            case NEURON_SAVE_NEURONDATA:
                            case NEURON_SAVE_METADATA:
                                if (!user.equals(AccessManager.getSubjectKey())) {
                                    exchanger = new TmProtobufExchanger();
                                    msgBody = message.getBody();
                                    exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
                                    annotationModel.getNeuronManager().addNeuron(neuron);
                                    annotationModel.fireBackgroundNeuronChanged(neuron);
                                }
                                break;
                            case NEURON_DELETE:
                                if (!user.equals(AccessManager.getSubjectKey())) {
                                    exchanger = new TmProtobufExchanger();
                                    msgBody = message.getBody();
                                    exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
                                    annotationModel.fireBackgroundNeuronDeleted(neuron);
                                }
                                break;
                        }
                    } catch (Exception e) {
                        handle (e.getMessage());
                    }

                }
            });
        }
    }

    /**
     * error callback receiving update from queue
     */
    @Override
    public void handle(String errorMsg) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        String error = "Problems receiving message updates, " + errorMsg;
        annotationMgr.presentError(error, "Problem receiving message from Message Server");
        log.error(error);
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
