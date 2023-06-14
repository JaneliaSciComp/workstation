package org.janelia.workstation.controller.model.annotations.neuron;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.janelia.messaging.core.ConnectionManager;
import org.janelia.messaging.core.MessageConnection;
import org.janelia.messaging.core.MessageSender;
import org.janelia.messaging.core.impl.MessageSenderImpl;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmHistory;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronMessageConstants;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
//import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the TiledMicroscopeDomainMgr.
 * <p>
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for
 * client-side use.  Not intended to be used from multiple threads, which are
 * feeding different workspaces.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class NeuronModelAdapter {

    private static final int MAX_NEURONS = 1000000;
    private static Logger LOG = LoggerFactory.getLogger(NeuronModelAdapter.class);

    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private MessageSender messageSender;

    Stream<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) {
        LOG.info("Loading neurons for workspace: {}", workspace);
        StopWatch stopWatch = new StopWatch();
        try {
            return tmDomainMgr.streamWorkspaceNeurons(workspace.getId())
                    .limit(MAX_NEURONS)
                    .map(neuron -> {
                        if (ClientDomainUtils.hasWriteAccess(workspace)) {
                          /*  if (ApplicationPanel.isVerifyNeurons()) {
                                LOG.info("Checking neuron data consistency");
                                // check neuron consistency and repair (some) problems
                                LOG.debug("Checking neuron data for TmNeuronMetadata#{}", neuron.getId());
                                List<String> results = neuron.checkRepairNeuron();
                                if (results.size() > 0) {
                                    // save results, then output to LOG; this is unfortunately
                                    //  not visible to the user; we aren't in a place in the
                                    //  code where we can pop a dialog
                                    for (String s : results) {
                                        LOG.warn(s);
                                    }
                                    try {
                                        return tmDomainMgr.save(neuron);
                                    } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                    }
                                }
                            }*/
                        }
                        // make sure to initialize cross references
                        neuron.initNeuronData();
                        return neuron;
                    })
                    ;
        } finally {
            LOG.info("Getting neurons stream took {} ms", stopWatch.getElapsedTime());
        }
    }

    private MessageSender getSender() {
        if (messageSender == null) {
            // get constants; these used to be static final in class, but (a) they are only used here, and (b) they
            //  seem to get initialized too early in the current refactor; it seems that at the point
            //  this class is created, the props haven't yet been loaded and everything will come back empty
            String MESSAGESERVER_URL = ConsoleProperties.getInstance().getProperty("domain.msgserver.url").trim();
            String MESSAGESERVER_USERACCOUNT = ConsoleProperties.getInstance().getProperty("domain.msgserver.useraccount").trim();
            String MESSAGESERVER_PASSWORD = ConsoleProperties.getInstance().getProperty("domain.msgserver.password").trim();
            String MESSAGESERVER_UPDATESEXCHANGE = ConsoleProperties.getInstance().getProperty("domain.msgserver.exchange.updates").trim();
            String MESSAGESERVER_ROUTINGKEY = ConsoleProperties.getInstance().getProperty("domain.msgserver.routingkey.updates").trim();
            MessageConnection messageConnection = ConnectionManager.getInstance()
                    .getConnection(MESSAGESERVER_URL, MESSAGESERVER_USERACCOUNT, MESSAGESERVER_PASSWORD, 0,
                            (e) -> {
                                if (e instanceof RuntimeException) {
                                    throw (RuntimeException)e;
                                } else {
                                    throw new IllegalStateException(e);
                                }
                            });
            MessageSender newMessageSender = new MessageSenderImpl(messageConnection);
            newMessageSender.connectTo(MESSAGESERVER_UPDATESEXCHANGE, MESSAGESERVER_ROUTINGKEY);
            messageSender = newMessageSender;
        }
        return messageSender;
    }

    private void sendMessage(TmNeuronMetadata neuron, NeuronMessageConstants.MessageType type, Map<String, String> extraArguments) throws Exception {
        // whatever the message is, unsync the object and increment the unsynced level counter
        neuron.setSynced(false);
        neuron.incrementSyncLevel();

        List<Long> neuronIds = new ArrayList<Long>();
        neuronIds.add(neuron.getId());
        ObjectMapper mapper = new ObjectMapper();
        byte[] neuronData = mapper.writeValueAsBytes(neuron);

        // add historical event
        TmHistoricalEvent event = new TmHistoricalEvent();
        Map<Long,byte[]> map = new HashMap<>();
        map.put(neuron.getId(), neuronData);
        event.setNeurons(map);
        event.setTimestamp(new Date());
        switch (type) {
            case NEURON_CREATE:
                event.setType(TmHistoricalEvent.EVENT_TYPE.NEURON_CREATE);
                break;
            case NEURON_DELETE:
                event.setType(TmHistoricalEvent.EVENT_TYPE.NEURON_DELETE);
                break;
            case NEURON_SAVE_NEURONDATA:
                event.setType(TmHistoricalEvent.EVENT_TYPE.NEURON_UPDATE);
                break;
        }
        if (extraArguments==null || !extraArguments.containsKey("undo") ||
                    !extraArguments.get("undo").equals("true")) {
            TmModelManager.getInstance().getNeuronHistory().addHistoricalEvent(event);
        }

        Map<String, Object> updateHeaders = new HashMap<String, Object>();
        updateHeaders.put(NeuronMessageConstants.Headers.TYPE, type.toString());
        updateHeaders.put(NeuronMessageConstants.Headers.USER, AccessManager.getSubjectKey());
        updateHeaders.put(NeuronMessageConstants.Headers.WORKSPACE, neuron.getWorkspaceId().toString());
        updateHeaders.put(NeuronMessageConstants.Headers.NEURONIDS, neuronIds.toString());
        if (extraArguments != null) {
            Iterator<String> extraKeys = extraArguments.keySet().iterator();
            while (extraKeys.hasNext()) {
                String extraKey = extraKeys.next();
                updateHeaders.put(extraKey, extraArguments.get(extraKey));
            }
        }

        getSender().sendMessage(updateHeaders, neuronData);
    }

    CompletableFuture<TmNeuronMetadata> asyncCreateNeuron(TmNeuronMetadata neuron) throws Exception {
        // make sure the neuron contains the current user's ownerKey;
        neuron.setOwnerKey(AccessManager.getSubjectKey());
        sendMessage(neuron, NeuronMessageConstants.MessageType.NEURON_CREATE, null);
        return new CompletableFuture<>();
    }

    void asyncSaveNeuron(TmNeuronMetadata neuron, Map<String, String> extraArgs) throws Exception {
        //sendMessage(neuron, NeuronMessageConstants.MessageType.NEURON_SAVE_NEURONDATA, extraArgs);
        tmDomainMgr.save(neuron);
    }

    void asyncDeleteNeuron(TmNeuronMetadata neuron) throws Exception {
        //sendMessage(neuron, NeuronMessageConstants.MessageType.NEURON_DELETE, null);
        tmDomainMgr.remove(neuron);
    }

    CompletableFuture<Boolean> requestOwnership(TmNeuronMetadata neuron) throws Exception {
        sendMessage(neuron, NeuronMessageConstants.MessageType.REQUEST_NEURON_OWNERSHIP, null);
        return new CompletableFuture<>();
    }

    void requestAssignment(TmNeuronMetadata neuron, String targetUser) throws Exception {
        Map<String, String> extraArgs = new HashMap<>();
        extraArgs.put(NeuronMessageConstants.Headers.TARGET_USER, targetUser);
        sendMessage(neuron, NeuronMessageConstants.MessageType.REQUEST_NEURON_ASSIGNMENT, extraArgs);
    }
}
