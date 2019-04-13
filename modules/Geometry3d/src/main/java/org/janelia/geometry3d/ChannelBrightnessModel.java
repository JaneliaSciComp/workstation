package org.janelia.geometry3d;

import java.util.Observer;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.Copyable;
import org.janelia.console.viewerapi.ObservableInterface;

/**
 *
 * @author Christopher Bruns
 */
public class ChannelBrightnessModel 
implements Copyable<ChannelBrightnessModel>, ObservableInterface
{
    private float minimum = 0; // range 0-1
    private float maximum = 1; // range 0-1
    private float gamma = 1.0f; // range 0-infinity
    private final ComposableObservable changeObservable = new ComposableObservable();

    public ChannelBrightnessModel() {}

    public ChannelBrightnessModel(ChannelBrightnessModel rhs) {
        copy(rhs);
    }

    @Override
    public final void copy(ChannelBrightnessModel rhs) {
        setMinimum(rhs.minimum);
        setMaximum(rhs.maximum);        
    }
    
    public float getMaximum() {
        return maximum;
    }

    public float getMinimum() {
        return minimum;
    }

    public final void setMinimum(float minimum) {
        if (minimum == this.minimum)
            return;
        // System.out.println("Min changed!");
        changeObservable.setChanged();
        this.minimum = minimum;
    }

    public final void setMaximum(float maximum) {
        if (maximum == this.maximum)
            return;
        changeObservable.setChanged();
        this.maximum = maximum;
    }

    @Override
    public void setChanged() {
        changeObservable.setChanged();
    }

    @Override
    public void notifyObservers() {
        changeObservable.notifyObservers();
    }
    
    @Override
    public void notifyObservers(Object arg) {
        changeObservable.notifyObservers(arg);
    }

    @Override
    public void addObserver(Observer observer) {
        changeObservable.addObserver(observer);
    }

    @Override
    public void deleteObserver(Observer observer) {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers() {
        changeObservable.deleteObservers();
    }

    @Override
    public boolean hasChanged()
    {
        return changeObservable.hasChanged();
    }

    public float getGamma() {
        return gamma;
    }
    
}
