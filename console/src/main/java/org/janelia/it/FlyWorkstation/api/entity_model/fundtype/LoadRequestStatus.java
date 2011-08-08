package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:58 PM
 */

import org.janelia.it.FlyWorkstation.api.entity_model.access.LoadRequestStatusObserver;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadRequestStatus implements java.io.Serializable {

    public static final LoadRequestState INACTIVE = new LoadRequestState("Inactive");
    public static final LoadRequestState WAITING = new LoadRequestState("Waiting for Thread");
    public static final LoadRequestState LOADING = new LoadRequestState("Loading");
    public static final LoadRequestState UNLOADING = new LoadRequestState("Unloading");
    public static final LoadRequestState LOADED = new LoadRequestState("Loaded");
    public static final LoadRequestState UNLOADED = new LoadRequestState("Unloaded");
    public static final LoadRequestState ALIGNED = new LoadRequestState("Aligned");
    public static final LoadRequestState NOTIFIED = new LoadRequestState("Notified");
    public static final LoadRequestState COMPLETE = new LoadRequestState("Complete");

    private static final int LOADED_PERCENTAGE = 0;
    private static final int NOTIFIED_PERCENTAGE = 1;
    private LoadRequestState state;
    private LoadRequest request;
    private LoadFilter filter;
    private String idString;
    private transient List swingEventThreadObservers;
    private transient List anyThreadObservers;
    private int percentLoaded;
    private int numberLoaded = -1; //initialize to -1 to distinguish 0 loaded from
    //not yet loaded -- PED 2/23/01
    private int numberAligned;
    private int numberNotified;

    private int lastPercentNotified;
    private int lastPercentAligned;

    private static boolean DEBUG = false;

    LoadRequestStatus(LoadFilter filter) {
        this(filter.getFilterName());
        this.filter = filter;
    }

    LoadRequestStatus(String idString) {
        this.idString = idString;
        state = INACTIVE;
    }

    public String toString() {
        return "LoadRequestStatus: " + getId();
    }

    public String getId() {
        return idString;
    }

    public LoadRequest getLoadRequest() {
        return request;
    }

    public LoadFilter getLoadFilter() {
        return filter;
    }

    public LoadRequestState getLoadRequestState() {
        return state;
    }

    public int getPercentLoaded() {
        return percentLoaded;
    }

    public int getPercentAligned() {
        if (numberLoaded == -1) return 0;
        return (int) (((float) numberAligned / numberLoaded) * 100);
    }

    public int getPercentNotified() {
        if (numberLoaded == -1) return 0;
        return (int) (((float) numberNotified / numberLoaded) * 100);
    }

    /**
     * @parameter swingThreadNotificationOnly - will force all notifications
     * to be sent on the Swing/AWT Event Queue Thread.  Specify true
     * if you are observing from a class that interacts with Swing/AWT classes.
     */
    public void addLoadRequestStatusObserver(LoadRequestStatusObserver observer, boolean swingThreadNotificationOnly) {

        addLoadRequestStatusObserver(observer, swingThreadNotificationOnly, true);
    }

    /**
     * @parameter swingThreadNotificationOnly - will force all notifications
     * to be sent on the Swing/AWT Event Queue Thread.  Specify true
     * if you are observing from a class that interacts with Swing/AWT classes.
     */
    public void addLoadRequestStatusObserver(LoadRequestStatusObserver observer, boolean swingThreadNotificationOnly, boolean bringUpToDate) {

        if (swingThreadNotificationOnly) {
            if (swingEventThreadObservers == null)
                swingEventThreadObservers = Collections.synchronizedList(new ArrayList());
            swingEventThreadObservers.add(observer);
        }
        else {
            if (anyThreadObservers == null) anyThreadObservers = Collections.synchronizedList(new ArrayList());
            anyThreadObservers.add(observer);
        }
        if (bringUpToDate) {
            observer.stateChanged(this, state);
            observer.loadedPercentageChanged(this, percentLoaded);
//         observer.alignedPercentageChanged(this,getPercentAligned());
            observer.notifiedPercentageChanged(this, getPercentNotified());
        }
    }

    public void removeLoadRequestStatusObserver(LoadRequestStatusObserver observer) {
        if (swingEventThreadObservers != null) {
            swingEventThreadObservers.remove(observer);
            if (swingEventThreadObservers.size() == 0) swingEventThreadObservers = null;
        }
        if (anyThreadObservers != null) {
            anyThreadObservers.remove(observer);
            if (anyThreadObservers.size() == 0) anyThreadObservers = null;
        }
    }

    void setNumberLoaded(int numberLoaded) {
        this.numberLoaded = numberLoaded;
        postNewPercent(100, LOADED_PERCENTAGE);
        if (DEBUG) System.out.println(getId() + ": Setting number loaded to: " + numberLoaded);
    }

    void setNumberUnloaded(int numberUnloaded) {
        this.numberLoaded = numberUnloaded;
        postNewPercent(0, LOADED_PERCENTAGE);
        if (DEBUG) System.out.println(getId() + ": Setting number unloaded to: " + numberLoaded);
    }

    /**
     * Methods to be called in the package
     */

    void incrementNumberNotified() {
        numberNotified++;
        if (DEBUG) System.out.println(getId() + ": Incrementing number notified to: " + numberNotified);
        int newNotified = getPercentNotified();
        if (newNotified > lastPercentNotified) {
            postNewPercent(getPercentNotified(), NOTIFIED_PERCENTAGE);
            lastPercentNotified = newNotified;
        }
        if (numberNotified == numberLoaded) setLoadRequestState(NOTIFIED);
    }

    void decrementNumberLoaded() {
        if (numberLoaded != 0) {
            numberLoaded--;
            if (DEBUG) System.out.println(getId() + ": Decrementing number loaded to: " + numberNotified);
            if (numberAligned == numberLoaded) setLoadRequestState(ALIGNED);
            if (numberNotified == numberLoaded) setLoadRequestState(NOTIFIED);
        }
    }

    void setLoadRequestState(LoadRequestState state) {
        if (DEBUG) System.out.println(getId() + ": Setting new State to: " + state);
        this.state = state;

        //Addition/removal of this loadRequestStatus to/from the ActiveThreadModel --PED 2/23/01
        if (state == WAITING) ActiveThreadModel.getActiveThreadModel().addActiveLoadRequestStatus(this);
        if (state == COMPLETE) {
            ActiveThreadModel.getActiveThreadModel().removeActiveLoadRequestStatus(this);
            getLoadFilter().getLoadFilterStatus().requestCompleted(getLoadRequest());
        }
        //Notification of state change --PED 2/23/01

        postNewState(state);
        //Automatic transition of state if necessary --PED 2/23/01
        if (state == LOADED && numberLoaded == 0) setLoadRequestState(ALIGNED);
        if (state == ALIGNED && numberLoaded == 0) setLoadRequestState(NOTIFIED);
        if (state == NOTIFIED) setLoadRequestState(COMPLETE);

        //In unloading, notifications may complete before the state transition.  Check for this
        //and transition to NOTIFIED if it happens --PED 6/6/01
        if (state == UNLOADED && numberLoaded == numberNotified) setLoadRequestState(NOTIFIED);

        //Transition from loaded to aligned and aligned to notifed automatically in case
        //the numbers approach the loaded before the prior state is set -- PED 6/6/01
        if (state == LOADED && numberLoaded == numberAligned) setLoadRequestState(ALIGNED);
        if (state == ALIGNED && numberLoaded == numberNotified) setLoadRequestState(NOTIFIED);
    }

    void setPendingLoadRequestAndStateToWaiting(LoadRequest loadRequest) {
        request = loadRequest;
        setLoadRequestState(WAITING);
    }

    private void postNewState(LoadRequestState newState) {
        LoadRequestStatusObserver[] anyThreadObservers = getObservers(false);
        if (anyThreadObservers.length > 0) {
            new Notifier(anyThreadObservers, newState).run();
        }
        LoadRequestStatusObserver[] swingThreadObservers = getObservers(true);
        if (swingThreadObservers.length > 0) {
            if (SwingUtilities.isEventDispatchThread()) new Notifier(swingThreadObservers, newState).run();
            else SwingUtilities.invokeLater(new Notifier(swingThreadObservers, newState));
        }
    }

    private void postNewPercent(int newPercentage, int type) {
        LoadRequestStatusObserver[] anyThreadObservers = getObservers(false);
        if (anyThreadObservers.length > 0) {
            new Notifier(anyThreadObservers, newPercentage, type).run();
        }
        LoadRequestStatusObserver[] swingThreadObservers = getObservers(true);
        if (swingThreadObservers.length > 0) {
            if (SwingUtilities.isEventDispatchThread()) new Notifier(swingThreadObservers, newPercentage, type).run();
            else SwingUtilities.invokeLater(new Notifier(swingThreadObservers, newPercentage, type));
        }
    }

    private LoadRequestStatusObserver[] getObservers(boolean swingThread) {
        LoadRequestStatusObserver[] emptyArray = new LoadRequestStatusObserver[0];
        if (swingThread) {
            if (swingEventThreadObservers != null) {
                return (LoadRequestStatusObserver[]) swingEventThreadObservers.toArray(emptyArray);
            }
            else return emptyArray;
        }
        else {
            if (anyThreadObservers != null) {
                return (LoadRequestStatusObserver[]) anyThreadObservers.toArray(emptyArray);
            }
            else return emptyArray;
        }
    }

    private class Notifier implements Runnable {
        private int percentLoaded;
        private LoadRequestState state;
        private LoadRequestStatusObserver[] observers;
        private int type;

        private Notifier(LoadRequestStatusObserver[] observers, int percent, int type) {

            this.type = type;
            this.observers = observers;
            this.percentLoaded = percent;
        }

        private Notifier(LoadRequestStatusObserver[] observers, LoadRequestState state) {

            this.observers = observers;
            this.state = state;
        }

        public void run() {
            for (LoadRequestStatusObserver observer : observers) {
                if (state != null) observer.stateChanged(LoadRequestStatus.this, state);
                else switch (type) {
                    case LOADED_PERCENTAGE:
                        observer.loadedPercentageChanged(LoadRequestStatus.this, percentLoaded);
                        break;
                    case NOTIFIED_PERCENTAGE:
                        observer.notifiedPercentageChanged(LoadRequestStatus.this, percentLoaded);
                        break;


                }
            }
        }
    }


}