package org.janelia.console.viewerapi;

/**
 *
 * @author brunsc
 */
public interface GenericObserver<ObservedType> {
    void update(GenericObservable<ObservedType> object, ObservedType data);
}
