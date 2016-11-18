package org.janelia.jacs2.service.impl;

public interface ServiceSupplier<E> {
    boolean put(E e);
    E take();
}
