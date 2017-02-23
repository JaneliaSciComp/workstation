package org.janelia.jacs2.asyncservice.common;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * FutureBasedServiceComputation is an implementation of a ServiceComputation.
 *
 * @param <T> result type
 */
public class FutureBasedServiceComputation<T> implements ServiceComputation<T> {

    private final ExecutorService executor;
    private CompletableFuture<T> future;

    FutureBasedServiceComputation(ExecutorService executor) {
        this.executor = executor;
    }

    FutureBasedServiceComputation(ExecutorService executor, T result) {
        this.executor = executor;
        future = CompletableFuture.completedFuture(result);
    }

    FutureBasedServiceComputation(ExecutorService executor, Throwable exc) {
        this.executor = executor;
        future = new CompletableFuture<>();
        future.completeExceptionally(exc);
    }

    private FutureBasedServiceComputation(ExecutorService executor, CompletableFuture<T> future) {
        this.executor = executor;
        this.future = future;
    }

    @Override
    public T get() {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    public void complete(T result) {
        future.complete(result);
    }

    public void completeExceptionally(Throwable exc) {
        future.completeExceptionally(exc);
    }

    @Override
    public ServiceComputation<T> supply(Supplier<T> fn) {
        future = CompletableFuture.supplyAsync(fn, executor);
        return this;
    }

    @Override
    public ServiceComputation<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return new FutureBasedServiceComputation<>(executor, future.exceptionally(fn));
    }

    @Override
    public <U> ServiceComputation<U> thenApply(Function<? super T, ? extends U> fn) {
        return new FutureBasedServiceComputation<>(executor, future.thenApplyAsync(fn, executor));
    }

    @Override
    public <U> ServiceComputation<U> thenCompose(Function<? super T, ? extends ServiceComputation<U>> fn) {
        return new FutureBasedServiceComputation<>(executor, future.thenCompose(t -> CompletableFuture.supplyAsync(() -> fn.apply(t).get(), executor)));
    }

    @Override
    public ServiceComputation<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return new FutureBasedServiceComputation<T>(executor, future.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> ServiceComputation<U> thenCombineAll(List<ServiceComputation<?>> otherComputations, BiFunction<? super T, List<?>, ? extends U> fn) {
        CompletableFuture newFuture = CompletableFuture.supplyAsync(() -> {
            List<Object> otherResults = otherComputations.stream()
                    .map(ServiceComputation::get)
                    .collect(Collectors.toList());
            return fn.apply(this.get(), otherResults);
        }, executor);
        return new FutureBasedServiceComputation<>(executor, newFuture);
    }

}
