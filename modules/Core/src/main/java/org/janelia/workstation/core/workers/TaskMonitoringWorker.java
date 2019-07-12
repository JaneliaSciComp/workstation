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
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.workers.WorkerStartedEvent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A background worker thread which monitors a server-side task. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskMonitoringWorker extends NamedBackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskMonitoringWorker.class);
    
    public static final int numWorkerThreads = 10;
    
    protected static final int REFRESH_DELAY_MS = 2000;

    static {
        if (log.isDebugEnabled()) {
            log.debug("Using {} task monitoring threads.", numWorkerThreads);
        }
    }
    
    private Long taskId;
    private Task task;
    private ProgressHandle handle;

    public TaskMonitoringWorker() {
    }
    
    public TaskMonitoringWorker(Long taskId) {
        this.taskId = taskId;
    }
    
    @Override
    protected void doStuff() throws Exception {

        try {

            while (true) {
                this.task = StateMgr.getStateMgr().getTaskById(taskId);
                
                if (getName()==null) {
                    setName(task.getDisplayName());
                }
                
                String lastStatus = task.getLastEvent().getDescription();
                if (lastStatus!=null) {
                    setStatus(lastStatus);
                }

                if (handle==null) {
                    handle = ProgressHandleFactory.createHandle(task.getDisplayName(), new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            FrameworkAccess.getProgressController().showProgressPanel();
                        }
                    });
                    handle.start();
                    handle.switchToIndeterminate();
                }

                if (task==null) {
                    handle.finish();
                    throw new IllegalStateException("Task does not exist: "+taskId);
                }   

                if (task.isDone()) {
                    handle.finish();
                    // Check for errors
                    if (task.getLastEvent().getEventType().equals(Event.ERROR_EVENT)) {
                        String msg = "Task "+task.getObjectId()+" had error: "+task.getLastEvent().getDescription();
                        throw new ServiceException(msg);
                    }
                    return;
                }
                
                throwExceptionIfCancelled();
                
                try {
                    Thread.sleep(REFRESH_DELAY_MS);
                }
                catch (InterruptedException e) {
                    handle.finish();
                    return;
                }
            }
        
        }
        catch (Exception e) {
            if (handle!=null) {
                handle.finish();
            }
            throw e;
        }
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Task getTask() {
        return task;
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
                    thread.setName("TaskMonitoring-" + thread.getName());
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

    /**
     * Overridden to use our own executor service.
     */
    @Override
    public void executeWithEvents(boolean showProgressMonitor) {
        this.emitEvents = true;
        this.showProgressMonitor = showProgressMonitor;
        Events.getInstance().postOnEventBus(new WorkerStartedEvent(this, showProgressMonitor));
        getWorkersExecutorService().execute(this);
    }
}
