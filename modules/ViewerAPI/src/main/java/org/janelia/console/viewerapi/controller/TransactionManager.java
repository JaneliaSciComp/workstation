 package org.janelia.console.viewerapi.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author schauderd
 * This class is intended as a starting point for managing global transactions from the ViewerAPI
 * 
 */

public class TransactionManager {
    boolean started;
    Map<Observer, Map<String,Object>> delayedItems;
    private static TransactionManager instance;
    
    private TransactionManager() {
        this.started = false;
    }
    
    public static TransactionManager getInstance() {
        if (instance==null) {
            instance = new TransactionManager();
        }
        return instance;
    }
    
    
    public void beginTransaction() {
        delayedItems = new HashMap<Observer, Map<String,Object>>();
        started = true;
    }
    
    public void endTransaction() {
        started = false;
        Iterator<Observer> delayedItemsIter = delayedItems.keySet().iterator();
        while (delayedItemsIter.hasNext()) {
            Observer observer = delayedItemsIter.next();
            Map<String,Object> observerInfo = delayedItems.get(observer);
            observer.update((Observable)observerInfo.get("observable"), observerInfo.get("meta"));
        }
    }
    
    public boolean isTransactionStarted() {
        return started;
    }
    
    // in some cases an observer should executed only once in a transaction
    // this is a simple mechanism where the observer can register itself and 
    // get called (in most cases the way we are using Observables is with no
    // event information
    public void addObservables(Observer observer, Observable observable, Object arg) {
        Map<String,Object> arguments = new HashMap<String,Object>();
        arguments.put("observable", observable);
        arguments.put("meta", arg);
        if (delayedItems.containsKey(observer)) {
            Object meta = delayedItems.get(observer).get("meta");
            List argList;
            if (meta==null || ! (meta instanceof List)) {
                argList = new ArrayList();
            } else {
                argList = (List)meta;
            }
            argList.add(arg);
            delayedItems.get(observer).put("meta", argList);
        } else {
            delayedItems.put(observer, arguments);
        }
    }
}
