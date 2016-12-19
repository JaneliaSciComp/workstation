package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.hamcrest.beans.HasPropertyWithValue;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacsServiceDispatcherTest {

    private static final Long TEST_ID = 101L;

    @Mock
    private Logger logger;
    @Spy
    private ExecutorService serviceExecutor;
    @Mock
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    @Mock
    private Instance<ServiceRegistry> serviceRegistrarSource;
    @Mock
    private ServiceRegistry serviceRegistry;
    @InjectMocks
    private JacsServiceDispatcher testDispatcher;

    @Before
    public void setUp() {
        testDispatcher = new JacsServiceDispatcher();
        serviceExecutor = Executors.newFixedThreadPool(25);
        MockitoAnnotations.initMocks(this);
        when(serviceRegistrarSource.get()).thenReturn(serviceRegistry);
        Answer<Void> saveServiceData = invocation -> {
            JacsServiceData ti = invocation.getArgument(0);
            ti.setId(TEST_ID);
            return null;
        };
        doAnswer(saveServiceData).when(jacsServiceDataPersistence).save(any(JacsServiceData.class));
    }

    @Test
    public void mainServiceAsyncSubmit() {
        JacsServiceData serviceData = submitTestService("test", null);

        assertThat(serviceData.getId(), equalTo(TEST_ID));
    }

    private JacsServiceData createTestService(Long serviceId, String serviceName) {
        JacsServiceData testService = new JacsServiceData();
        testService.setId(serviceId);
        testService.setName(serviceName);
        return testService;
    }

    private JacsServiceData submitTestService(String serviceName, JacsServiceData testParentService) {
        JacsServiceData testService = createTestService(null, serviceName);
        return testDispatcher.submitServiceAsync(testService, testParentService == null ? Optional.<JacsServiceData>empty() : Optional.of(testParentService));
    }

    @Test
    public void childServiceAsyncSubmit() {
        JacsServiceData mainServiceData = new JacsServiceData();
        mainServiceData.setId(1L);
        mainServiceData.setName("main");

        JacsServiceData childServiceData = submitTestService("test", mainServiceData);

        assertThat(childServiceData.getId(), equalTo(TEST_ID));
        assertThat(childServiceData.getParentServiceId(), equalTo(mainServiceData.getId()));
    }

    @Test
    public void dispatchServiceWhenNoSlotsAreAvailable() {
        testDispatcher.setAvailableSlots(0);
        submitTestService("test", null);
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
        JacsServiceData testServiceData = submitTestService("submittedService", null);

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
        CompletionStage<JacsService> process = CompletableFuture.completedFuture(new JacsService(testDispatcher, testServiceData));
        ServiceSyncer done = new ServiceSyncer();
        Thread joiner = new Thread(done);
        joiner.start();

        Answer<Void> doneAnswer = invocation -> {
            done.done = true;
            return null;
        };

        ServiceComputation testComputation = prepareComputations(testServiceData, null, process, doneAnswer);

        testDispatcher.dispatchServices();
        try {
            joiner.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        verify(logger).info("Dequeued service {}", testServiceData);
        ArgumentCaptor<JacsService> jacsServiceArg = ArgumentCaptor.forClass(JacsService.class);
        verify(testComputation).preProcessData(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation).isReadyToProcess(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation).processData(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation).isDone(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(jacsServiceDataPersistence, atLeast(2)).update(testServiceData);
        assertThat(testServiceData.getState(), equalTo(JacsServiceState.SUCCESSFUL));
    }

    private ServiceComputation prepareComputations(JacsServiceData testServiceData, Throwable exc, CompletionStage<JacsService> processingStage, Answer<Void> doneAnswer) {
        ServiceDescriptor testDescriptor = mock(ServiceDescriptor.class);
        ServiceComputation testComputation = mock(ServiceComputation.class);

        when(serviceRegistry.lookupService(testServiceData.getName())).thenReturn(testDescriptor);
        when(testDescriptor.createComputationInstance()).thenReturn(testComputation);

        when(testComputation.preProcessData(any(JacsService.class))).then(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        when(testComputation.isReadyToProcess(any(JacsService.class))).then(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        when(testComputation.processData(any(JacsService.class))).thenReturn(processingStage);
        when(testComputation.isDone(any(JacsService.class))).then(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));

        doAnswer(doneAnswer).when(testComputation).postProcessData(any(JacsService.class), exc != null ? any(Throwable.class) : isNull());
        return testComputation;
    }

    @Test
    public void serviceProcessingError() {
        JacsServiceData testServiceData = submitTestService("submittedService", null);
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(new PageResult<JacsServiceData>());
        ServiceSyncer done = new ServiceSyncer();
        Thread joiner = new Thread(done);
        joiner.start();

        JacsService processingService = new JacsService(testDispatcher, testServiceData);

        CompletableFuture<JacsService> process = new CompletableFuture<>();
        ComputationException processException = new ComputationException(processingService, "test exception");
        process.completeExceptionally(processException);

        Answer<Void> doneAnswer = invocation -> {
            done.done = true;
            return null;
        };

        ServiceComputation testComputation = prepareComputations(testServiceData, processException, process, doneAnswer);

        testDispatcher.dispatchServices();
        try {
            joiner.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        verify(logger).info("Dequeued service {}", testServiceData);

        ArgumentCaptor<JacsService> jacsServiceArg = ArgumentCaptor.forClass(JacsService.class);
        verify(testComputation).preProcessData(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation).isReadyToProcess(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation).processData(jacsServiceArg.capture());
        assertSame(testServiceData, jacsServiceArg.getValue().getJacsServiceData());

        verify(testComputation, never()).isDone(any(JacsService.class));
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
