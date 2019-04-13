package org.janelia.scenewindow.fps;

import java.util.Observable;
import org.janelia.console.viewerapi.ComposableObservable;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class FrameTracker {
    private ComposableObservable frameBeginObservable = new ComposableObservable();
    private ComposableObservable frameEndObservable = new ComposableObservable();
    
    public Observable getFrameBeginObservable()
    {
        return frameBeginObservable;
    }
    
    public Observable getFrameEndObservable()
    {
        return frameEndObservable;
    }
    
    public void signalFrameBegin() {
        frameBeginObservable.setChanged();
        frameBeginObservable.notifyObservers();
    }
    
    public void signalFrameEnd() {
        frameEndObservable.setChanged();
        frameEndObservable.notifyObservers();
    }
}
