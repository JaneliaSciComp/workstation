package org.janelia.jacs2.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SingleUsageBlockingQueueTaskCommChannel<E> implements TaskCommChannel<E> {
    @Named("SLF4J")
    @Inject
    private Logger logger;
    private E lastRetrievedElement;
    private BlockingQueue<E> singleElementQueue = new LinkedBlockingDeque<>(1);

    @Override
    public boolean put(E e) {
        try {
            if (lastRetrievedElement == null) {
                singleElementQueue.put(e);
            } else {
                logger.warn("Queue had already been filled with {} while trying to put {}", lastRetrievedElement, e);
            }
            return true;
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }

    @Override
    public E take() {
        try {
            if (lastRetrievedElement != null) {
                return lastRetrievedElement;
            }
            lastRetrievedElement = singleElementQueue.take();
            return lastRetrievedElement;
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }
}
