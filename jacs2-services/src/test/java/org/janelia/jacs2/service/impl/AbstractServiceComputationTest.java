package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
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

    static class TestSuccessfulComputation extends AbstractServiceComputation<Void> {

        @Override
        public CompletionStage<JacsService<Void>> processData(JacsService<Void> js) {
            CompletableFuture<JacsService<Void>> completableFuture = new CompletableFuture<>();
            completableFuture.complete(js);
            return completableFuture;
        }
    }

    static class TestFailedComputation extends AbstractServiceComputation<Void> {

        @Override
        public CompletionStage<JacsService<Void>> processData(JacsService<Void> js) {
            CompletableFuture<JacsService<Void>> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(new ComputationException(js, "test"));
            return completableFuture;
        }
    }

    @Mock
    private Logger logger;
    @Mock
    private Instance<JacsServiceDataPersistence> serviceInfoPersistenceSource;
    @Mock
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    @Spy
    private Executor serviceExecutor;

    @InjectMocks
    private TestSuccessfulComputation testSuccessfullComputation;
    @InjectMocks
    private TestFailedComputation testFailedComputation;

    private JacsServiceData testJacsServiceData;

    @Before
    public void setUp() {
        testJacsServiceData = new JacsServiceData();
        serviceExecutor = Executors.newCachedThreadPool();
        MockitoAnnotations.initMocks(this);
        when(serviceInfoPersistenceSource.get()).thenReturn(jacsServiceDataPersistence);
    }

    @Test
    public void testSuccessfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        CompletionStage<JacsService<Void>> computation =
                CompletableFuture
                    .supplyAsync(() -> new JacsService<Void>(null, testJacsServiceData), serviceExecutor)
                    .thenComposeAsync(js -> testSuccessfullComputation.processData(js), serviceExecutor);
        computation
            .thenAcceptAsync(successful, serviceExecutor)
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
        CompletionStage<JacsService<Void>> computation =
                CompletableFuture
                        .supplyAsync(() -> new JacsService<Void>(null, testJacsServiceData), serviceExecutor)
                        .thenComposeAsync(js -> testFailedComputation.processData(js), serviceExecutor);
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
