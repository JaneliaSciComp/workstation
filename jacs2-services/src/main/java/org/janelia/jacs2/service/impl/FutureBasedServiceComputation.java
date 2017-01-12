package org.janelia.jacs2.service.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * ServiceComputationStage represents a certain service computation stage.
 *
 * @param <S> service type
 * @param <R> stage result type
 */
public class FutureBasedServiceComputation<S, R> implements ServiceComputation<S, R> {

    private static class ComputeResult<R> {
        private final R result;
        private final Throwable exc;

        public ComputeResult(R result, Throwable exc) {
            this.result = result;
            this.exc = exc;
        }
    }

    private final CountDownLatch done;
    private final ExecutorService executor;
    private final S service;
    private ComputeResult<R> result;

    FutureBasedServiceComputation(S service, ExecutorService executor) {
        this.executor = executor;
        this.service = service;
        this.done = new CountDownLatch(1);
    }

    @Override
    public S getService() {
        return service;
    }

    @Override
    public R get() {
        getResult();
        if (result.exc != null) {
            throw new IllegalStateException(result.exc);
        }
        return result.result;
    }

    private ComputeResult<R> getResult() {
        try {
            done.await();
        } catch (InterruptedException e) {
            completeExceptionally(e);
        }
        return result;
    }

    public void complete(R result) {
        this.result = new ComputeResult<>(result, null);
        done.countDown();
    }

    public void completeExceptionally(Throwable exc) {
        this.result = new ComputeResult<>(null, exc);
        done.countDown();
    }

    @Override
    public ServiceComputation<S, R> applyFunction(Function<S, ? extends R> fn) {
        executor.submit(() -> complete(fn.apply(service)));
        return this;
    }

    @Override
    public ServiceComputation<S, R> applyComputation(ServiceComputation<S, ? extends R> fn) {
        executor.submit(() -> complete(fn.get()));
        return this;
    }

    @Override
    public <S1, R1> ServiceComputation<S1, R1> thenApply(S1 s1, BiFunction<S1, ? super R, ? extends R1> fn) {
        FutureBasedServiceComputation<S1, R1> next = new FutureBasedServiceComputation<>(s1, executor);
        next.applyFunction(t -> {
            ComputeResult<R> r = getResult();
            if (r.exc == null) {
                next.complete(fn.apply(t, r.result));
            } else {
                next.completeExceptionally(r.exc);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <S1, R1> ServiceComputation<S1, R1> thenCompose(S1 s1, BiFunction<S1, ? super R, ServiceComputation<S1, ? extends R1>> fn) {
        FutureBasedServiceComputation<S1, R1> next = new FutureBasedServiceComputation<>(s1, executor);
        next.applyFunction(t -> {
            ComputeResult<R> r = getResult();
            if (r.exc == null) {
                ServiceComputation<S1, ? extends R1> stage = fn.apply(t, r.result);
                next.complete(stage.get());
            } else {
                next.completeExceptionally(r.exc);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public ServiceComputation<S, R> whenComplete(BiConsumer<? super R, ? super Throwable> action) {
        FutureBasedServiceComputation<S, R> next = new FutureBasedServiceComputation<>(service, executor);
        next.applyFunction(t -> {
            ComputeResult<R> r = getResult();
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
}
