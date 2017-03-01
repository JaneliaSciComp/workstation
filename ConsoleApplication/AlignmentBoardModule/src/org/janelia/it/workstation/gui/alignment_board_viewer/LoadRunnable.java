package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class LoadRunnable implements Runnable {
    private MaskChanRenderableData metaData;
    private CyclicBarrier barrier;
    private VolumeLoader volumeLoader;

    public LoadRunnable(
            MaskChanRenderableData signalRenderable,
            VolumeLoader volumeLoader,
            CyclicBarrier barrier
    ) {
        this.metaData = signalRenderable;
        this.volumeLoader = volumeLoader;
        this.barrier = barrier;
    }

    public void run() {
        try {
            volumeLoader.loadVolume(metaData);
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
