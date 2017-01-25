package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacsServiceDispatcherTest {

    private static final Long TEST_ID = 101L;

    private ServiceComputationFactory serviceComputationFactory;
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private Instance<ServiceRegistry> serviceRegistrarSource;
    private ServiceRegistry serviceRegistry;
    private Logger logger;
    private JacsServiceDispatcher testDispatcher;

    @Before
    public void setUp() {
        ExecutorService executor = mock(ExecutorService.class);

        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        });

        serviceComputationFactory = new ServiceComputationFactory(executor);
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        serviceRegistrarSource = mock(Instance.class);
        serviceRegistry = mock(ServiceRegistry.class);
        logger = mock(Logger.class);
        testDispatcher = new JacsServiceDispatcher(serviceComputationFactory, jacsServiceDataPersistence, serviceRegistrarSource, logger);
        when(serviceRegistrarSource.get()).thenReturn(serviceRegistry);
        Answer<Void> saveServiceData = invocation -> {
            JacsServiceData ti = invocation.getArgument(0);
            ti.setId(TEST_ID);
            return null;
        };
        doAnswer(saveServiceData).when(jacsServiceDataPersistence).saveHierarchy(any(JacsServiceData.class));
    }

    @Test
    public void serviceAsyncSubmit() {
        JacsServiceData serviceData = submitTestService("test");

        assertThat(serviceData.getId(), equalTo(TEST_ID));
    }

    private JacsServiceData createTestService(Long serviceId, String serviceName) {
        JacsServiceData testService = new JacsServiceData();
        testService.setId(serviceId);
        testService.setName(serviceName);
        return testService;
    }

    private JacsServiceData submitTestService(String serviceName) {
        JacsServiceData testService = createTestService(null, serviceName);
        return testDispatcher.submitServiceAsync(testService);
    }

    @Test
    public void dispatchServiceWhenNoSlotsAreAvailable() {
        testDispatcher.setAvailableSlots(0);
        submitTestService("test");
        testDispatcher.dispatchServices();
        verify(logger).info("No available processing slots");
    }

    @Test
    public void increaseNumberOfSlots() {
        int nSlots = 110;
        testDispatcher.setAvailableSlots(0);
        testDispatcher.setAvailableSlots(nSlots);
        ServerStats stats = testDispatcher.getServerStats();
        assertThat(stats.getAvailableSlots(), equalTo(nSlots));
    }

    @Test
    public void runSubmittedService() {
        JacsServiceData testServiceData = submitTestService("submittedService");

        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(new PageResult<>());

        verifyDispatch(testServiceData);
    }

    @Test
    public void runServiceFromPersistenceStore() {
        JacsServiceData testServiceData = createTestService(1L, "persistedService");

        PageResult<JacsServiceData> nonEmptyPageResult = new PageResult<>();
        nonEmptyPageResult.setResultList(ImmutableList.of(testServiceData));
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(nonEmptyPageResult)
                .thenReturn(new PageResult<>());

        verifyDispatch(testServiceData);
    }

    private static class ServiceSyncer implements Runnable {
        volatile boolean done = false;
        @Override
        public void run() {
            while (!done) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private void verifyDispatch(JacsServiceData testServiceData) {
        ServiceProcessor testProcessor = prepareServiceProcessor(testServiceData, null);
        testDispatcher.dispatchServices();
        verify(logger).debug("Dequeued service {}", testServiceData);
        ArgumentCaptor<JacsServiceData> jacsServiceArg = ArgumentCaptor.forClass(JacsServiceData.class);
        verify(testProcessor).process(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue());

        verify(jacsServiceDataPersistence, atLeast(2)).update(testServiceData);
        assertThat(testServiceData.getState(), equalTo(JacsServiceState.SUCCESSFUL));
    }

    private ServiceProcessor prepareServiceProcessor(JacsServiceData testServiceData, Exception exc) {
        ServiceDescriptor testDescriptor = mock(ServiceDescriptor.class);
        ServiceProcessor testProcessor = mock(ServiceProcessor.class);

        when(serviceRegistry.lookupService(testServiceData.getName())).thenReturn(testDescriptor);
        when(testDescriptor.createServiceProcessor()).thenReturn(testProcessor);

        if (exc == null) {
            when(testProcessor.process(any(JacsServiceData.class))).thenReturn(serviceComputationFactory.newCompletedComputation(null));
        } else {
            when(testProcessor.process(any(JacsServiceData.class))).thenReturn(serviceComputationFactory.newFailedComputation(exc));
        }
        return testProcessor;
    }

    @Test
    public void serviceProcessingError() {
        JacsServiceData testServiceData = submitTestService("submittedService");
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(new PageResult<>());
        ComputationException processException = new ComputationException(testServiceData, "test exception");
        ServiceProcessor testProcessor = prepareServiceProcessor(testServiceData, processException);

        testDispatcher.dispatchServices();
        verify(logger).debug("Dequeued service {}", testServiceData);

        ArgumentCaptor<JacsServiceData> jacsServiceArg = ArgumentCaptor.forClass(JacsServiceData.class);
        verify(testProcessor).process(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue());
        verify(jacsServiceDataPersistence, times(3)).update(testServiceData);
        assertThat(testServiceData.getState(), equalTo(JacsServiceState.ERROR));
    }

    @Test
    public void syncServiceQueue() {
        PageResult<JacsServiceData> serviceDataPageResult = new PageResult<>();
        List<JacsServiceData> serviceResults = ImmutableList.<JacsServiceData>builder()
                .add(createTestService(1L, "t1"))
                .add(createTestService(2L, "t2"))
                .add(createTestService(3L, "t3"))
                .add(createTestService(4L, "t4"))
                .add(createTestService(5L, "t5"))
                .add(createTestService(6L, "t6"))
                .add(createTestService(7L, "t7"))
                .build();
        serviceDataPageResult.setResultList(serviceResults);
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class))).thenReturn(serviceDataPageResult);
        testDispatcher.syncServiceQueue();
        ServerStats stats = testDispatcher.getServerStats();
        assertThat(stats.getWaitingServices(), equalTo(serviceResults.size()));
    }
}
