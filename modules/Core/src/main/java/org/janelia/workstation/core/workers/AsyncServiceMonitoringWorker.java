package org.janelia.workstation.core.workers;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.workers.WorkerChangedEvent;
import org.janelia.workstation.core.events.workers.WorkerStartedEvent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A background worker thread which monitors an async service invocation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AsyncServiceMonitoringWorker extends BackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceMonitoringWorker.class);

    public static final int numWorkerThreads = 10;

    protected static final int REFRESH_DELAY_MS = 2000;

    static {
        if (log.isDebugEnabled()) {
            log.debug("Using {} task monitoring threads.", numWorkerThreads);
        }
    }

    private Long serviceId;
    private String serviceName;
    private ProgressHandle handle;


    public AsyncServiceMonitoringWorker() {
    }

    public AsyncServiceMonitoringWorker(Long serviceId) {
        this.serviceId = serviceId;
    }
    
    @Override
    protected void doStuff() throws Exception {
        AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
        try {
            if (serviceId == null) {
                throw new ServiceException("There was no service invocation - serviceId is still empty");
            }
            while (true) {
                String serviceStatus = asyncServiceClient.getServiceStatus(serviceId);
                setStatus(serviceStatus);

                if (handle==null) {
                    handle = ProgressHandleFactory.createHandle(getName(), new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            FrameworkAccess.getProgressController().showProgressPanel();
                        }
                    });
                    handle.start();
                    handle.switchToIndeterminate();
                }

                Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));

                if (hasCompletedUnsuccessfully(serviceStatus)) {
                    completed();
                    handle.finish();
                    // handle errors
                    throwException(serviceStatus);
                } 
                else if (hasCompletedSuccessfully(serviceStatus)) {
                    completed();
                    handle.finish();
                    return;
                }

                try {
                    Thread.sleep(REFRESH_DELAY_MS);
                }
                catch (InterruptedException e) {
                    handle.finish();
                    return;
                }
            }
        } catch (Exception e) {
            if (handle!=null) {
                handle.finish();
            }
            throw e;
        }
    }
    
    protected void completed() {
        
    }

    @Override
    public String getName() {
        return serviceName;
    }

    public Long getServiceId() {
        return serviceId;
    }

    /**
     * Adapted from SimpleWorker so that we can use a separate thread pool and customize the number of threads
     */
    private static ExecutorService executorService;
    private static synchronized ExecutorService getWorkersExecutorService() {
        if (executorService == null) {
            //this creates daemon threads.
            ThreadFactory threadFactory = new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                public Thread newThread(final Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setName("AsyncServiceMonitoring-" + thread.getName());
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

    @Override
    public void executeWithEvents(boolean showProgressMonitor) {
        this.emitEvents = true;
        this.showProgressMonitor = showProgressMonitor;
        Events.getInstance().postOnEventBus(new WorkerStartedEvent(this, showProgressMonitor));
        getWorkersExecutorService().execute(this);
    }

    protected void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    private boolean hasCompletedUnsuccessfully(String state) {
        return "CANCELED".equals(state) || "ERROR".equals(state) || "TIMEOUT".equals(state);
    }

    private boolean hasCompletedSuccessfully(String state) {
        return "SUCCESSFUL".equals(state) || "ARCHIVED".equals(state);
    }

    private void throwException (String state) throws ServiceException {
        switch (state) {
            case "CANCELED":
                throw new ServiceException("Service " + serviceId + " was cancelled");
            case "ERROR":
                throw new ServiceException("Service " + serviceId + " encountered errors");
            case "TIMEOUT":
                throw new ServiceException("Service " + serviceId + " timed out");
            default:
                throw new ServiceException("Unknow service state " + state);
        }
    }
}
