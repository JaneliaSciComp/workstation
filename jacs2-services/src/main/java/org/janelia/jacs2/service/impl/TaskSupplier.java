package org.janelia.jacs2.service.impl;

public interface TaskSupplier<E> {
    boolean put(E e);
    E take();
}
