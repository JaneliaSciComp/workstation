package org.janelia.jacs2.asyncservice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionException;
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

    private static <U> U waitForResult(ServiceComputation<U> computation) {
        for (;;) {
            if (computation.isDone()) {
                return computation.get();
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new SuspendedException(e);
            }
        }
    }

    private final ServiceComputationQueue computationQueue;
    private final Logger logger;
    private final ServiceComputationTask<T> task;

    FutureBasedServiceComputation(ServiceComputationQueue computationQueue, Logger logger, ServiceComputationTask<T> task) {
        this.computationQueue = computationQueue;
        this.logger = logger;
        this.task = task;
    }

    FutureBasedServiceComputation(ServiceComputationQueue computationQueue, Logger logger) {
        this(computationQueue, logger, new ServiceComputationTask<>(null));
    }

    FutureBasedServiceComputation(ServiceComputationQueue computationQueue, Logger logger, T result) {
        this(computationQueue, logger, new ServiceComputationTask<>(null, result));
    }

    FutureBasedServiceComputation(ServiceComputationQueue computationQueue, Logger logger, Throwable exc) {
        this(computationQueue, logger, new ServiceComputationTask<>(null, exc));
    }

    @Override
    public T get() {
        ServiceComputationTask.ComputeResult<T> result = task.get();
        if (result.exc != null) {
            if (result.exc instanceof ComputationException) {
                throw (ComputationException) result.exc;
            } else if (result.exc instanceof SuspendedException) {
                throw (SuspendedException) result.exc;
            } else if (result.exc instanceof CompletionException) {
                throw (CompletionException) result.exc;
            } else {
                throw new CompletionException(result.exc);
            }
        }
        return result.result;
    }

    @Override
    public boolean isDone() {
        return task.isDone();
    }

    @Override
    public boolean isCompletedExceptionally() {
        return task.isCompletedExceptionally();
    }

    @Override
    public boolean isSuspended() {
        return task.isSuspended();
    }

    public void complete(T result) {
        task.complete(result);
    }

    public void completeExceptionally(Throwable exc) {
        task.completeExceptionally(exc);
    }

    @Override
    public ServiceComputation<T> supply(Supplier<T> fn) {
        submit((() -> fn.get()));
        return this;
    }

    @Override
    public ServiceComputation<T> exceptionally(Function<Throwable, ? extends T> fn) {
        FutureBasedServiceComputation<T> next = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(this));
        next.submit(() -> {
            try {
                T r = waitForResult(this);
                next.complete(r);
            } catch (Exception e) {
                next.complete(fn.apply(e));
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <U> ServiceComputation<U> thenApply(Function<? super T, ? extends U> fn) {
        FutureBasedServiceComputation<U> next = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(this));
        next.submit(() -> {
            try {
                T r = waitForResult(this);
                next.complete(fn.apply(r));
            } catch (Exception e) {
                next.completeExceptionally(e);
            }
            return next.get();
         });
        return next;
    }

    @Override
    public <U> ServiceComputation<U> thenCompose(Function<? super T, ? extends ServiceComputation<U>> fn) {
        FutureBasedServiceComputation<ServiceComputation<U>> nextStage = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(this));
        FutureBasedServiceComputation<U> next = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(nextStage));
        nextStage.submit(() -> {
            try {
                T r = waitForResult(this);
                nextStage.complete(fn.apply(r));
            } catch (Exception e) {
                nextStage.completeExceptionally(e);
            }
            return nextStage.get();
        });
        next.submit(() -> {
            try {
                ServiceComputation<U> computation = waitForResult(nextStage);
                U result = waitForResult(computation);
                next.complete(result);
            } catch (Exception e) {
                next.completeExceptionally(e);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public ServiceComputation<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        FutureBasedServiceComputation<T> next = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(this));
        next.submit(() -> {
            try {
                T r = waitForResult(this);
                action.accept(r, null);
                next.complete(r);
            } catch (Exception e) {
                action.accept(null, e);
                next.completeExceptionally(e);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <U, V> ServiceComputation<V> thenCombine(ServiceComputation<U> otherComputation, BiFunction<? super T, ? super U, ? extends V> fn) {
        ServiceComputationTask<V> nextTask = new ServiceComputationTask<>(this);
        nextTask.push(otherComputation);
        FutureBasedServiceComputation<V> next = new FutureBasedServiceComputation<>(computationQueue, logger, nextTask);
        next.submit(() -> {
            try {
                T r = waitForResult(this);
                U u = waitForResult(otherComputation);
                next.complete(fn.apply(r, u));
            } catch (Exception e) {
                next.completeExceptionally(e);
            }
            return next.get();
        });
        return next;
    }

    @Override
    public <U> ServiceComputation<U> thenCombineAll(List<ServiceComputation<?>> otherComputations, BiFunction<? super T, List<?>, ? extends U> fn) {
        ServiceComputationTask<U> nextTask = new ServiceComputationTask<>(this);
        otherComputations.forEach(nextTask::push);
        FutureBasedServiceComputation<U> next = new FutureBasedServiceComputation<>(computationQueue, logger, nextTask);
        next.submit(() -> {
            try {
                T r = waitForResult(this);
                List<Object> otherResults = otherComputations.stream()
                        .map(ServiceComputation::get)
                        .collect(Collectors.toList());
                next.complete(fn.apply(r, otherResults));
            } catch (Exception e) {
                next.completeExceptionally(e);
            }
            return next.get();
        });
        return next;
    }

    public ServiceComputation<T> thenSuspendUntil(ContinuationCond fn) {
        FutureBasedServiceComputation<Boolean> waitFor = new FutureBasedServiceComputation<>(computationQueue, logger, new ServiceComputationTask<>(this));
        ServiceComputationTask<T> nextTask = new ServiceComputationTask<T>(waitFor);
        FutureBasedServiceComputation<T> next = new FutureBasedServiceComputation<>(computationQueue, logger, nextTask);
        waitFor.submit(() -> {
            if (fn.checkCond()) {
                logger.debug("Resume {}", nextTask);
                nextTask.resume();
                waitFor.complete(true);
                return true;
            } else {
                logger.debug("Suspend {}", nextTask);
                nextTask.suspend();
                return false;
            }
        });
        next.submit(() -> {
            try {
                waitForResult(waitFor);
                T r = waitForResult(this);
                next.complete(r);
            } catch (Exception e) {
                next.completeExceptionally(e);
            }
            return next.get();
        });
        return next;
    }

    private void submit(ServiceComputationTask.ContinuationSupplier<T> fn) {
        task.setResultSupplier(fn);
        computationQueue.submit(task);
    }

}
