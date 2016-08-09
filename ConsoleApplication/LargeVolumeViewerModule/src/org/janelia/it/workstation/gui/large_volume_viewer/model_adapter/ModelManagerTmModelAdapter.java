/**
 * Implementation of the model adapter, which pulls/pushes data through
 * the Model Manager.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.model_adapter;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Stopwatch;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When invoked to fetch exchange neurons, this implementation will do so
 * by way of the model manager.  It adapts the model manipulator for 
 * client-side use.  Not intended to be used from multiple threads, which are
 * feeding different workspaces.
 *
 * @author fosterl
 */
public class ModelManagerTmModelAdapter implements TmModelAdapter {

    private static Logger log = LoggerFactory.getLogger(ModelManagerTmModelAdapter.class);

    // Very large initial work unit estimate; just need to work out the
    // conversion factor between this and the true number, once that is
    // learned.
    public static final int TOTAL_WORKUNITS = 3;

    private static ScheduledThreadPoolExecutor saveQueue=new ScheduledThreadPoolExecutor(1, new CustomNamedThreadFactory("Tm-Save-Queue"));
    private static boolean saveQueueErrorFlag=false;

    private TiledMicroscopeDomainMgr tmDomainMgr;

    @Override
    public List<TmNeuron> loadNeurons(TmWorkspace workspace) throws Exception {
        this.tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
        List<TmNeuron> neurons;
        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Loading annotations...");

        try {
            progressHandle.start(TOTAL_WORKUNITS);
            progressHandle.setInitialDelay(0);            
            progressHandle.progress(0);
            progressHandle.setDisplayName("Loading neuron data...");
            neurons = tmDomainMgr.getWorkspaceNeurons(workspace.getId());

            progressHandle.progress(1);

            progressHandle.setDisplayName("Building neurons...");

            progressHandle.progress(2);

            // Await completion.
            log.info("Neuron exchange complete.");
            progressHandle.setDisplayName("Populating viewer...");
            progressHandle.progress(3);

            // check neuron consistency and repair (some) problems
            for (TmNeuron neuron: neurons) {
                log.info("Checking neuron "+neuron);
                List<String> results = neuron.checkRepairNeuron();
                // List<String> results = neuron.checkNeuron();
                if (results.size() > 0) {
                    // save results, then output to log; this is unfortunately
                    //  not visible to the user; we aren't in a place in the
                    //  code where we can pop a dialog
                    saveNeuron(neuron);
                    for (String s: results) {
                        log.warn(s);
                    }
                }
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
        private TmNeuron neuron;
        private AtomicBoolean running = new AtomicBoolean(false);
        private TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();

        public SaveNeuronRunnable(TmNeuron neuron) {
            this.neuron=neuron;
        }
        
        public TmNeuron getNeuron() {
            return neuron;
        }
        
        public boolean isRunning() {
            return running.get();
        }

        @Override
        public void run() {
            running.set(true);
            try {
                Stopwatch w = new Stopwatch();
                tmDomainMgr.save(neuron);
                log.info("Neuron save/update time = " + w.elapsedMillis() + " ms");
            }
            catch (Exception ex) {
                log.error("Error saving neuron",ex);
                saveQueueErrorFlag=true;
            }
            running.set(false); // For completeness.
        }
    }

    private static void saveNeuronToQueue(TmNeuron neuron) throws Exception {
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
            SaveNeuronRunnable saveNeuronRunnable = new SaveNeuronRunnable(neuron);
            saveQueue.submit(saveNeuronRunnable);
        }
    }

    /**
     * Pushes neuron as entity data, to database.
     * 
     * @param neuron internal-to-LVV object.
     * @throws Exception 
     */
    @Override
    public void saveNeuron(TmNeuron neuron) throws Exception {
        if (neuron.getId()==null) {
            // New neuron, do not use queue
            SaveNeuronRunnable saveNeuronRunnable=new SaveNeuronRunnable(neuron);
            saveNeuronRunnable.run();
        }
        else {
            saveNeuronToQueue(neuron);
        }
    }

    @Override
    public void deleteNeuron(TmNeuron neuron) throws Exception {
        tmDomainMgr.remove(neuron);
    }
}
