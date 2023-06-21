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
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.management.ManagementFactory;
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
    private static final String MESSAGESERVER_EXCHANGE_REFRESH = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.refresh").trim();

    private NeuronManager neuronManager;
    static RefreshHandler handler;
    private boolean receiveUpdates = false;
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
                            FrameworkAccess.handleExceptionQuietly(
                                    "Problems initializing connection to message server "+
                                            MESSAGESERVER_URL+" with username "+
                                            MESSAGESERVER_USERACCOUNT, exc);
                        });
        AsyncMessageConsumer msgReceiver = new AsyncMessageConsumerImpl(messageConnection);
        ((AsyncMessageConsumerImpl) msgReceiver).setAutoAck(true);
        // create a temporary binding to ModelRefresh exchange and listen on that channel for the replies
        msgReceiver.bindAndConnectTo(MESSAGESERVER_EXCHANGE_REFRESH, "", null);
        msgReceiver.subscribe(this);
        log.info("Established connection to message server {}", MESSAGESERVER_URL);
        return messageConnection.isOpen();
    }

    /**
     * Successful refresh update received
     */
    @Override
    public void handleMessage(Map<String, Object> msgHeaders, byte[] msgBody) {
        try {
            StopWatch stopWatch = new StopWatch();

            if (msgHeaders == null) {
                log.warn("Encountered null message headers when handling refresh message");
                return;
            }

            // thread logging
            log.trace("Thread Count: {}", ManagementFactory.getThreadMXBean().getThreadCount());
            log.trace("Heap Size: {}", Runtime.getRuntime().totalMemory());

            log.debug("message properties: TYPE={},USER={},WORKSPACE={}",
                    msgHeaders.get(NeuronMessageConstants.Headers.TYPE),
                    msgHeaders.get(NeuronMessageConstants.Headers.USER),
                    msgHeaders.get(NeuronMessageConstants.Headers.WORKSPACE));

            NeuronMessageConstants.MessageType action = NeuronMessageConstants.MessageType.valueOf(
                    MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.TYPE));
            String user = MessagingUtils.getHeaderAsString(msgHeaders, NeuronMessageConstants.Headers.USER);
            Long workspace = MessagingUtils.getHeaderAsLong(msgHeaders, NeuronMessageConstants.Headers.WORKSPACE);

            // Ignore our own updates
            if (user.equals(AccessManager.getSubjectKey())) return;

            // Ignore updates if user has disabled shared workspaces
            if (ApplicationPanel.isDisableSharedWorkspace()) {
                return;
            }

            // flag to suppress shared updates
            if (!receiveUpdates && !user.equals(AccessManager.getSubjectKey())) {
                return;
            }

            if (action == NeuronMessageConstants.MessageType.ERROR_PROCESSING) {
                if (user.equals(AccessManager.getSubjectKey())) {
                    log.warn("Error message received from server: {}", new String(msgBody));
                }
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            TmNeuronMetadata neuron = mapper.readValue(msgBody, TmNeuronMetadata.class);

            log.debug("Processed headers for workspace Id {}", workspace);
            // if not this workspace or user isn't looking at a workspace right now or workspace not relating to a workspace update, filter out message

            log.debug("PRE-WORKSPACE TIME: {}", stopWatch.getElapsedTime());
            if (workspace == null || modelManager.getCurrentWorkspace() == null
                    || workspace.longValue() != modelManager.getCurrentWorkspace().getId().longValue()) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    StopWatch stopWatch2 = new StopWatch();
                    // fire notice to NeuronManager
                    // change relevant to this workspace and not executed on this client,
                    // update model or process request
                    switch (action) {
                        case NEURON_CREATE:
                            handleNeuronCreate(user, neuron, n -> neuronManager.getNeuronModel().addNeuron(n));
                            break;
                        case NEURON_SAVE_NEURONDATA:
                            if (neuronManager.getNeuronModel().getNeuronById(neuron.getId())==null) {
                                // We don't know about this neuron yet
                                neuronManager.getNeuronModel().addNeuron(neuron);
                                updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_CREATE);
                                neuronManager.fireNeuronCreated(neuron);
                            } else {
                                handleNeuronChanged(user, neuron);
                            }
                            break;
                        case NEURON_DELETE:
                            handleNeuronDeleted(user, neuron);
                            break;
                        case REQUEST_NEURON_ASSIGNMENT:
                            handleNeuronAssignment(user, neuron);
                            break;
                    }
                    stopWatch2.stop();
                    log.debug("RefreshHandler.invokeLater: handled update in {} ms", stopWatch2.getElapsedTime());
                } catch (Exception e) {
                    FrameworkAccess.handleExceptionQuietly("Error handling refresh message: " + e.getMessage(), e);
                }
                log.debug("TOTAL MESSAGING PROCESSING TIME: {}", stopWatch.getElapsedTime());
            });
            stopWatch.stop();
            log.debug("RefreshHandler: handled message in {} ms", stopWatch.getElapsedTime());
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly("Error handling refresh message: " + e.getMessage(), e);
        }
    }

    private void handleNeuronCreate(String user, TmNeuronMetadata neuron, Consumer<TmNeuronMetadata> neuronAction) {
        try {
            log.info("Processing neuron '{}' ({}) remotely created by {}", neuron.getName(), neuron.getId(), user);
            neuronAction.accept(neuron);
			updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_CREATE);
            neuronManager.fireNeuronCreated(neuron);
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly("Error handling neuron creation: " + e.getMessage(), e);
        }
    }

    private void handleNeuronChanged(String user, TmNeuronMetadata neuron) {
        try {
            log.info("Processing neuron '{}' ({}) remotely modified by {}", neuron.getName(), neuron.getId(), user);
            neuronManager.getNeuronModel().refreshNeuronFromShared(neuron);
            neuronManager.refreshNeuron(neuron);
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly("Error handling neuron change: " + e.getMessage(), e);
        }
    }

    private void handleNeuronDeleted(String user, TmNeuronMetadata neuron) {
        try {
            log.info("Processing neuron '{}' ({}) remotely deleted by {}", neuron.getName(), neuron.getId(), user);
            updateFilter(neuron, NeuronMessageConstants.MessageType.NEURON_DELETE);
            neuronManager.fireNeuronDeleted(neuron);
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly("Error handling neuron deletion: " + e.getMessage(), e);
        }
    }

    private void handleNeuronAssignment(String user, TmNeuronMetadata neuron) {
        try {
            log.info("Processing neuron '{}' ({}) remotely reassigned by {}", neuron.getName(), neuron.getId(), user);
            updateFilter(neuron, NeuronMessageConstants.MessageType.REQUEST_NEURON_ASSIGNMENT);
            neuronManager.fireNeuronChanged(neuron);
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly("Error handling neuron deletion: " + e.getMessage(), e);
        }
    }

    public void updateFilter(TmNeuronMetadata neuron, NeuronMessageConstants.MessageType action) {
        neuronManager.updateNeuronFilter(neuron, action);
    }

    @Override
    public void cancelMessage(String routingTag) {
        log.info("The messaging system might have canceled the consumer");
    }

    public NeuronManager getAnnotationModel() {
        return neuronManager;
    }

    public void setAnnotationModel(NeuronManager annotationModel) {
        this.neuronManager = annotationModel;
    }
}
