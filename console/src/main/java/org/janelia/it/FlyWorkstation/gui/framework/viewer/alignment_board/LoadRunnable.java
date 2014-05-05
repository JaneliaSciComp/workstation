package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.jacs.shared.loader.renderable.MaskChanRenderableData;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class LoadRunnable implements Runnable {
    private MaskChanRenderableData metaData;
    private CyclicBarrier barrier;
    private RenderableDataLoader renderableDataLoader;

    public LoadRunnable(
            MaskChanRenderableData signalRenderable,
            RenderableDataLoader renderableDataLoader,
            CyclicBarrier barrier
    ) {
        this.metaData = signalRenderable;
        this.renderableDataLoader = renderableDataLoader;
        this.barrier = barrier;
    }

    public void run() {
        try {
            renderableDataLoader.loadRenderableData(metaData);
            //System.out.println( "Finished loading " + metaData.getMaskPath() );
        } catch ( Exception ex ) {
            ex.printStackTrace();
            if ( barrier != null )
                barrier.reset();   // This tells others that the barrier is broken.
            throw new RuntimeException( ex );
        }

        try {
            if ( barrier != null )
                barrier.await();
        } catch ( BrokenBarrierException bbe ) {
            bbe.printStackTrace();
        } catch ( InterruptedException ie ) {
            ie.printStackTrace();
        }
    }
}

