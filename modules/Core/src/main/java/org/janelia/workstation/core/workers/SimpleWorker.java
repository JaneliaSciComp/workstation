package org.janelia.workstation.core.workers;

import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * A simple result worker that doesn't return any results directly.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SimpleWorker extends ResultWorker<Void> {

    @Override
    protected Void createResult() throws Exception {
        doStuff();
        return null;
    }
    
    /**
     * Implement this to do stuff in another (non-EDT) thread so that it doesn't affect the GUI.
     *
     * @throws Exception any exception throw will be passed back with hadError
     */
    protected abstract void doStuff() throws Exception;
    
    /**
     * Short cut for running something in the background with minimal boilerplate.
     * @param runnable something to run
     */
    public static SimpleListenableFuture<Void> runInBackground(final Runnable runnable) {

        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() {
                runnable.run();
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        
        return worker.executeWithFuture();
    }
    
}