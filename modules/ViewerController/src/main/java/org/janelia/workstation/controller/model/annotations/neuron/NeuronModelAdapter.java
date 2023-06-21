package org.janelia.workstation.controller.model.annotations.neuron;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.controller.util.QueuedWorkThread;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the TiledMicroscopeDomainMgr.
 *
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for
 * client-side use.
 *
 * The behavior of this class depends on the "UseNeuronQueue" option that is configurable by the user. If this
 * option is enabled, then all actions are asynchronous (managed by a local queue that serializes operations).
 * Otherwise, actions are taken synchronously.
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
    // Alert user if queue grows larger than
    private static final int ALERT_QUEUE_SIZE = 10;

    private final TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    private final BlockingQueue<Runnable> neuronPersistQueue;
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

    public Stream<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) {
        LOG.info("Loading neurons for workspace: {}", workspace);
        StopWatch stopWatch = new StopWatch();
        try {
            return tmDomainMgr.streamWorkspaceNeurons(workspace.getId())
                    .limit(MAX_NEURONS)
                    .map(neuron -> {
                        // make sure to initialize cross-references
                        neuron.initNeuronData();
                        return neuron;
                    })
                    ;
        } finally {
            LOG.info("Getting neurons stream took {} ms", stopWatch.getElapsedTime());
        }
    }

    public CompletableFuture<TmNeuronMetadata> createNeuron(TmNeuronMetadata neuron) {
        Callable<TmNeuronMetadata> action = () -> tmDomainMgr.createNeuron(neuron);
        return USE_NEURON_WORK_QUEUE ? getFuture(action) : getPresent(action);
    }

    public CompletableFuture<TmNeuronMetadata> saveNeuron(TmNeuronMetadata neuron) {
        Callable<TmNeuronMetadata> action = () -> tmDomainMgr.updateNeuron(neuron);
        return USE_NEURON_WORK_QUEUE ? getFuture(action) : getPresent(action);
    }

    public CompletableFuture<TmNeuronMetadata> changeOwnership(TmNeuronMetadata neuron, String targetOwner) {
        Callable<TmNeuronMetadata> action = () -> tmDomainMgr.changeOwnership(neuron, targetOwner);
        return USE_NEURON_WORK_QUEUE ? getFuture(action) : getPresent(action);
    }

    public CompletableFuture<TmNeuronMetadata> removeNeuron(TmNeuronMetadata neuron) {
        Callable<TmNeuronMetadata> action = () -> tmDomainMgr.removeNeuron(neuron);
        return USE_NEURON_WORK_QUEUE ? getFuture(action) : getPresent(action);
    }

    /**
     * Call the given producer synchronously and return a completed CompletebleFuture.
     * This is here so that the interface of the methods looks the same whether or not
     * we use futures.
     * @param producer a lambda that produces a neuron
     * @return the future neuron
     */
    private CompletableFuture<TmNeuronMetadata> getPresent(Callable<TmNeuronMetadata> producer) {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        try {
            TmNeuronMetadata updatedNeuron = producer.call();
            future.complete(updatedNeuron);
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * Add the given producer to the work queue and return a future value.
     * @param producer lambda that produces a neuron
     * @return the future neuron
     */
    private CompletableFuture<TmNeuronMetadata> getFuture(Callable<TmNeuronMetadata> producer) {
        CompletableFuture<TmNeuronMetadata> future = new CompletableFuture<>();
        neuronPersistQueue.add(() -> {
            try {
                TmNeuronMetadata updatedNeuron = producer.call();
                future.complete(updatedNeuron);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        if (neuronPersistQueue.size() > ALERT_QUEUE_SIZE) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "The system is currently having trouble saving your changes. " +
                            "There are "+neuronPersistQueue.size()+" actions in the queue.",
                    "Persistence issues detected",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return future;
    }
}
