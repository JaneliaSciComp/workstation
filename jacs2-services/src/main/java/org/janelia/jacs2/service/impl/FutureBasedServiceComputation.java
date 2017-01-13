package org.janelia.jacs2.service.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * FutureBasedServiceComputation is an implementation of a ServiceComputation.
 *
 * @param <T> result type
 */
public class FutureBasedServiceComputation<T> implements ServiceComputation<T> {

    private static class ComputeResult<T> {
        private final T result;
        private final Throwable exc;

        public ComputeResult(T result, Throwable exc) {
            this.result = result;
            this.exc = exc;
        }
    }

    private final CountDownLatch done;
    private final ExecutorService executor;
    private ComputeResult<T> result;

    FutureBasedServiceComputation(ExecutorService executor) {
        this.executor = executor;
        this.done = new CountDownLatch(1);
    }

    FutureBasedServiceComputation(ExecutorService executor, T result) {
        this.executor = executor;
        this.done = new CountDownLatch(1);
        complete(result);
    }

    FutureBasedServiceComputation(ExecutorService executor, Throwable exc) {
        this.executor = executor;
        this.done = new CountDownLatch(1);
        completeExceptionally(exc);
    }

    @Override
    public T get() {
        getResult();
        if (result.exc != null) {
            throw new IllegalStateException(result.exc);
        }
        return result.result;
    }

    private ComputeResult<T> getResult() {
        try {
            done.await();
        } catch (InterruptedException e) {
            completeExceptionally(e);
        }
        return result;
    }

    @Override
    public boolean isDone() {
        return result != null;
    }

    @Override
    public boolean isCompletedExceptionally() {
        return isDone() && result.exc != null;
    }

    public void complete(T result) {
        this.result = new ComputeResult<>(result, null);
        done.countDown();
    }

    public void completeExceptionally(Throwable exc) {
        this.result = new ComputeResult<>(null, exc);
        done.countDown();
    }

    @Override
    public ServiceComputation<T> supply(Supplier<T> fn) {
        execute(fn);
        return this;
    }

    @Override
    public ServiceComputation<T> exceptionally(Function<Throwable, ? extends T> fn) {
        FutureBasedServiceComputation<T> next = new FutureBasedServiceComputation<>(executor);
        next.execute(() -> {
            ComputeResult<T> r = getResult();
            if (r.exc != null) {
                next.complete(fn.apply(r.exc));
            } else {
                next.complete(r.result);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <U> ServiceComputation<U> thenApply(Function<? super T, ? extends U> fn) {
        FutureBasedServiceComputation<U> next = new FutureBasedServiceComputation<>(executor);
        next.execute(() -> {
            ComputeResult<T> r = getResult();
            if (r.exc == null) {
                next.complete(fn.apply(r.result));
            } else {
                next.completeExceptionally(r.exc);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <U> ServiceComputation<U> thenCompose(Function<? super T, ? extends ServiceComputation<U>> fn) {
        FutureBasedServiceComputation<U> next = new FutureBasedServiceComputation<>(executor);
        next.execute(() -> {
            ComputeResult<T> r = getResult();
            if (r.exc == null) {
                ServiceComputation<U> stage = fn.apply(r.result);
                next.complete(stage.get());
            } else {
                next.completeExceptionally(r.exc);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public ServiceComputation<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        FutureBasedServiceComputation<T> next = new FutureBasedServiceComputation<>(executor);
        next.execute(() -> {
            ComputeResult<T> r = getResult();
            action.accept(r.result, r.exc);
            if (r.exc == null) {
                next.complete(r.result);
            } else {
                next.completeExceptionally(r.exc);
            }
            return next.get();
        });
        return next;
    }

    private void execute(Supplier<T> fn) {
        executor.submit(() -> {
            try {
                complete(fn.get());
            } catch (Exception e) {
                if (result == null || result.exc == null) {
                    completeExceptionally(e);
                }
            }
        });
    }
}
