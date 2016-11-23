package org.janelia.jacs2.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SingleUsageBlockingQueueTaskCommChannel<E> implements TaskCommChannel<E> {
    private static final Logger LOG = LoggerFactory.getLogger(SingleUsageBlockingQueueTaskCommChannel.class);

    private E lastRetrievedElement;
    private BlockingQueue<E> singleElementQueue = new LinkedBlockingDeque<>(1);

    @Override
    public boolean put(E e) {
        try {
            LOG.debug("Trying to put {} into {}", e, singleElementQueue);
            if (lastRetrievedElement == null) {
                singleElementQueue.put(e);
            } else {
                LOG.warn("Queue had already been filled with {} while trying to put {}", lastRetrievedElement, e);
            }
            LOG.debug("Successfully put {} into {}", e, singleElementQueue);
            return true;
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }

    @Override
    public E take() {
        try {
            if (lastRetrievedElement != null) {
                LOG.debug("Take last element {}", lastRetrievedElement);
                return lastRetrievedElement;
            }
            lastRetrievedElement = singleElementQueue.take();
            LOG.debug("Take element {}", lastRetrievedElement);
            return lastRetrievedElement;
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }
}
