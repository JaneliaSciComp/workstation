package org.janelia.jacs2.asyncservice.common;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * FutureBasedServiceComputation is an implementation of a ServiceComputation.
 *
 * @param <T> result type
 */
public class FutureBasedServiceComputation<T> implements ServiceComputation<T> {

    private final ExecutorService executor;
    private final ExecutorService suspendedExecutor;
    private CompletableFuture<T> future;

    FutureBasedServiceComputation(ExecutorService executor, ExecutorService suspendedExecutor) {
        this.executor = executor;
        this.suspendedExecutor = suspendedExecutor;
    }

    FutureBasedServiceComputation(ExecutorService executor, ExecutorService suspendedExecutor, T result) {
        this.executor = executor;
        this.suspendedExecutor = suspendedExecutor;
        future = CompletableFuture.completedFuture(result);
    }

    FutureBasedServiceComputation(ExecutorService executor, ExecutorService suspendedExecutor, Throwable exc) {
        this.executor = executor;
        this.suspendedExecutor = suspendedExecutor;
        future = new CompletableFuture<>();
        future.completeExceptionally(exc);
    }

    private FutureBasedServiceComputation(ExecutorService executor, ExecutorService suspendedExecutor, CompletableFuture<T> future) {
        this.executor = executor;
        this.suspendedExecutor = suspendedExecutor;
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
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor, future.exceptionally(fn));
    }

    @Override
    public <U> ServiceComputation<U> thenApply(Function<? super T, ? extends U> fn) {
        CompletableFuture<U> newFuture = future.thenApplyAsync(fn, executor);
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor, newFuture);
    }

    @Override
    public <U> ServiceComputation<U> thenCompose(Function<? super T, ? extends ServiceComputation<U>> fn) {
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor,
                future.thenComposeAsync(t -> CompletableFuture.completedFuture(fn.apply(t).get()), executor));
    }

    @Override
    public ServiceComputation<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return new FutureBasedServiceComputation<T>(executor, suspendedExecutor, future.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> ServiceComputation<U> thenCombineAll(List<ServiceComputation<?>> otherComputations, BiFunction<? super T, List<?>, ? extends U> fn) {
        CompletableFuture newFuture = CompletableFuture.supplyAsync(() -> {
            List<Object> otherResults = otherComputations.stream()
                    .map(ServiceComputation::get)
                    .collect(Collectors.toList());
            return fn.apply(this.get(), otherResults);
        }, executor);
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor, newFuture);
    }

    @Override
    public <U> ServiceComputation<U> suspend(Predicate<? super T> condToCont, Function<? super T, ? extends U> fn) {
        Coroutine suspend = new Coroutine() {
            @Override
            public void run(Continuation continuation) throws Exception {
                for(;;) {
                    if (future.isDone() && condToCont.test(future.getNow(null))) {
                        break;
                    }
                    continuation.suspend();
                }
            }
        };
        CoroutineRunner runner = new CoroutineRunner(suspend);
        CompletableFuture<U> newFuture = CompletableFuture.supplyAsync(() -> {
            for(;;) {
                if (!runner.execute()) {
                    return fn.apply(future.getNow(null));
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, suspendedExecutor);
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor, newFuture);
    }

}
