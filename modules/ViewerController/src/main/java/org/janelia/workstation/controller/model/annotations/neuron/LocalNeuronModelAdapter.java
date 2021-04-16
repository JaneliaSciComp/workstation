package org.janelia.workstation.controller.model.annotations.neuron;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.messaging.core.ConnectionManager;
import org.janelia.messaging.core.MessageConnection;
import org.janelia.messaging.core.MessageSender;
import org.janelia.messaging.core.impl.MessageSenderImpl;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgrFactory;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronMessageConstants;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

class LocalNeuronModelAdapter implements NeuronModelAdapter {
    private static Logger LOG = LoggerFactory.getLogger(NeuronModelAdapter.class);
    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgrFactory.getDomainMgr();

    @Override
    public Stream<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) {
        LOG.info("Loading neurons for workspace: {}", workspace);
        StopWatch stopWatch = new StopWatch();
        try {
            return tmDomainMgr.streamWorkspaceNeurons(workspace.getId())
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

    @Override
    public CompletableFuture<TmNeuronMetadata> asyncCreateNeuron(TmNeuronMetadata neuron) throws Exception {
        // make sure the neuron contains the current user's ownerKey;
        neuron.setOwnerKey(AccessManager.getSubjectKey());
        return new CompletableFuture<>();
    }

    @Override
    public void asyncSaveNeuron(TmNeuronMetadata neuron, Map<String, String> extraArgs) throws Exception {
    }

    @Override
    public void asyncDeleteNeuron(TmNeuronMetadata neuron) throws Exception {
    }

    @Override
    public CompletableFuture<Boolean> requestOwnership(TmNeuronMetadata neuron) throws Exception {
        return new CompletableFuture<>();
    }

    @Override
    public void requestAssignment(TmNeuronMetadata neuron, String targetUser) throws Exception {
    }
}
