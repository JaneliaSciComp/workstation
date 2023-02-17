package org.janelia.workstation.controller.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.messaging.core.AsyncMessageConsumer;
import org.janelia.messaging.core.ConnectionManager;
import org.janelia.messaging.core.MessageConnection;
import org.janelia.messaging.core.MessageHandler;
import org.janelia.messaging.core.impl.AsyncMessageConsumerImpl;
import org.janelia.messaging.utils.MessagingUtils;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronMessageConstants;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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

    private NeuronManager annotationModel;
    private AsyncMessageConsumer msgReceiver;
    static RefreshHandler handler;
    private boolean receiveUpdates = false;
    private boolean freezeUpdates = false;
    private Map<Long, Map<String, Object>> updatesMap = new HashMap<>();
    private TmModelManager modelManager;

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
        modelManager = TmModelManager.getInstance();
    }

    public void setModel(TmModelManager modelManager) {
        this.modelManager = modelManager;
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
        MessageConnection messageConnection = ConnectionManager.getInstance()
                .getConnection(
                        MESSAGESERVER_URL, MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD, 20,
                        (exc) -> {
                           // AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
                            String error = "Problems initializing connection to message server " + MESSAGESERVER_URL +
                                    " with username: " + MESSAGESERVER_USERACCOUNT;
                          //  annotationMgr.presentError(error, "Problem connecting to Message Server");
                            log.error(error, exc);
                        });
        msgReceiver = new AsyncMessageConsumerImpl(messageConnection);
        ((AsyncMessageConsumerImpl)msgReceiver).setAutoAck(true);
        // create a temporary binding to ModelRefresh exchange and listen on that channel for the replies
        msgReceiver.bindAndConnectTo("ModelRefresh", "", null);
        msgReceiver.subscribe(this);
        log.info("Established connection to message server " + MESSAGESERVER_URL);
        return messageConnection.isOpen();
    }

    void refreshNeuronUpdates() {
        // freeze neuron updates 
        freezeUpdates = true;
        // play back all the latest neurons and empty the map
        Iterator<Long> neuronIterator = updatesMap.keySet().iterator();
        log.info("Number of updates to refresh: {}", updatesMap.size());
        while (neuronIterator.hasNext()) {
            try {
                Long neuronId = neuronIterator.next();
                Map<String, Object> neuronData = updatesMap.get(neuronId);
                TmNeuronMetadata neuron = (TmNeuronMetadata) neuronData.get("neuron");
                NeuronMessageConstants.MessageType action = (NeuronMessageConstants.MessageType) neuronData.get("action");
                String user = (String) neuronData.get("user");
                // if not a neuron CRUD action, ignore
                switch (action) {
                    case NEURON_CREATE:
                        log.info("processing remote create: {}", neuron.getName());
                        annotationModel.getNeuronModel().addNeuron(neuron);
                        updateFilter(neuron, action);
                        annotationModel.fireNeuronCreated(neuron);
                        break;
                    case NEURON_SAVE_NEURONDATA:
                        log.info("processing remote save: {},", neuron.getName());
                        annotationModel.getNeuronModel().addNeuron(neuron);
                        updateFilter(neuron, action);
                        annotationModel.fireNeuronChanged(neuron);
                        break;
                    case NEURON_DELETE:
                        log.info("processing remote delete: {},", neuron.getName());
                        updateFilter(neuron, action);
                        annotationModel.fireNeuronDeleted(neuron);
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

    private void addNeuronUpdate(Map<String, Object> msgHeaders, byte[] msgBody, NeuronMessageConstants.MessageType action, String user) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TmNeuronMetadata neuron = mapper.readValue(msgBody, TmNeuronMetadata.class);

            // assume this has to do with neuron CRUD; otherwise ignore
            Map neuronData = new HashMap<>();
            neuronData.put("neuron", neuron);
            neuronData.put("action", action);
            neuronData.put("user", user);
            log.info("Adding neuron remote update: {}", neuron.getName());
            if (neuron != null && neuron.getId() != null) {
                this.updatesMap.put(neuron.getId(), neuronData);
            }
        } catch (Exception e) {
            // any type of exception, log the exception and dump the neuron
            log.info("Problem storing update {}", e.getMessage());
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
            log.debug("Thread Count: {}", ManagementFactory.getThreadMXBean().getThreadCount());
            log.debug("Heap Size: {}", Runtime.getRuntime().totalMemory());

            log.debug("message properties: TYPE={},USER={},WORKSPACE={}",
                    msgHeaders.get(NeuronMessageConstants.Headers.TYPE),
                    msgHeaders.get(NeuronMessageConstants.Headers.USER),
                    msgHeaders.get(NeuronMessageConstants.Headers.WORKSPACE));

            NeuronMessageConstants.MessageType action = NeuronMessageConstants.MessageType.valueOf(MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.TYPE));
            String user = MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.USER);

            Long workspace = MessagingUtils.getHeaderAsLong(msgHeaders, NeuronMessageConstants.Headers.WORKSPACE);

            // flag to suppress shared updates
            if (!receiveUpdates && !freezeUpdates && !user.equals(AccessManager.getSubjectKey())) {
                if (workspace != null && annotationModel != null && modelManager.getCurrentWorkspace() != null
                        && workspace.longValue() == modelManager.getCurrentWorkspace().getId().longValue()) {
                    addNeuronUpdate(msgHeaders, msgBody, action, user);
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

            ObjectMapper mapper = new ObjectMapper();
            TmNeuronMetadata neuron = mapper.readValue(msgBody, TmNeuronMetadata.class);

            TmNeuronMetadata localNeuron = annotationModel.getNeuronModel().getNeuronById(neuron.getId());// decrease the sync level
            if (localNeuron != null) {
                localNeuron.decrementSyncLevel();
                if (localNeuron.getSyncLevel() == 0) {
                    localNeuron.setSynced(true);
                }
            }

            log.info("Processed headers for workspace Id {}", workspace);
            // if not this workspace or user isn't looking at a workspace right now or workspace not relating to a workspace update, filter out message

            log.debug("PRE-WORKSPACE TIME: {}", stopWatch.getElapsedTime());
            if (workspace == null || modelManager.getCurrentWorkspace() == null
                    || workspace.longValue() != modelManager.getCurrentWorkspace().getId().longValue()) {
                return;
            }

            if (action == NeuronMessageConstants.MessageType.NEURON_OWNERSHIP_DECISION) {
                boolean decision = MessagingUtils.getHeaderAsBoolean(msgHeaders, NeuronMessageConstants.Headers.DECISION);
                if (decision) {
                    TmNeuronMetadata origNeuron = annotationModel.getNeuronModel().getNeuronById(neuron.getId());
                    if (origNeuron != null) {
                        origNeuron.setOwnerKey(neuron.getOwnerKey());
                        origNeuron.setWriters(neuron.getWriters());
                        origNeuron.setReaders(neuron.getReaders());
                    }
                }
                annotationModel.getNeuronModel().completeOwnershipRequest(decision);
                updateFilter(neuron, action);
                SwingUtilities.invokeLater(() -> {
                    StopWatch stopWatch2 = new StopWatch();
                    annotationModel.fireNeuronsOwnerChanged(neuron);
                    stopWatch2.stop();
                    log.info("RefreshHandler.invokeLater: handled ownership decision update in {} ms", stopWatch2.getElapsedTime());
                });
            } else if (action == NeuronMessageConstants.MessageType.NEURON_CREATE && user.equals(AccessManager.getSubjectKey())) {
                // complete the future outside of the swing thread, since the copyGUI thread is blocked
                StopWatch stopWatch2 = new StopWatch();
                handleNeuronCreate(neuron, n -> annotationModel.getNeuronModel().completeCreateNeuron(n));
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
                            // fire notice to NeuronManager
                            // change relevant to this workspace and not executed on this client,
                            // update model or process request
                            switch (action) {
                                case NEURON_CREATE:
                                    handleNeuronCreate(neuron, n -> annotationModel.getNeuronModel().addNeuron(n));
                                    break;
                                case NEURON_SAVE_NEURONDATA:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        if (annotationModel.getNeuronModel().getNeuronById(neuron.getId())==null) {
                                            annotationModel.getNeuronModel().addNeuron(neuron);
                                            updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_CREATE);
                                            annotationModel.fireNeuronCreated(neuron);
                                        } else
                                            handleNeuronChanged(neuron);
                                    }
                                    break;
                                case NEURON_DELETE:
                                    if (!user.equals(AccessManager.getSubjectKey())) {
                                        handleNeuronDeleted(neuron);
                                         
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

    private void handleNeuronCreate(TmNeuronMetadata neuron, Consumer<TmNeuronMetadata> neuronAction) {
        try {
            log.info("remote processing create neuron " + neuron.getName());
            neuronAction.accept(neuron);
			updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_CREATE);
            annotationModel.fireNeuronCreated(neuron);
        } catch (Exception e) {
            logError("Error handling neuron creation: " + e.getMessage());
            log.error("Error handling neuron creation message for {}", neuron, e);
        }
    }

    private void handleNeuronChanged(TmNeuronMetadata neuron) {
        try {
            log.info("remote processing change neuron " + neuron.getName());
            if (!ApplicationPanel.isDisableSharedWorkspace()) {
                annotationModel.getNeuronModel().refreshNeuronFromShared(neuron);
                annotationModel.refreshNeuron(neuron);
            }
        } catch (Exception e) {
            logError("Error handling neuron change: " + e.getMessage());
            log.error("Error handling neuron changed message for {}", neuron, e);
        }
    }

    private void handleNeuronDeleted(TmNeuronMetadata neuron) {
        try {
            log.info("remote processing delete neuron" + neuron.getName());
            if (!ApplicationPanel.isDisableSharedWorkspace()) {
                updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_DELETE);
                annotationModel.fireNeuronDeleted(neuron);
            }
        } catch (Exception e) {
            logError("Error handling neuron change: " + e.getMessage());
            log.error("Error handling neuron changed message for {}", neuron, e);
        }
    }
	
    public void updateFilter(TmNeuronMetadata neuron, NeuronMessageConstants.MessageType action) {
        annotationModel.updateNeuronFilter(neuron, action);
    }

    public void logError(String errorMsg) {
        String error = "Problems receiving message updates, " + errorMsg;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               // AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
             //   annotationMgr.presentError(errorMsg, new RuntimeException(error));
            }
        });
        log.error(error);
    }

    @Override
    public void cancelMessage(String routingTag) {
        log.info("The messaging system might have canceled the consumer");
    }

    public NeuronManager getAnnotationModel() {
        return annotationModel;
    }

    public void setAnnotationModel(NeuronManager annotationModel) {
        this.annotationModel = annotationModel;
    }


}
