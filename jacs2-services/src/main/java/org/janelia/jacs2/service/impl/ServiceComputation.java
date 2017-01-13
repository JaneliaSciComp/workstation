package org.janelia.jacs2.service.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ServiceComputation represents a certain service computation stage.
 *
 * @param <T> result type
 */
public interface ServiceComputation<T> {
    T get();
    boolean isDone();
    boolean isCompletedExceptionally();
    ServiceComputation<T> supply(Supplier<T> fn);
    ServiceComputation<T> exceptionally(Function<Throwable, ? extends T> fn);
    <U> ServiceComputation<U> thenApply(Function<? super T, ? extends U> fn);
    <U> ServiceComputation<U> thenCompose(Function<? super T, ? extends ServiceComputation<U>> fn);
    ServiceComputation<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);
}
