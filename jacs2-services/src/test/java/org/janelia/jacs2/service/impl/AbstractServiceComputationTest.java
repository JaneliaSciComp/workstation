package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

    @InjectMocks
    private TestComputation testComputation;
    private TaskInfo testTaskInfo;

    @Before
    public void setUp() {
        testTaskInfo = new TaskInfo();
        testComputation = new TestComputation();
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
        .thenCompose(si -> {
            return testComputation.processData();
        });
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
