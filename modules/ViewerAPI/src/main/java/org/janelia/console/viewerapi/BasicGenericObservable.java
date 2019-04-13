package org.janelia.console.viewerapi;

import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author brunsc
 */
public class BasicGenericObservable<ObservedType> implements GenericObservable<ObservedType> 
{
    private boolean isChanged = false;
    private final Collection<GenericObserver<ObservedType>> observers = 
            new HashSet<>();

    @Override
    public void setChanged() {
        isChanged = true;
    }

    @Override
    public void clearChanged() {
        isChanged = false;
    }

    @Override
    public void addObserver(GenericObserver<ObservedType> observer) {
        observers.add(observer);
    }

    @Override
    public int countObservers() {
        return observers.size();
    }

    @Override
    public void deleteObserver(GenericObserver<ObservedType> observer) {
        observers.remove(observer);
    }

    @Override
    public void deleteObservers() {
        observers.clear();
    }

    @Override
    public boolean hasChanged() {
        return isChanged;
    }

    @Override
    public void notifyObservers(ObservedType data) {
        if (!isChanged) 
            return;
        for (GenericObserver<ObservedType> observer : observers) {
            observer.update(this, data);
        }
        isChanged = false;
    }
    
}
