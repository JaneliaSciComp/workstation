package org.janelia.workstation.controller.model.annotations.neuron;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

    private static Logger LOG = LoggerFactory.getLogger(NeuronModelAdapter.class);

    private static final int MAX_NEURONS = 1000000;
    // Use async work queue to process neuron updates
    private static final boolean USE_NEURON_WORK_QUEUE = true;
    // Run serial neuron persistence every N milliseconds
    private static final int WORK_DELAY_MILLIS = 2000;
    // How long to wait before processing a neuron operation
    private static final int WORK_ITEM_DELAY = 200;
    // How many neurons to process every interval
    private static final int MAX_BATCH_SIZE = 1000;

    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private DedupedDelayQueue<TmNeuronMetadata> neuronPersistQueue;
    private ScheduledExecutorService executor;

    public NeuronModelAdapter() {
        this.neuronPersistQueue = new DedupedDelayQueue<TmNeuronMetadata>() {
            {
                setWorkItemDelay(WORK_ITEM_DELAY);
            }

            @Override
            void processList(List<TmNeuronMetadata> workItems) {
                LOG.info("Executing {} neuron operations", workItems.size());
                for (TmNeuronMetadata neuron : workItems) {
                    try {
                        tmDomainMgr.updateNeuron(neuron);
                    }
                    catch (Exception e) {
                        FrameworkAccess.handleException("Error saving neuron", e);
                    }
                }
            }
        };

        Runnable queueProcessTask = () -> neuronPersistQueue.process(MAX_BATCH_SIZE);
        this.executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(queueProcessTask, 0, WORK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    Stream<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) {
        LOG.info("Loading neurons for workspace: {}", workspace);
        StopWatch stopWatch = new StopWatch();
        try {
            return tmDomainMgr.streamWorkspaceNeurons(workspace.getId())
                    .limit(MAX_NEURONS)
                    .map(neuron -> {
                        // make sure to initialize cross references
                        neuron.initNeuronData();
                        return neuron;
                    })
                    ;
        } finally {
            LOG.info("Getting neurons stream took {} ms", stopWatch.getElapsedTime());
        }
    }

    TmNeuronMetadata createNeuron(TmNeuronMetadata neuron) throws Exception {
        return tmDomainMgr.createNeuron(neuron);
    }

    void saveNeuron(TmNeuronMetadata neuron) throws Exception {
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.addWorkItem(neuron);
        }
        else {
            tmDomainMgr.updateNeuron(neuron);
        }
    }

    TmNeuronMetadata changeOwnership(TmNeuronMetadata neuron, String targetUser) throws Exception {
        return tmDomainMgr.changeOwnership(neuron, targetUser);
    }

    void removeNeuron(TmNeuronMetadata neuron) throws Exception {
        tmDomainMgr.removeNeuron(neuron);
    }
}
