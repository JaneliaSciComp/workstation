package org.janelia.jacs2.service.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class BlockingQueueServiceSupplier<E> implements ServiceSupplier<E> {
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
            return singleElementQueue.take();
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }
}
