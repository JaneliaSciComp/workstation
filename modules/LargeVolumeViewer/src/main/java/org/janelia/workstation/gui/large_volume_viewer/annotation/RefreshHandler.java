package org.janelia.workstation.gui.large_volume_viewer.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.messaging.core.ConnectionManager;
import org.janelia.messaging.core.MessageConsumer;
import org.janelia.messaging.core.MessageHandler;
import org.janelia.messaging.utils.MessagingUtils;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.gui.large_volume_viewer.model_adapter.NeuronMessageConstants;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author schauderd
 * handler to manage updates coming from the persistence broker 
 */
public class RefreshHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(RefreshHandler.class);
    private static final String MESSAGESERVER_URL = ConsoleProperties.getInstance().getProperty("domain.msgserver.url").trim();
    private static final String MESSAGESERVER_USERACCOUNT = ConsoleProperties.getInstance().getProperty("domain.msgserver.useraccount").trim();
    private static final String MESSAGESERVER_PASSWORD = ConsoleProperties.getInstance().getProperty("domain.msgserver.password").trim();
    private static final String MESSAGESERVER_REFRESHEXCHANGE = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.refresh").trim();
    
    private AnnotationModel annotationModel;
    private MessageConsumer msgReceiver;
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
            ConnectionManager connManager = new ConnectionManager(MESSAGESERVER_URL,  MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD, 20);
            msgReceiver = new MessageConsumer(connManager);
            msgReceiver.connect("ModelRefresh", "ModelRefresh", 1);
            msgReceiver.setupMessageHandler(this);
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
    
    void refreshNeuronUpdates() {
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
                 NeuronMessageConstants.MessageType action = (NeuronMessageConstants.MessageType) neuronData.get("action");
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
    
    private void addNeuronUpdate (Map<String, Object> msgHeaders, byte[] msgBody, NeuronMessageConstants.MessageType action, String user) {
         try {             
             ObjectMapper mapper = new ObjectMapper();
             String metadata = MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.METADATA);
             TmNeuronMetadata neuron = mapper.readValue(metadata, TmNeuronMetadata.class);

             TmProtobufExchanger exchanger = new TmProtobufExchanger();
             exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
             
             // assume this has to do with neuron CRUD; otherwise ignore
             Map neuronData = new HashMap<>();
             neuronData.put("neuron", neuron);
             neuronData.put("action", action);
             neuronData.put("user", user);
             log.info ("Adding neuron remote update: {}", neuron.getName());
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
    public void handleMessage(Map<String, Object> msgHeaders, byte[] msgBody) {
        try {
            StopWatch stopWatch = new StopWatch();

            if (msgHeaders == null) {
                logError("Issue trying to process metadata from update");
                return;
            }
            // thead logging
            log.debug ("Thread Count: {}", ManagementFactory.getThreadMXBean().getThreadCount());
            log.debug ("Heap Size: {}", Runtime.getRuntime().totalMemory());
            
            log.debug("message properties: TYPE={},USER={},WORKSPACE={},METADATA={}",
                    msgHeaders.get(NeuronMessageConstants.Headers.TYPE),
                    msgHeaders.get(NeuronMessageConstants.Headers.USER),
                    msgHeaders.get(NeuronMessageConstants.Headers.WORKSPACE),
                    msgHeaders.get(NeuronMessageConstants.Headers.METADATA));

            NeuronMessageConstants.MessageType action = NeuronMessageConstants.MessageType.valueOf(MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.TYPE));
            String user = MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.USER);
            
            Long workspace = MessagingUtils.getHeaderAsLong(msgHeaders, NeuronMessageConstants.Headers.WORKSPACE);
            
            // flag to suppress shared updates
            if (!receiveUpdates && !freezeUpdates && !user.equals(AccessManager.getSubjectKey())) {
                if (workspace != null && annotationModel!=null && annotationModel.getCurrentWorkspace() != null
                    && workspace.longValue()==annotationModel.getCurrentWorkspace().getId().longValue()) {
                    addNeuronUpdate (msgHeaders, msgBody, action, user);
                    log.debug("SHARED UPDATE TIME: {}", stopWatch.getElapsedTime());
                }
                return;
            }
            
            if (action == NeuronMessageConstants.MessageType.ERROR_PROCESSING) {
                if (user != null && user.equals(AccessManager.getSubjectKey())) {
                    log.info("Error message received from server");
                    logError(new String(msgBody));
                }
                return;
            }

            if (!msgHeaders.containsKey(NeuronMessageConstants.Headers.METADATA) || msgHeaders.get(NeuronMessageConstants.Headers.METADATA)==null) {
                log.error("Message includes no neuron information; rejecting processing");
                return;
            }
                
            String metadata = MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.METADATA);
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

            if (action == NeuronMessageConstants.MessageType.NEURON_OWNERSHIP_DECISION) {
                boolean decision = MessagingUtils.getHeaderAsBoolean(msgHeaders, NeuronMessageConstants.Headers.DECISION);
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
            } else if (action == NeuronMessageConstants.MessageType.NEURON_CREATE && user.equals(AccessManager.getSubjectKey())) {
                // complete the future outside of the swing thread, since the copyGUI thread is blocked
                StopWatch stopWatch2 = new StopWatch();
                handleNeuronCreate(neuron, msgBody, n -> annotationModel.getNeuronManager().completeCreateNeuron(n));
                stopWatch2.stop();
                log.info("RefreshHandler: Remote own neuron creation update in {} ms", stopWatch2.getElapsedTime());
                log.debug("TOTAL MESSAGING PROCESSING TIME: {}", stopWatch.getElapsedTime());
            } else if (action == NeuronMessageConstants.MessageType.REQUEST_NEURON_OWNERSHIP) {
                // some other user is asking for ownership of this neuron... process accordingly
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            StopWatch stopWatch2 = new StopWatch();
                            // fire notice to AnnotationModel
                            // change relevant to this workspace and not executed on this client,
                            // update model or process request
                            switch (action) {
                                case NEURON_CREATE:
                                    handleNeuronCreate(neuron, msgBody, n -> annotationModel.getNeuronManager().addNeuron(n));
                                    break;
                                case NEURON_SAVE_NEURONDATA:
                                case NEURON_SAVE_METADATA:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        handleNeuronChanged(neuron, msgBody);
                                    }
                                    break;
                                case NEURON_DELETE:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        handleNeuronDeleted(neuron, msgBody);
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

    private void handleNeuronCreate(TmNeuronMetadata neuron, byte[] msgBody, Consumer<TmNeuronMetadata> neuronAction) {
       try {
           log.info("remote processing create neuron " + neuron.getName());
           TmProtobufExchanger exchanger = new TmProtobufExchanger();
           exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
           neuronAction.accept(neuron);
           annotationModel.fireBackgroundNeuronCreated(neuron);
       } catch (Exception e) {  
           logError("Error handling neuron creation: " + e.getMessage());
           log.error("Error handling neuron creation message for {}", neuron, e);
       }  
    }

    private void handleNeuronChanged(TmNeuronMetadata neuron, byte[] msgBody) {
        try {
            log.info("remote processing change neuron " + neuron.getName());
            TmProtobufExchanger exchanger = new TmProtobufExchanger();
            exchanger.deserializeNeuron(new ByteArrayInputStream(msgBody), neuron);
            annotationModel.getNeuronManager().addNeuron(neuron);
            annotationModel.fireBackgroundNeuronChanged(neuron);
        } catch (Exception e) {
           logError("Error handling neuron change: " + e.getMessage());
           log.error("Error handling neuron changed message for {}", neuron, e);
        }
    }

    private void handleNeuronDeleted(TmNeuronMetadata neuron, byte[] msgBody) {
        try {
            log.info("remote processing delete neuron" + neuron.getName());
            TmProtobufExchanger exchanger = new TmProtobufExchanger();
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
    public void cancelMessage(String routingTag) {
        log.info("The messaging system might have canceled the consumer");
    }

    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }
 
    
}
