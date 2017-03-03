package org.janelia.jacs2.asyncservice.common;


import com.offbynull.coroutines.user.CoroutineRunner;
import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class ServiceComputationQueue {

    static boolean runTask(ServiceComputationTask<?> task) {
        CoroutineRunner r = new CoroutineRunner(task);
        for (;;) {
            if (!r.execute()) {
                return !task.isSuspended() && task.isDone();
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private final ExecutorService taskExecutor;
    private final ExecutorService queueInspector;
    private final BlockingQueue<ServiceComputationTask<?>> taskQueue;

    @Inject
    public ServiceComputationQueue(ExecutorService taskExecutor, @SuspendedTaskExecutor ExecutorService queueInspector) {
        this.taskExecutor = taskExecutor;
        this.queueInspector = queueInspector;
        taskQueue = new LinkedBlockingQueue<>(300);
        this.queueInspector.submit(() -> executeTasks());
    }

    private void executeTasks() {
        for (;;) {
            try {
                cycleThroughAvailableTasks();
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void cycleThroughAvailableTasks() {
        int n = taskQueue.size();
        for (int i = 0; i < n; i++) {
            try {
                ServiceComputationTask<?> task = taskQueue.take();
                if (task.isReady()) {
                    taskExecutor.execute(() -> {
                        if (!ServiceComputationQueue.runTask(task)) {
                            // if it's not done put it back in the queue
                            taskQueue.offer(task);
                        }
                    });
                } else {
                    // if the task is not ready put it back in the queue
                    taskQueue.offer(task);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    void submit(ServiceComputationTask<?> task) {
        taskQueue.offer(task);
    }
}
