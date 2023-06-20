package org.janelia.workstation.controller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

import java.util.concurrent.BlockingQueue;

/**
 * Background daemon thread that continuously processes work from
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
        while (true) {
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
            log.info("Processing {} work items", deque.size());
            Runnable runnable;
            while ((runnable = deque.poll()) != null) {
                try {
                    runnable.run();
                    // Introduce some artificial latency for each item, for testing purposes
                    // Thread.sleep(2000);
                }
                catch (Exception e) {
                    handleException(e);
                }
            }
        }
    }

    public abstract void handleException(Throwable e);
}