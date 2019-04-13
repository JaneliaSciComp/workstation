
package org.janelia.console.viewerapi;

import java.util.Observable;

/**
 * Exposes protected methods, so Observable can be used
 * via composition, in addition to use by inheritance.
 * Efficient implementations should implement bulk updates by automatically
 * calling setChanged() many times, and then manually calling 
 * notifyObservers() once, after
 * all the relevant changes have been registered.
 * 
 * @author cmbruns
 */
public class ComposableObservable extends Observable 
implements ObservableInterface
{
    /**
     * Potentially slow notification of all listeners. For efficiency,
     * notifyObservers() only notifies listeners IF setChanged() has been
     * called since the previous call to notifyObservers().
     */
    @Override
    public void notifyObservers() {
        super.notifyObservers();
    }

    /**
     * Exposes setChanged() publicly, so we can use Observable by composition, not just by inheritance.
     * setChanged() is a fast inexpensive operation that marks the Observable as "dirty",
     * but does NOT automatically notify listeners. 
     * It should be OK to call "setChanged()" whenever the Observable is known to have
     * changes to its internal state. 
     */
    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean hasChanged()
    {
        return super.hasChanged();
    }
}
