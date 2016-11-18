package org.janelia.jacs2.service.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SingleUsageBlockingQueueTaskCommChannel<E> implements TaskCommChannel<E> {
    private E lastRetrievedElement;
    private BlockingQueue<E> singleElementQueue = new LinkedBlockingDeque<>(1);

    @Override
    public boolean put(E e) {
        try {
            singleElementQueue.put(e);
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
