package org.janelia.it.workstation.gui.large_volume_viewer.model_adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the TiledMicroscopeDomainMgr.
 *
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for 
 * client-side use.  Not intended to be used from multiple threads, which are
 * feeding different workspaces.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgrTmModelAdapter implements TmModelAdapter {

    private static Logger log = LoggerFactory.getLogger(DomainMgrTmModelAdapter.class);

    // Very large initial work unit estimate; just need to work out the
    // conversion factor between this and the true number, once that is
    // learned.
    public static final int TOTAL_WORKUNITS = 3;

    private static ScheduledThreadPoolExecutor saveQueue = new ScheduledThreadPoolExecutor(1, new CustomNamedThreadFactory("Tm-Save-Queue"));
    private static boolean saveQueueErrorFlag = false;

    private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();

    @Override
    public List<TmNeuronMetadata> loadNeurons(TmWorkspace workspace) throws Exception {
        log.info("Checking neurons for workspace: "+workspace);
        List<TmNeuronMetadata> neurons = new ArrayList<>();
        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Loading annotations...");

        try {
            progressHandle.setInitialDelay(0);
            progressHandle.start(TOTAL_WORKUNITS);            
            progressHandle.progress(0);
            progressHandle.setDisplayName("Loading neuron data...");
            
            StopWatch stopWatch = new StopWatch();
            List<TmNeuronMetadata> neuronList = tmDomainMgr.getWorkspaceNeurons(workspace.getId());
            log.info("Loading {} neurons took {} ms", neuronList.size(), stopWatch.getElapsedTime());
            
            progressHandle.progress(1);

            // Await completion.
            progressHandle.setDisplayName("Populating viewer...");
            progressHandle.progress(3);

            // check neuron consistency and repair (some) problems
            for (TmNeuronMetadata neuron: neuronList) {
                log.debug("Checking neuron data for TmNeuronMetadata#{}", neuron.getId());
                List<String> results = neuron.checkRepairNeuron();
                // List<String> results = neuron.checkNeuron();
                if (results.size() > 0) {
                    // save results, then output to log; this is unfortunately
                    //  not visible to the user; we aren't in a place in the
                    //  code where we can pop a dialog
                    for (String s: results) {
                        log.warn(s);
                    }
                	neuron = tmDomainMgr.save(neuron);
                }
                neurons.add(neuron);
            }

            progressHandle.finish();
        }
        catch (Exception ex) {
            progressHandle.finish();
            throw ex;
        }

        return neurons;
    }

    private static class SaveNeuronRunnable implements Runnable {

        private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
        private TmNeuronMetadata tmNeuronMetadata;
        private boolean metadataOnly = false;
        private AtomicBoolean running = new AtomicBoolean(false);

        public SaveNeuronRunnable(TmNeuronMetadata tmNeuronMetadata, boolean metadataOnly) {
            this.tmNeuronMetadata = tmNeuronMetadata;
            this.metadataOnly = metadataOnly;
        }

        @Override
        public void run() {
            running.set(true);
            try {
                StopWatch w = new StopWatch();
                if (metadataOnly) {
                    tmDomainMgr.saveMetadata(tmNeuronMetadata);
                }
                else {
                    tmDomainMgr.save(tmNeuronMetadata);
                }
                log.trace("Neuron save/update time = {} ms", w.getElapsedTime());
            }
            catch (Exception ex) {
                log.error("Error saving neuron",ex);
                saveQueueErrorFlag=true;
            }
            running.set(false); // For completeness.
        }
    }

    private static void saveNeuronToQueue(TmNeuronMetadata neuron, boolean metadataOnly) throws Exception {
        if (saveQueueErrorFlag) {
            throw new Exception("Neuron save queue in error state - workstation should be restarted");
        } else {
            // Save-queue tasks are identical to one another.  So let us
            // not accumulate redundant tasks.  If one is waiting to save
            // this neuron, no need to schedule another, which will just
            // save it again after--and possibly after other operations.


            // this doesn't work right now; keeping it in because I hope to fix it
            /*
            for (Runnable runnable: saveQueue.getQueue()) {
                SaveNeuronRunnable snr = (SaveNeuronRunnable)runnable;
                if (snr.sameNeuron(neuron)  &&  ! snr.isRunning()) {
                    // There is a yet-to-run task focused on saving this neuron.
                    return;
                }
            }
            */
            SaveNeuronRunnable saveNeuronRunnable = new SaveNeuronRunnable(neuron, metadataOnly);
            saveQueue.submit(saveNeuronRunnable);
        }
    }

    public TmNeuronMetadata createNeuron(TmNeuronMetadata tmNeuronMetadata) throws Exception {
        return tmDomainMgr.save(tmNeuronMetadata);
    }

    /**
     * Pushes neuron as entity data, to database.
     * 
     * @param tmNeuronMetadata internal-to-LVV object.
     * @throws Exception 
     */
    @Override
    public void saveNeuron(TmNeuronMetadata tmNeuronMetadata) throws Exception {
        saveNeuronToQueue(tmNeuronMetadata, false);
    }

    @Override
    public void saveNeuronMetadata(TmNeuronMetadata tmNeuronMetadata) throws Exception {
        saveNeuronToQueue(tmNeuronMetadata, true);
    }

    @Override
    public void deleteNeuron(TmNeuronMetadata tmNeuronMetadata) throws Exception {
        tmDomainMgr.remove(tmNeuronMetadata);
    }
}
