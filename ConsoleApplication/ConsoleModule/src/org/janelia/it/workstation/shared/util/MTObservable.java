package org.janelia.it.workstation.shared.util;

import javax.swing.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:51 PM
 * This class was created to have similar behavior to the java.util.Observable
 * class.  There are key differences. This class is
 * designed to notify on the System Event Queue thread.  This is import (and
 * necessary) when using Swing components.  The java.util.Observable class will
 * always notify on the thread that calls notifyObservers.  Further, it will not
 * allow a call to notifyObservers on a thread that is different than the one that
 * caused the change.  This will cause serious problems in any application where
 * a model entity is being modified in a multi-threaded manor, but the View enities
 * must be modified on the System Event Queue, such as with Swing and a multi-threaded
 * model.
 *
 * @author P. Davies
 */
public class MTObservable extends Observable {

    private Vector<Observer> systemEventThreadObservers;
    private Vector<Observer> anyThreadObservers;

    public MTObservable() {
    }

    /**
     * Add observer to the list that wil be notified on the SystemEventThread.
     * No synchronization necessary as underlying Vector is sychronized
     */
    public void addObserver(Observer observer) {
        if (systemEventThreadObservers == null) systemEventThreadObservers = new Vector<Observer>();
        if (systemEventThreadObservers.contains(observer)) return;
        systemEventThreadObservers.addElement(observer);
    }

    /**
     * Add observer to the list that wil be notified on the SystemEventThread
     */
    public void addSystemEventThreadObserver(Observer observer) {
        addObserver(observer);
    }

    /**
     * Add observer to the list that will be notified on the observables thread
     * No synchronization necessary as underlying Vector is sychronized
     */
    public void addAnyThreadObserver(Observer observer) {
        if (anyThreadObservers == null) anyThreadObservers = new Vector<Observer>();
        if (anyThreadObservers.contains(observer)) return;
        anyThreadObservers.addElement(observer);
    }

    /**
     * Remove observer from the list.  No synchronization necessary as underlying Vector is sychronized
     */
    public void deleteObserver(Observer observer) {
        if (systemEventThreadObservers == null && anyThreadObservers == null) return;
        if ((systemEventThreadObservers != null) && systemEventThreadObservers.contains(observer)) {
            systemEventThreadObservers.removeElement(observer);
            if (systemEventThreadObservers.size() == 0) systemEventThreadObservers = null;
        }
        if ((anyThreadObservers != null) && anyThreadObservers.contains(observer)) {
            anyThreadObservers.removeElement(observer);
            if (anyThreadObservers.size() == 0) anyThreadObservers = null;
        }
    }

    /**
     * Remove all observer from the list.  No synchronization necessary as underlying Vector is sychronized
     */
    public void deleteObservers() {
        systemEventThreadObservers = null;
        anyThreadObservers = null;
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    public void notifyObservers(Object arg) {
        if (!hasChanged()) return;
        //take snapshot of observer array
        if (systemEventThreadObservers != null) {
            Object[] obsArray = systemEventThreadObservers.toArray();
            SwingUtilities.invokeLater(new Notifier(this, obsArray, arg));
        }
        if (anyThreadObservers != null) {
            Object[] observers = anyThreadObservers.toArray();
            for (int i = 0; i < observers.length; i++) {
                ((Observer) observers[i]).update(this, arg);
            }
        }
    }

    private class Notifier implements Runnable {
        Object[] observers;
        Object arg;
        Observable observable;

        public Notifier(Observable observable, Object[] observers, Object arg) {
            this.observers = observers;
            this.arg = arg;
            this.observable = observable;
        }

        public void run() {
            for (int i = 0; i < observers.length; i++) {
                ((Observer) observers[i]).update(observable, arg);
            }
        }

    }
}
