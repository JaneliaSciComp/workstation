package org.janelia.console.viewerapi;

/**
 *
 * @author brunsc
 */
public interface GenericObservable<ObservedType> 
{
    // setChanged() and clearChanged() are usually protected.
    // but I want to make these Observables composable, so these are public
    void setChanged();
    void clearChanged();
    
    // Genericized public interface from java.util.Observable
    void addObserver(GenericObserver<ObservedType> observer);
    int countObservers();
    void deleteObserver(GenericObserver<ObservedType> observer);
    void deleteObservers();
    boolean hasChanged();
    // void notifyObservers(); // elide no-arguments version for now
    void notifyObservers(ObservedType data);
}
