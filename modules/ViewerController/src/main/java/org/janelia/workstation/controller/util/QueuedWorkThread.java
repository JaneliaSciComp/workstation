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
    private final BlockingQueue<ThrowingLambda> queue;
    private final Queue<ThrowingLambda> deque = new ArrayDeque<>();
    private final int batchSize;

    public QueuedWorkThread(BlockingQueue<ThrowingLambda> queue, int batchSize) {
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
            ThrowingLambda runnable;
            while ((runnable = deque.poll()) != null) {
                try {
                    runnable.accept();
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