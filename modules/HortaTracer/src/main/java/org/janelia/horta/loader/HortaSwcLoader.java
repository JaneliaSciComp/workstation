package org.janelia.horta.loader;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.janelia.horta.render.NeuronMPRenderer;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class HortaSwcLoader implements FileTypeLoader
{
    // We sometimes want to quickly load so many neuron models,
    // So load them asynchronously in new threads.
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    
    private final NeuronSet neuronSet;
    private final NeuronMPRenderer renderer;
    
    public HortaSwcLoader(NeuronSet neuronSet, NeuronMPRenderer renderer) {
        this.neuronSet = neuronSet;
        this.renderer = renderer;
    }
    
    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("SWC"))
            return true;
        return false;
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException
    {
        pool.submit(new Runnable() {
            @Override
            public void run()
            {
                try {
                    NeuronModel neuron = new BasicNeuronModel(source.openInputStream(), source.getFileName(), neuronSet);
                    synchronized(neuronSet) {
                        if (neuronSet.add(neuron))
                            neuronSet.getMembershipChangeObservable().setChanged();
                    }
                    //renderer.addNeuronActors(neuron);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        return true;
    }

    public void runAfterLoad(final Runnable onComplete)
    {
        final ExecutorService closedPool = pool;
        synchronized(this) {
            pool = Executors.newFixedThreadPool(10);
        }
        closedPool.shutdown();
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run()
            {
                try {
                    closedPool.awaitTermination(120, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                onComplete.run();
            }
            
        });
        thread.start();
    }
    
}
