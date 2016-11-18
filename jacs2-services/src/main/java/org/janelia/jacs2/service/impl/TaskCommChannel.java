package org.janelia.jacs2.service.impl;

/**
 * Task communication channel.
 * @param <E> type of the object that is sent through the channel.
 */
public interface TaskCommChannel<E> {
    boolean put(E e);
    E take();
}
