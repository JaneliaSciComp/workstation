package org.janelia.it.FlyWorkstation.tracing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TextureCache;
import org.janelia.it.FlyWorkstation.signal.Signal1;

public class PathTracer {
    private ThreadPoolExecutor pathTraceExecutor;

    public Signal1<TracedPathSegment> pathTracedSignal = new Signal1<TracedPathSegment>();
    
    public PathTracer(int threadPoolSize) {
        pathTraceExecutor = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }
    
    public void tracePathAsynchronous(TextureCache textureCache, 
            SharedVolumeImage volume, 
            PathTraceRequest request) 
    {
        PathTraceWorker worker = new PathTraceWorker(textureCache, volume, request);
        worker.pathTracedSignal.connect(pathTracedSignal);
        pathTraceExecutor.submit(worker);
    }

}
