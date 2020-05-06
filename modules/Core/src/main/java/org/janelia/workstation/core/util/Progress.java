package org.janelia.workstation.core.util;

/**
 * An interface for generic client/task interfacing. It works in both directions: 
 * 
 * The client can interface with the task by setting the value of isCancelled,
 * which the task is encouraged to read and act upon. 
 * 
 * The client can get the task's progress via the setProgress method.
 *   
 * Note that this object is created by the client and passed into the task. 
 * A setter for cancellation state and a getter for the progress are assumed to be 
 * private to the client and thus not included in this interface. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Progress {

    /**
     * Returns true if this task has been cancelled. The client is responsible for 
     * implementing this method and returning an accurate value when the task 
     * requests it.
     */
    boolean isCancelled();
    
    /**
     * Set the progress with number of items completed out of a total number.
     * 
     * @param curr number of items completed
     * @param total total number of items
     */
    void setProgress(long curr, long total);
    
    /**
     * Set a description of the current step that the task is performing
     * 
     * @param status description of task status
     */
    void setStatus(String status);

}