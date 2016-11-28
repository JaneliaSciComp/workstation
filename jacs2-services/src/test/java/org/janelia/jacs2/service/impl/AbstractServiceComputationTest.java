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

import javax.enterprise.inject.Instance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractServiceComputationTest {

    static class TestSuccessfulComputation extends AbstractServiceComputation {

        @Override
        public CompletionStage<TaskInfo> processData(TaskInfo ti) {
            CompletableFuture<TaskInfo> completableFuture = new CompletableFuture<>();
            completableFuture.complete(ti);
            return completableFuture;
        }
    }

    static class TestFailedComputation extends AbstractServiceComputation {

        @Override
        public CompletionStage<TaskInfo> processData(TaskInfo ti) {
            CompletableFuture<TaskInfo> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(new IllegalStateException("test"));
            return completableFuture;
        }
    }

    @Mock
    private Logger logger;
    @Mock
    private Instance<TaskInfoPersistence> serviceInfoPersistenceSource;
    @Mock
    private TaskInfoPersistence taskInfoPersistence;
    @Spy
    private Executor taskExecutor;

    @InjectMocks
    private TestSuccessfulComputation testSuccessfullComputation;
    @InjectMocks
    private TestFailedComputation testFailedComputation;

    private TaskInfo testTaskInfo;

    @Before
    public void setUp() {
        testTaskInfo = new TaskInfo();
        taskExecutor = Executors.newCachedThreadPool();
        MockitoAnnotations.initMocks(this);
        when(serviceInfoPersistenceSource.get()).thenReturn(taskInfoPersistence);
    }

    @Test
    public void testSuccessfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        CompletionStage<TaskInfo> computation =
                CompletableFuture
                    .supplyAsync(() -> testTaskInfo, taskExecutor)
                    .thenComposeAsync(ti -> testSuccessfullComputation.processData(ti), taskExecutor);
        computation
            .thenAcceptAsync(successful, taskExecutor)
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
        CompletionStage<TaskInfo> computation =
                CompletableFuture
                        .supplyAsync(() -> testTaskInfo, taskExecutor)
                        .thenComposeAsync(ti -> testFailedComputation.processData(ti), taskExecutor);
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
