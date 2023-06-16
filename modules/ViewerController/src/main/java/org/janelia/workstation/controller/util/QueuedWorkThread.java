package org.janelia.workstation.controller.util;

import org.janelia.workstation.core.options.ApplicationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

import java.util.concurrent.BlockingQueue;

/**
 * Background daemon thread that continuously processes work from
 * a queue and executes them.
 */
public abstract class QueuedWorkThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(QueuedWorkThread.class);

    /** Introduce some artificial latency for each item, for testing purposes */
    private static final int ARTIFICIAL_LATENCY = 2000;
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
                    if (ARTIFICIAL_LATENCY > 0) {
                        Thread.sleep(ARTIFICIAL_LATENCY);
                    }
                }
                catch (Exception e) {
                    handleException(e);
                }
            }
        }
    }

    public abstract void handleException(Throwable e);
}