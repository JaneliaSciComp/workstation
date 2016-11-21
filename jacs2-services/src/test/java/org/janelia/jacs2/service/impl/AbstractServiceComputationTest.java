package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Instance;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractServiceComputationTest {

    private static class TestComputation extends AbstractServiceComputation {

        @Override
        protected TaskInfo doWork(TaskInfo si) {
            return si;
        }
    }

    @Mock
    private Logger logger;
    @Mock
    private Instance<TaskInfoPersistence> serviceInfoPersistenceSource;
    @Mock
    private TaskInfoPersistence taskInfoPersistence;
    @Spy
    private ManagedExecutorService managedExecutorService;

    @InjectMocks
    private TestComputation testComputation;
    private TaskInfo testTaskInfo;

    @Before
    public void setUp() {
        testTaskInfo = new TaskInfo();
        testComputation = new TestComputation();
        ExecutorService executorService = Executors.newCachedThreadPool();
        managedExecutorService = new ManagedExecutorService() {
            @Override
            public void shutdown() {
                executorService.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return executorService.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return executorService.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return executorService.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return executorService.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return executorService.submit(task);
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return executorService.submit(task, result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return executorService.submit(task);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return executorService.invokeAll(tasks);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return executorService.invokeAll(tasks, timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return executorService.invokeAny(tasks);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return executorService.invokeAny(tasks, timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                executorService.execute(command);
            }
        };
        MockitoAnnotations.initMocks(this);
        when(serviceInfoPersistenceSource.get()).thenReturn(taskInfoPersistence);
    }

    @Test
    public void testSuccessfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        CompletionStage<TaskInfo> computation = CompletableFuture.supplyAsync(() -> {
            testComputation.getReadyChannel().put(testTaskInfo);
            return testTaskInfo;
        })
        .thenCompose(si -> testComputation.processData());

        computation
            .thenAccept(successful)
            .exceptionally(ex -> {
                failure.accept(ex);
                return null;
            })
            .toCompletableFuture()
            .join();
        verify(failure, never()).accept(any());
        verify(successful).accept(any());
    }

    @Test
    public void testFailedProcessing() throws ComputationException {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        AbstractServiceComputation spyComputation = spy(testComputation);
        when(spyComputation.doWork(any(TaskInfo.class))).thenThrow(new IllegalStateException("test"));

        CompletionStage<TaskInfo> computation = CompletableFuture.supplyAsync(() -> {
            testComputation.getReadyChannel().put(testTaskInfo);
            return testTaskInfo;
        })
        .thenCompose(si -> {
            return spyComputation.processData();
        });
        computation
                .thenAccept(successful)
                .exceptionally(ex -> {
                    failure.accept(ex);
                    return null;
                })
                .toCompletableFuture()
                .join();
        verify(successful, never()).accept(any());
        verify(failure).accept(any());
    }
}
