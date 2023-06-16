package org.janelia.workstation.controller.model.annotations.neuron;

import com.rabbitmq.client.UnblockedCallback;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private static final boolean USE_NEURON_WORK_QUEUE = ApplicationPanel.isUseNeuronQueue();
    // Run serial neuron persistence every N milliseconds
    private static final int WORK_DELAY_MILLIS = 2000;
    // How long to wait before processing a neuron operation
    private static final int WORK_ITEM_DELAY = 200;
    // How many neurons to process every interval
    private static final int MAX_BATCH_SIZE = 1000;

    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private DedupedDelayQueue<ThrowingLambda> neuronPersistQueue;
    private ScheduledExecutorService executor;

    public NeuronModelAdapter() {


        this.neuronPersistQueue = new DedupedDelayQueue<ThrowingLambda>() {
            {
                setWorkItemDelay(WORK_ITEM_DELAY);
            }

            @Override
            void processList(List<ThrowingLambda> workItems) {
                LOG.info("Executing {} neuron operations", workItems.size());
                for (ThrowingLambda lambda : workItems) {
                    try {
                        lambda.accept();
                    }
                    catch (Exception e) {
                        FrameworkAccess.handleException("Error executing neuron operation", e);
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

    CompletableFuture<TmNeuronMetadata> createNeuronAsync(TmNeuronMetadata neuron) throws Exception {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        if (USE_NEURON_WORK_QUEUE) {
            neuronPersistQueue.addWorkItem(() -> {
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
            neuronPersistQueue.addWorkItem(() -> {
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
            neuronPersistQueue.addWorkItem(() -> {
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
            neuronPersistQueue.addWorkItem(() -> {
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
