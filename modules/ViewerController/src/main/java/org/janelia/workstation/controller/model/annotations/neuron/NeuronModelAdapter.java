package org.janelia.workstation.controller.model.annotations.neuron;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.controller.util.QueuedWorkThread;
import org.janelia.workstation.controller.util.ThrowingLambda;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
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
    private static final boolean USE_NEURON_WORK_QUEUE = ApplicationPanel.isUseNeuronQueue();
    // How many neurons to process every interval
    private static final int MAX_BATCH_SIZE = 1000;

    private final TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private final BlockingQueue<ThrowingLambda> neuronPersistQueue;
    private final QueuedWorkThread workThread;

    public NeuronModelAdapter() {
        this.neuronPersistQueue = new ArrayBlockingQueue<>(MAX_BATCH_SIZE);
        this.workThread = new QueuedWorkThread(neuronPersistQueue, MAX_BATCH_SIZE) {
            public void handleException(Throwable e) {
                FrameworkAccess.handleException(e);
            }
        };
        workThread.start();
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

    CompletableFuture<TmNeuronMetadata> createNeuronAsync(TmNeuronMetadata neuron) throws Exception {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.add(() -> {
                future.complete(tmDomainMgr.createNeuron(neuron));
            });
        }
        else {
            future.complete(tmDomainMgr.createNeuron(neuron));
        }
        return future;
    }

    CompletableFuture<TmNeuronMetadata> saveNeuron(TmNeuronMetadata neuron) throws Exception {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.add(() -> {
                future.complete(tmDomainMgr.updateNeuron(neuron));
            });
        }
        else {
            future.complete(tmDomainMgr.updateNeuron(neuron));
        }
        return future;
    }

    CompletableFuture<TmNeuronMetadata> changeOwnership(TmNeuronMetadata neuron, String targetOwner) throws Exception {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.add(() -> {
                future.complete(tmDomainMgr.changeOwnership(neuron, targetOwner));
            });
        }
        else {
            future.complete(tmDomainMgr.changeOwnership(neuron, targetOwner));
        }
        return future;
    }

    CompletableFuture<TmNeuronMetadata> removeNeuron(TmNeuronMetadata neuron) throws Exception {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.add(() -> {
                tmDomainMgr.removeNeuron(neuron);
                future.complete(neuron);
            });
        }
        else {
            tmDomainMgr.removeNeuron(neuron);
            future.complete(neuron);
        }
        return future;
    }
}
