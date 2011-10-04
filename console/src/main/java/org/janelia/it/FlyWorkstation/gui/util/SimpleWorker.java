package org.janelia.it.FlyWorkstation.gui.util;

import javax.swing.SwingWorker;

/**
 * A simple worker class that handles exceptions.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SimpleWorker extends SwingWorker<Void, Void> {

    private Throwable error;
    private boolean disregard;

    @Override
    protected Void doInBackground() throws Exception {
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
}