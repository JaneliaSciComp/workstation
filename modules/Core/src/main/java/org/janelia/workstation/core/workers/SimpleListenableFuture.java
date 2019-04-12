package org.janelia.workstation.core.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractFuture;

/**
 * Simple listenable future implementation which allows clients to register a listener to 
 * wait for the completion of a ResultWorker.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleListenableFuture<T> extends AbstractFuture<T> {

    public boolean setComplete(T result) {
        return super.set(result);
    }

    /**
     * We have to override this to allow it to be called from this package.
     */
    @Override
    protected boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }

    public void addListener(Runnable listener) {
        super.addListener(listener, getFutureExecutorService());
    }

    public static final int numWorkerThreads = 10;
    private static ExecutorService executorService;
    private static synchronized ExecutorService getFutureExecutorService() {
        if (executorService == null) {
            //this creates daemon threads.
            ThreadFactory threadFactory = new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                public Thread newThread(final Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setName("SimpleWorkerFuture-" + thread.getName());
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executorService = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads,
                            10L, TimeUnit.MINUTES,
                            new LinkedBlockingQueue<Runnable>(),
                            threadFactory);
        }

        return executorService;
    }
}
