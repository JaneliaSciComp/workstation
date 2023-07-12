package org.janelia.workstation.controller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

import java.util.concurrent.BlockingQueue;

/**
 * Background daemon thread that continuously processes work items (i.e. runnables) from
 * a queue and executes them. This thread runs forever after being started.
 */
public abstract class QueuedWorkThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(QueuedWorkThread.class);

    private final BlockingQueue<Runnable> queue;
    private final Queue<Runnable> deque = new ArrayDeque<>();
    private final int batchSize;

    public QueuedWorkThread(BlockingQueue<Runnable> queue, int batchSize) {
        this.queue = queue;
        this.batchSize = batchSize;
        setDaemon(true);
    }

    public void run() {
        // Run forever
        while (true) {
            // Get the next batch of items to process
            int drained = queue.drainTo(deque, batchSize);
            if (drained == 0) {
                // Block until there is something to process
                try {
                    deque.offer(queue.take());
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // Process all the items in order, one at a time
            log.info("Processing {} work items", deque.size());
            Runnable runnable;
            while ((runnable = deque.poll()) != null) {
                try {
                    runnable.run();
                }
                catch (Exception e) {
                    handleException(e);
                }
            }
            notifyBatchCompleted();
        }
    }

    /**
     * Override this to take action when a batch of work items has been completed.
     */
    protected void notifyBatchCompleted() {
    }

    /**
     * Override this to handle exceptions that occur when handling work items.
     * @param e the exception that was thrown in executing the work item
     */
    public abstract void handleException(Throwable e);
}