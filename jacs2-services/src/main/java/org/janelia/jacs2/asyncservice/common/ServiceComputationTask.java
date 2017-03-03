package org.janelia.jacs2.asyncservice.common;


import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

class ServiceComputationTask<T> implements Runnable {

    @FunctionalInterface
    static interface ContinuationSupplier<T> {
        T get();
    }

    static class ComputeResult<T> {
        final T result;
        final Throwable exc;

        public ComputeResult(T result, Throwable exc) {
            this.result = result;
            this.exc = exc;
        }
    }

    private static class Stack<E> {
        private static class Node <E> {
            public final E item;
            public Node<E> next;

            public Node(E item) {
                this.item = item;
            }
        }

        private AtomicReference<Node<E>> head = new AtomicReference<>();

        void push(E item) {
            Node<E> newHead = new Node<>(item);
            Node<E> oldHead;
            do {
                oldHead = head.get();
                newHead.next = oldHead;
            } while (!head.compareAndSet(oldHead, newHead));
        }

        E top() {
            Node<E> headContent = head.get();
            if (headContent == null) {
                return null;
            } else {
                return headContent.item;
            }
        }

        E pop() {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null) {
                    return null;
                }
                newHead = oldHead.next;
            } while (!head.compareAndSet(oldHead, newHead));
            return oldHead.item;
        }
    }

    private final CountDownLatch done = new CountDownLatch(1);
    private final Stack<ServiceComputation<?>> depStack = new Stack<>();
    private ContinuationSupplier<T> resultSupplier;
    private ComputeResult<T> result;
    private volatile boolean suspended;

    ServiceComputationTask(ServiceComputation<?> dep) {
        push(dep);
    }

    ServiceComputationTask(ServiceComputation<?> dep, T result) {
        this(dep);
        complete(result);
    }

    ServiceComputationTask(ServiceComputation<?> dep, Throwable exc) {
        this(dep);
        completeExceptionally(exc);
    }

    @Override
    public void run() {
        tryFire();
    }

    void push(ServiceComputation<?> dep) {
        if (dep != null) depStack.push(dep);
    }

    void setResultSupplier(ContinuationSupplier<T> resultSupplier) {
        this.resultSupplier = resultSupplier;
    }

    void tryFire() {
        if (isDone()) {
            return;
        }
        for (;;) {
            if (isReady() && !isSuspended()) {
                if (resultSupplier != null) {
                    try {
                        T r = resultSupplier.get();
                        if (isSuspended()) {
                            return;
                        }
                        complete(r);
                    } catch (Exception e) {
                        if (isSuspended()) {
                            return;
                        }
                        completeExceptionally(e);
                    }
                    return;
                } else {
                    throw new IllegalStateException("No result supplier has been provided");
                }
            } else if (suspended) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    boolean isReady() {
        for (ServiceComputation<?> dep = depStack.top(); ;) {
            if (dep == null) {
                return true;
            }
            if (dep.isDone()) {
                // the current dependency completed successfully - go to the next one
                depStack.pop();
                dep = depStack.top();
            } else {
                return false;
            }
        }
    }

    ComputeResult<T> get() {
        try {
            done.await();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return result;
    }

    boolean isDone() {
        return result != null;
    }

    boolean isCompletedExceptionally() {
        return isDone() && result.exc != null;
    }

    void complete(T result) {
        this.result = new ComputeResult<>(result, null);
        done.countDown();
    }

    void completeExceptionally(Throwable exc) {
        this.result = new ComputeResult<>(null, exc);
        done.countDown();
    }

    boolean isSuspended() {
        return suspended;
    }

    void suspend() {
        suspended = true;
    }

    void resume() {
        suspended = false;
    }
}
