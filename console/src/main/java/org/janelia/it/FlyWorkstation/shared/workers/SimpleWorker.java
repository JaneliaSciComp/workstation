package org.janelia.it.FlyWorkstation.shared.workers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple worker class that handles exceptions. Also allows for easy attachment of a ProgressMonitor.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SimpleWorker extends SwingWorker<Void, Void> implements PropertyChangeListener {

    private static final Logger log = LoggerFactory.getLogger(SimpleWorker.class);
    
    protected Throwable error;
    protected boolean disregard;
    protected ProgressMonitor progressMonitor; 
    
    @Override
    protected Void doInBackground() throws Exception {
    	addPropertyChangeListener(this);
        setProgress(0);
        if (progressMonitor!=null) {
        	setProgress(1);
        }
        try {
            doStuff();
        }
        catch (Throwable e) {
            this.error = e;
        }
        return null;
    }

    @Override
    protected void done() {
        if (isCancelled() || isDisregarded()) {
            return;
        }
        setProgress(100);
        if (error == null) {
            hadSuccess();
        }
        else {
            hadError(error);
        }
    }

    public synchronized boolean isDisregarded() {
    	return disregard;
    }
    
    /**
     * A little different from cancel() because it doesn't interrupt the thread, it just ensures that hadSuccess or 
     * hadError is never called. 
     */
    public synchronized void disregard() {
    	this.disregard = true;
    }
    
    /**
     * Implement this to do stuff in another (non-EDT) thread so that it doesn't affect the GUI.
     *
     * @throws Exception any exception throw will be passed back with hadError
     */
    protected abstract void doStuff() throws Exception;

    /**
     * Called in the EDT after doStuff completes without throwing an error.
     */
    protected abstract void hadSuccess();

    /**
     * Called in the EDT after doStuff throws an error.
     *
     * @param error the error thrown by doStuff
     */
    protected abstract void hadError(Throwable error);

    /**
     * Return the error that occurred, or null if no error occurred.
     * @return
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Get the progress monitor for this task, if one was been set.
     * @return
     */
    public ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

    /**
     * Add a progress monitor to this task.
     * @param progressMonitor
     */
	public void setProgressMonitor(ProgressMonitor progressMonitor) {
		this.progressMonitor = progressMonitor;
	}

	/**
	 * Set the progress with number of items completed out of a total number.
	 * @param curr number of items completed
	 * @param total total number of items
	 */
	public void setProgress(long curr, long total) {
    	double percentDone = (double)curr / (double)total;
    	int p = (int)Math.round(100*percentDone);
        try {
            setProgress(p);
        }
        catch (IllegalArgumentException e) {
            log.error("Invalid progress: "+curr+"/"+total,e);
        }
	}
	
	/**
     * Invoked when the workers progress property changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
    	if (progressMonitor==null) return;
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("Completed %d%%", progress);
            progressMonitor.setNote(message);
            if (progressMonitor.isCanceled()) {
                cancel(true);
            }
        }
    }
}