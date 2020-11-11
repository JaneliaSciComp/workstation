package org.janelia.geometry3d;

import java.util.Observer;

/**
 *
 * @author Christopher Bruns
 */
public interface ObservableInterface {
    void setChanged();
    boolean hasChanged();
    void notifyObservers();
    void notifyObservers(Object arg);
    void addObserver(Observer observer);
    void deleteObserver(Observer observer);
    void deleteObservers();
}
