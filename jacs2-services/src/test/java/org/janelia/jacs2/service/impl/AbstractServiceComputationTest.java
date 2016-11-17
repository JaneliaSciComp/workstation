package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.persistence.ServiceInfoPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
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
        protected void performComputation() {
        }
    }

    @Mock
    private Logger logger;
    @Mock
    private Instance<ServiceInfoPersistence> serviceInfoPersistenceSource;
    @Mock
    private ServiceInfoPersistence serviceInfoPersistence;

    @InjectMocks
    private TestComputation testComputation;
    private ServiceInfo testServiceInfo;

    @Before
    public void setUp() {
        testServiceInfo = new ServiceInfo();
        testComputation = new TestComputation();
        testComputation.setComputationInfo(testServiceInfo);
        MockitoAnnotations.initMocks(this);
        when(serviceInfoPersistenceSource.get()).thenReturn(serviceInfoPersistence);
    }

    @Test
    public void testSuccessfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        CompletionStage<ServiceComputation> computation = testComputation.processData();
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
    public void testFailedProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        AbstractServiceComputation spyComputation = spy(testComputation);
        doThrow(new IllegalStateException("test")).when(spyComputation).performComputation();

        CompletionStage<ServiceComputation> computation = spyComputation.processData();
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
