package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:57 PM
 */

import org.janelia.it.FlyWorkstation.shared.util.MTObservable;

import java.util.*;

/**
 * This is a model of the threads that are actively loading in the system
 */
public class ActiveThreadModel extends MTObservable {
    static private ActiveThreadModel activeThreadModel;

    private Map statusObjects = Collections.synchronizedMap(new HashMap());

    private ActiveThreadModel() {
    }

    public static ActiveThreadModel getActiveThreadModel() {
        if (activeThreadModel == null) activeThreadModel = new ActiveThreadModel();
        return activeThreadModel;
    }

    public LoadRequestStatus[] getActiveLoadRequestStatusObjects() {
        Set activeEntries = null;
        synchronized (statusObjects) {
            activeEntries = statusObjects.entrySet();
        }
        List statusObjects = new ArrayList();
        for (Iterator it = activeEntries.iterator(); it.hasNext(); ) {
            statusObjects.add(((Map.Entry) it.next()).getValue());
        }
        return (LoadRequestStatus[]) statusObjects.toArray(new LoadRequestStatus[statusObjects.size()]);
    }

    public int getActiveThreadCount() {
        return statusObjects.size();
    }

    public void addObserver(Observer observer) {
        super.addObserver(observer);
    }

    public void addObserver(Observer observer, boolean bringUpToDate) {
        addObserver(observer);
        for (Iterator it = statusObjects.keySet().iterator(); it.hasNext(); ) {
            observer.update(this, statusObjects.get(it.next()));
        }
    }

    void addActiveLoadRequestStatus(LoadRequestStatus loadRequestStatus) {
        statusObjects.put(loadRequestStatus, loadRequestStatus);
        setChanged();
        notifyObservers(loadRequestStatus);
        clearChanged();
    }

    void removeActiveLoadRequestStatus(LoadRequestStatus loadRequestStatus) {
        statusObjects.remove(loadRequestStatus);
        setChanged();
        notifyObservers(loadRequestStatus);
        clearChanged();
    }


}


