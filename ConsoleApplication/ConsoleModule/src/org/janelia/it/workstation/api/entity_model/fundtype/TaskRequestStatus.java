package org.janelia.it.workstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:58 PM
 */

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskRequestStatus implements java.io.Serializable {

    public static final TaskRequestState INACTIVE = new TaskRequestState("Inactive");
    public static final TaskRequestState WAITING = new TaskRequestState("Waiting for Thread");
    public static final TaskRequestState LOADING = new TaskRequestState("Loading");
    public static final TaskRequestState UNLOADING = new TaskRequestState("Unloading");
    public static final TaskRequestState LOADED = new TaskRequestState("Loaded");
    public static final TaskRequestState UNLOADED = new TaskRequestState("Unloaded");
    public static final TaskRequestState ALIGNED = new TaskRequestState("Aligned");
    public static final TaskRequestState NOTIFIED = new TaskRequestState("Notified");
    public static final TaskRequestState RUNNING = new TaskRequestState("Running");
    public static final TaskRequestState COMPLETE = new TaskRequestState("Complete");

    private static final int LOADED_PERCENTAGE = 0;
    private static final int NOTIFIED_PERCENTAGE = 1;
    private TaskRequestState state;
    private TaskRequest request;
    private TaskFilter filter;
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

    TaskRequestStatus(TaskFilter filter) {
        this(filter.getFilterName());
        this.filter = filter;
    }

    TaskRequestStatus(String idString) {
        this.idString = idString;
        state = INACTIVE;
    }

    public String toString() {
        return "TaskRequestStatus: " + getId();
    }

    public String getId() {
        return idString;
    }

    public TaskRequest getTaskRequest() {
        return request;
    }

    public TaskFilter getTaskFilter() {
        return filter;
    }

    public TaskRequestState getTaskRequestState() {
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
    public void addTaskRequestStatusObserver(org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver observer, boolean swingThreadNotificationOnly) {

        addTaskRequestStatusObserver(observer, swingThreadNotificationOnly, true);
    }

    /**
     * @parameter swingThreadNotificationOnly - will force all notifications
     * to be sent on the Swing/AWT Event Queue Thread.  Specify true
     * if you are observing from a class that interacts with Swing/AWT classes.
     */
    public void addTaskRequestStatusObserver(org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver observer, boolean swingThreadNotificationOnly, boolean bringUpToDate) {

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

    public void removeTaskRequestStatusObserver(org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver observer) {
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
        if (numberNotified == numberLoaded) setTaskRequestState(NOTIFIED);
    }

    void decrementNumberLoaded() {
        if (numberLoaded != 0) {
            numberLoaded--;
            if (DEBUG) System.out.println(getId() + ": Decrementing number loaded to: " + numberNotified);
            if (numberAligned == numberLoaded) setTaskRequestState(ALIGNED);
            if (numberNotified == numberLoaded) setTaskRequestState(NOTIFIED);
        }
    }

    public void setTaskRequestState(TaskRequestState state) {
        if (DEBUG) System.out.println(getId() + ": Setting new State to: " + state);
        this.state = state;

        //Addition/removal of this taskRequestStatus to/from the ActiveThreadModel --PED 2/23/01
        if (state == WAITING) ActiveThreadModel.getActiveThreadModel().addActiveTaskRequestStatus(this);
        if (state == COMPLETE) {
            ActiveThreadModel.getActiveThreadModel().removeActiveTaskRequestStatus(this);
            getTaskFilter().getTaskFilterStatus().requestCompleted(getTaskRequest());
        }
        //Notification of state change --PED 2/23/01

        postNewState(state);
        //Automatic transition of state if necessary --PED 2/23/01
        if (state == LOADED && numberLoaded == 0) setTaskRequestState(ALIGNED);
        if (state == ALIGNED && numberLoaded == 0) setTaskRequestState(NOTIFIED);
        if (state == NOTIFIED) setTaskRequestState(COMPLETE);

        //In unloading, notifications may complete before the state transition.  Check for this
        //and transition to NOTIFIED if it happens --PED 6/6/01
        if (state == UNLOADED && numberLoaded == numberNotified) setTaskRequestState(NOTIFIED);

        //Transition from loaded to aligned and aligned to notifed automatically in case
        //the numbers approach the loaded before the prior state is set -- PED 6/6/01
        if (state == LOADED && numberLoaded == numberAligned) setTaskRequestState(ALIGNED);
        if (state == ALIGNED && numberLoaded == numberNotified) setTaskRequestState(NOTIFIED);
    }

    public void setPendingTaskRequestAndStateToWaiting(TaskRequest taskRequest) {
        request = taskRequest;
        setTaskRequestState(WAITING);
    }

    private void postNewState(TaskRequestState newState) {
        org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] anyThreadObservers = getObservers(false);
        if (anyThreadObservers.length > 0) {
            new Notifier(anyThreadObservers, newState).run();
        }
        org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] swingThreadObservers = getObservers(true);
        if (swingThreadObservers.length > 0) {
            if (SwingUtilities.isEventDispatchThread()) new Notifier(swingThreadObservers, newState).run();
            else SwingUtilities.invokeLater(new Notifier(swingThreadObservers, newState));
        }
    }

    private void postNewPercent(int newPercentage, int type) {
        org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] anyThreadObservers = getObservers(false);
        if (anyThreadObservers.length > 0) {
            new Notifier(anyThreadObservers, newPercentage, type).run();
        }
        org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] swingThreadObservers = getObservers(true);
        if (swingThreadObservers.length > 0) {
            if (SwingUtilities.isEventDispatchThread()) new Notifier(swingThreadObservers, newPercentage, type).run();
            else SwingUtilities.invokeLater(new Notifier(swingThreadObservers, newPercentage, type));
        }
    }

    private org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] getObservers(boolean swingThread) {
        org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] emptyArray = new org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[0];
        if (swingThread) {
            if (swingEventThreadObservers != null) {
                return (org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[]) swingEventThreadObservers.toArray(emptyArray);
            }
            else return emptyArray;
        }
        else {
            if (anyThreadObservers != null) {
                return (org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[]) anyThreadObservers.toArray(emptyArray);
            }
            else return emptyArray;
        }
    }

    private class Notifier implements Runnable {
        private int percentLoaded;
        private TaskRequestState state;
        private org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] observers;
        private int type;

        private Notifier(org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] observers, int percent, int type) {

            this.type = type;
            this.observers = observers;
            this.percentLoaded = percent;
        }

        private Notifier(org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver[] observers, TaskRequestState state) {

            this.observers = observers;
            this.state = state;
        }

        public void run() {
            for (org.janelia.it.workstation.api.entity_model.access.TaskRequestStatusObserver observer : observers) {
                if (state != null) observer.stateChanged(TaskRequestStatus.this, state);
                else switch (type) {
                    case LOADED_PERCENTAGE:
                        observer.loadedPercentageChanged(TaskRequestStatus.this, percentLoaded);
                        break;
                    case NOTIFIED_PERCENTAGE:
                        observer.notifiedPercentageChanged(TaskRequestStatus.this, percentLoaded);
                        break;


                }
            }
        }
    }


}