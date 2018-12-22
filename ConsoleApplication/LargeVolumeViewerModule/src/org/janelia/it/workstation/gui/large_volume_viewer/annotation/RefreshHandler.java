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
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.messaging.broker.sharedworkspace.HeaderConstants;
import org.janelia.messaging.broker.sharedworkspace.MessageType;
import org.janelia.messaging.client.ConnectionManager;
import org.janelia.messaging.client.Receiver;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.perf4j.StopWatch;
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
    private boolean receiveUpdates = true;
    private boolean freezeUpdates = false;
    private Map<Long, Map<String,Object>> updatesMap = new HashMap<>();
    
    /**
     * @return the receiveUpdates
     */
    public boolean isReceiveUpdates() {
        return receiveUpdates;
    }

    /**
     * @param receiveUpdates the receiveUpdates to set
     */
    public void setReceiveUpdates(boolean receiveUpdates) {
        this.receiveUpdates = receiveUpdates;
    }
    
    private RefreshHandler() {
    }
    
    public static Optional<RefreshHandler> getInstance() {
        if (handler == null) {
            RefreshHandler uninitializedHandler = new RefreshHandler();
            if (uninitializedHandler.init())
                handler = uninitializedHandler;
            else
                return Optional.empty();
        }
        return Optional.of(handler);
    }

    private boolean init() {
        try {
            ConnectionManager connManager = ConnectionManager.getInstance();
            connManager.configureTarget(MESSAGESERVER_URL,  MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD);
            connManager.setThreadPoolSize(20);
            msgChannel = connManager.getConnection(1);
        
            msgReceiver = new Receiver();
            msgReceiver.init(connManager, "ModelRefresh", true, 1);
            msgReceiver.setupReceiver(this);
            log.info("Established connection to message server " + MESSAGESERVER_URL);
            return true;
        } catch (Exception e) {
            AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
            String error = "Problems initializing connection to message server " + MESSAGESERVER_URL +
                    " with username: " + MESSAGESERVER_USERACCOUNT;
            annotationMgr.presentError(error, "Problem connecting to Message Server");
            log.error(error, e);
            return false;
        }
    }
    
    private String convertLongString (LongString data) {
        return LongStringHelper.asLongString(data.getBytes()).toString();
    }
    
    public void refreshNeuronUpdates() {
        // freeze neuron updates 
        freezeUpdates = true;
        // play back all the latest neurons and empty the map
        Iterator<Long> neuronIterator = updatesMap.keySet().iterator();
        log.info ("Number of updates to refresh: {}",updatesMap.size());
        while (neuronIterator.hasNext()) {
             try {
                 Long neuronId = neuronIterator.next();
                 Map<String, Object> neuronData = updatesMap.get(neuronId);
                 TmNeuronMetadata neuron = (TmNeuronMetadata)neuronData.get("neuron");
                 MessageType action = (MessageType) neuronData.get("action");
                 String user = (String) neuronData.get("user");
                 // if not a neuron CRUD action, ignore
                 switch (action) {
                     case NEURON_CREATE:
                         log.info("processing remote create: {}",neuron.getName());
                         annotationModel.getNeuronManager().addNeuron(neuron);
                         annotationModel.fireBackgroundNeuronCreated(neuron);
                         break;
                     case NEURON_SAVE_NEURONDATA:
                     case NEURON_SAVE_METADATA:
                          log.info("processing remote save: {},",neuron.getName());
                          annotationModel.getNeuronManager().addNeuron(neuron);
                          annotationModel.fireBackgroundNeuronChanged(neuron);
                         break;
                     case NEURON_DELETE:
                         log.info("processing remote delete: {},",neuron.getName());
                         annotationModel.fireBackgroundNeuronDeleted(neuron);
                         break;
                 }                 
                 
             } catch (Exception e) {
                 // skip this update
                 log.error("Error refreshing the annotation model", e);
             }
        }
        updatesMap.clear();
        
        freezeUpdates = true;
    }
    
    private void addNeuronUpdate (Delivery message,  Map<String, Object> msgHeaders, MessageType action, String user) {
         try {             
             ObjectMapper mapper = new ObjectMapper();
             String metadata = convertLongString((LongString) msgHeaders.get(HeaderConstants.METADATA));
             TmNeuronMetadata neuron = mapper.readValue(metadata, TmNeuronMetadata.class);

             TmProtobufExchanger exchanger = new TmProtobufExchanger();
             byte[] msgBody = message.getBody();
             exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
             
             // assume this has to do with neuron CRUD; otherwise ignore
             Map neuronData = new HashMap<>();
             neuronData.put("neuron", neuron);
             neuronData.put("action", action);
             neuronData.put("user", user);
             log.info ("Adding neuron remote update: {}",neuron.getName());
             if (neuron!=null && neuron.getId()!=null) {
                 this.updatesMap.put(neuron.getId(), neuronData);
             }
         } catch (Exception e) {
             // any type of exception, log the exception and dump the neuron
             log.info("Problem storing update {}",e.getMessage());
         }
    }

    /**
     * Successful refresh update received
     */
    @Override
    public void handle(String string, Delivery message) throws IOException {
        try {
            StopWatch stopWatch = new StopWatch();
            Map<String, Object> msgHeaders = message.getProperties().getHeaders();
        
            if (msgHeaders == null) {
                log.error("Issue trying to process metadata from update");
            }
            // thead logging
            log.debug ("Thread Count: {}", ManagementFactory.getThreadMXBean().getThreadCount());
            log.debug ("Heap Size: {}", Runtime.getRuntime().totalMemory());
            
            log.debug("message properties: TYPE={},USER={},WORKSPACE={},METADATA={}", msgHeaders.get(HeaderConstants.TYPE), msgHeaders.get(HeaderConstants.USER),
                    msgHeaders.get(HeaderConstants.WORKSPACE), msgHeaders.get(HeaderConstants.METADATA));

            MessageType action = MessageType.valueOf(convertLongString((LongString) msgHeaders.get(HeaderConstants.TYPE)));
            String user = convertLongString((LongString) msgHeaders.get(HeaderConstants.USER));
            
            Long workspace = Long.parseLong(convertLongString((LongString) msgHeaders.get(HeaderConstants.WORKSPACE)));
            
            // flag to suppress shared updates
            if (!receiveUpdates && !freezeUpdates && !user.equals(AccessManager.getSubjectKey())) {
                if (workspace != null && annotationModel!=null && annotationModel.getCurrentWorkspace() != null
                    && workspace.longValue()==annotationModel.getCurrentWorkspace().getId().longValue()) {
                    addNeuronUpdate (message, msgHeaders, action, user);
                    log.debug("SHARED UPDATE TIME: {}", stopWatch.getElapsedTime());
                }
                return;
            }
            
            if (action == MessageType.ERROR_PROCESSING) {
                if (user != null && user.equals(AccessManager.getSubjectKey())) {
                    log.info("Error message received from server");
                    byte[] msgBody = message.getBody();
                    logError(new String(msgBody));
                }
                return;
            }

            if (!msgHeaders.containsKey(HeaderConstants.METADATA) || msgHeaders.get(HeaderConstants.METADATA)==null) {
                log.error("Message includes no neuron information; rejecting processing");
                return;
            }
                
            String metadata = convertLongString((LongString) msgHeaders.get(HeaderConstants.METADATA));
            ObjectMapper mapper = new ObjectMapper();
            TmNeuronMetadata neuron = mapper.readValue(metadata, TmNeuronMetadata.class);

            TmNeuronMetadata localNeuron = annotationModel.getNeuronManager().getNeuronById(neuron.getId());// decrease the sync level
            if (localNeuron != null) {
                localNeuron.decrementSyncLevel();
                if (localNeuron.getSyncLevel() == 0) {
                    localNeuron.setSynced(true);
                }
            }

            log.info("Processed headers for workspace Id {}", workspace);
            // if not this workspace or user isn't looking at a workspace right now or workspace not relating to a workspace update, filter out message
            
            log.debug("PRE-WORKSPACE TIME: {}", stopWatch.getElapsedTime());
            if (workspace == null || annotationModel.getCurrentWorkspace() == null
                    || workspace.longValue() != annotationModel.getCurrentWorkspace().getId().longValue()) {
                return;
            }

            if (action == MessageType.NEURON_OWNERSHIP_DECISION) {
                boolean decision = Boolean.parseBoolean(convertLongString((LongString) msgHeaders.get(HeaderConstants.DECISION)));
                if (decision) {
                    TmNeuronMetadata origNeuron = annotationModel.getNeuronManager().getNeuronById(neuron.getId());
                    origNeuron.setOwnerKey(neuron.getOwnerKey());
                    origNeuron.setWriters(neuron.getWriters());
                    origNeuron.setReaders(neuron.getReaders());
                }
                annotationModel.getNeuronManager().completeOwnershipRequest(decision);
                SwingUtilities.invokeLater(() -> {
                    StopWatch stopWatch2 = new StopWatch();
                    annotationModel.fireBackgroundNeuronOwnershipChanged(neuron);
                    stopWatch2.stop();
                    log.info("RefreshHandler.invokeLater: handled ownership decision update in {} ms", stopWatch2.getElapsedTime());
                });
            } else if (action == MessageType.NEURON_CREATE && user.equals(AccessManager.getSubjectKey())) {
                // complete the future outside of the swing thread, since the copyGUI thread is blocked
                StopWatch stopWatch2 = new StopWatch();
                handleNeuronCreate(message, neuron, n -> annotationModel.getNeuronManager().completeCreateNeuron(n));
                stopWatch2.stop();
                log.info("RefreshHandler: Remote own neuron creation update in {} ms", stopWatch2.getElapsedTime());
                log.debug("TOTAL MESSAGING PROCESSING TIME: {}", stopWatch.getElapsedTime());
            } else if (action == MessageType.REQUEST_NEURON_OWNERSHIP) {
                // some other user is asking for ownership of this neuron... process accordingly
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            StopWatch stopWatch2 = new StopWatch();
                            // fire notice to AnnotationModel
                            Map<String, Object> msgHeaders = message.getProperties().getHeaders();
                            if (msgHeaders == null) {
                                throw new IOException("Issue trying to process metadata from update");
                            }

                            // change relevant to this workspace and not executed on this client, so update model or process request
                            switch (action) {
                                case NEURON_CREATE:
                                    handleNeuronCreate(message, neuron, n -> annotationModel.getNeuronManager().addNeuron(n));
                                    break;
                                case NEURON_SAVE_NEURONDATA:
                                case NEURON_SAVE_METADATA:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        handleNeuronChanged(message, neuron);
                                    }
                                    break;
                                case NEURON_DELETE:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        handleNeuronDeleted(message, neuron);
                                    }
                                    break;
                            }
                            stopWatch2.stop();
                            log.debug("RefreshHandler.invokeLater: handled update in {} ms", stopWatch2.getElapsedTime());
                        } catch (Exception e) {
                            log.error("Exception thrown in main GUI thread during message processing", e);
                            logError(e.getMessage());
                        }
                       log.debug("TOTAL MESSAGING PROCESSING TIME: {}", stopWatch.getElapsedTime());
                    }
                });
            }
            stopWatch.stop();
            log.info("RefreshHandler: handled message in {} ms", stopWatch.getElapsedTime());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void handleNeuronCreate(Delivery message, TmNeuronMetadata neuron, Consumer<TmNeuronMetadata> neuronAction) {
       try {
           log.info("remote processing create neuron " + neuron.getName());
           TmProtobufExchanger exchanger = new TmProtobufExchanger();
           byte[] msgBody = message.getBody();
           exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
           neuronAction.accept(neuron);
           annotationModel.fireBackgroundNeuronCreated(neuron);
       } catch (Exception e) {  
           logError("Error handling neuron creation: " + e.getMessage());
           log.error("Error handling neuron creation message for {}", neuron, e);
       }  
    }

    private void handleNeuronChanged(Delivery message, TmNeuronMetadata neuron) {
        try {
            log.info("remote processing change neuron " + neuron.getName());
            TmProtobufExchanger exchanger = new TmProtobufExchanger();
            byte[] msgBody = message.getBody();
            exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
            annotationModel.getNeuronManager().addNeuron(neuron);
            annotationModel.fireBackgroundNeuronChanged(neuron);
        } catch (Exception e) {
           logError("Error handling neuron change: " + e.getMessage());
           log.error("Error handling neuron changed message for {}", neuron, e);
        }
    }

    private void handleNeuronDeleted(Delivery message, TmNeuronMetadata neuron) {
        try {
            log.info("remote processing delete neuron" + neuron.getName());
            TmProtobufExchanger exchanger = new TmProtobufExchanger();
            byte[] msgBody = message.getBody();
            exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
            annotationModel.fireBackgroundNeuronDeleted(neuron);
        } catch (Exception e) {
           logError("Error handling neuron change: " + e.getMessage());
           log.error("Error handling neuron changed message for {}", neuron, e);
        }
    }

    public void logError(String errorMsg) {
        String error = "Problems receiving message updates, " + errorMsg;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                annotationMgr.presentError(errorMsg, new RuntimeException (error));
            }
        });
        log.error(error);
    }
    
    
    @Override
    public void handle(String consumerTag) {
        // this is a consumer cancel callback.... if this gets called with null, assume it is just a problem with rabbitmq
        // initializing
        log.info("Rabbitmq cancelling consumer");
    }

    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }
 
    
}
