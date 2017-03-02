package org.janelia.jacs2.asyncservice.common;


import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class ServiceComputationQueue {

    private final ExecutorService taskExecutor;
    private final ExecutorService queueInspector;
    private final BlockingQueue<ServiceComputationTask<?>> taskQueue;

    @Inject
    public ServiceComputationQueue(ExecutorService taskExecutor, @SuspendedTaskExecutor ExecutorService queueInspector) {
        this.taskExecutor = taskExecutor;
        this.queueInspector = queueInspector;
        taskQueue = new LinkedBlockingQueue<>();
        this.queueInspector.submit(() -> executeTasks());
    }

    private void executeTasks() {
        for (;;) {
            try {
                ServiceComputationTask<?> task = taskQueue.take();
                taskExecutor.submit(() -> {
                    if (!task.tryFire()) {
                        taskQueue.offer(task);
                    }
                });
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    void submit(ServiceComputationTask<?> task) {
        taskQueue.offer(task);
    }
}
