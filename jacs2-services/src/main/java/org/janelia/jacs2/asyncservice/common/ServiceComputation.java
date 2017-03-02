package org.janelia.jacs2.asyncservice.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
    <U, V> ServiceComputation<V> thenCombine(ServiceComputation<U> otherComputation, BiFunction<? super T, ? super U, ? extends V> fn);
    <U> ServiceComputation<U> thenCombineAll(List<ServiceComputation<?>> otherComputations, BiFunction<? super T, List<?>, ? extends U> fn);
}
