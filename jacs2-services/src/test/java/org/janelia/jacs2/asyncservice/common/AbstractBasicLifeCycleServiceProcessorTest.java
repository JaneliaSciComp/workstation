package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractBasicLifeCycleServiceProcessorTest {

    private static final String TEST_WORKING_DIR = "testDir";
    private static final Number TEST_ID = 1L;

    static class TestSuccessfulProcessorBasicLifeCycle extends AbstractBasicLifeCycleServiceProcessor<Void> {

        public TestSuccessfulProcessorBasicLifeCycle(ServiceComputationFactory computationFactory,
                                                     JacsServiceDataPersistence jacsServiceDataPersistence,
                                                     String defaultWorkingDir,
                                                     Logger logger) {
            super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        public ServiceMetaData getMetadata() {
            return new ServiceMetaData();
        }

        @Override
        public ServiceResultHandler<Void> getResultHandler() {
            return new VoidServiceResultHandler();
        }

        @Override
        protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
            return ImmutableList.of();
        }

        @Override
        protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
            return computationFactory.newCompletedComputation(jacsServiceData);
        }
    }

    static class TestFailedProcessorBasicLifeCycle extends AbstractBasicLifeCycleServiceProcessor<Void> {

        public TestFailedProcessorBasicLifeCycle(ServiceComputationFactory computationFactory,
                                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                                 String defaultWorkingDir,
                                                 Logger logger) {
            super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        public ServiceMetaData getMetadata() {
            return new ServiceMetaData();
        }

        @Override
        public ServiceResultHandler<Void> getResultHandler() {
            return new VoidServiceResultHandler();
        }

        @Override
        protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData));
        }

    }

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private ServiceComputationFactory serviceComputationFactory;
    private Logger logger;

    private TestSuccessfulProcessorBasicLifeCycle testSuccessfullProcessor;
    private TestFailedProcessorBasicLifeCycle testFailedProcessor;

    private JacsServiceData testJacsServiceData;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
        ServiceComputationQueue serviceComputationQueue = mock(ServiceComputationQueue.class);
        doAnswer(invocation -> {
            ServiceComputationTask task = invocation.getArgument(0);
            if (task != null) {
                for (;;) {
                    ServiceComputationQueue.runTask(task);
                    if (task.isDone()) {
                        break;
                    }
                    Thread.sleep(10L);
                }
            }
            return null;
        }).when(serviceComputationQueue).submit(any(ServiceComputationTask.class));
        serviceComputationFactory = new ServiceComputationFactory(serviceComputationQueue, logger);

        testJacsServiceData = new JacsServiceData();
        testJacsServiceData.setName("test");
        testJacsServiceData.setId(TEST_ID);
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);

        testSuccessfullProcessor = new TestSuccessfulProcessorBasicLifeCycle(
                serviceComputationFactory,
                jacsServiceDataPersistence,
                TEST_WORKING_DIR,
                logger);
        testFailedProcessor = new TestFailedProcessorBasicLifeCycle(
                serviceComputationFactory,
                jacsServiceDataPersistence,
                TEST_WORKING_DIR,
                logger);
    }

    @Test
    public void successfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        JacsServiceData testJacsServiceDataDependency = new JacsServiceData();
        testJacsServiceDataDependency.setId(TEST_ID.longValue() + 1);

        testJacsServiceData.addServiceDependency(testJacsServiceDataDependency);
        when(jacsServiceDataPersistence.findById(TEST_ID)).thenReturn(testJacsServiceData);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenAnswer(invocation -> {
                    testJacsServiceDataDependency.setState(JacsServiceState.SUCCESSFUL);
                    return testJacsServiceData;
                });

        testSuccessfullProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                   if (e == null) {
                       successful.accept(r);
                   } else {
                       failure.accept(e);
                   }
                });
        verify(failure, never()).accept(any());
        verify(successful).accept(any());
    }

    @Test
    public void processingFailureDueToDependencyFailure() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        JacsServiceData testJacsServiceDataDependency = new JacsServiceData();
        testJacsServiceDataDependency.setId(TEST_ID.longValue() + 1);

        testJacsServiceData.addServiceDependency(testJacsServiceDataDependency);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenAnswer(invocation -> {
                    testJacsServiceDataDependency.setState(JacsServiceState.CANCELED);
                    return testJacsServiceData;
                });

        testSuccessfullProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                    if (e == null) {
                        successful.accept(r);
                    } else {
                        failure.accept(e);
                    }
                });
        verify(failure).accept(any());
        verify(successful, never()).accept(any());
    }

    @Test
    public void processingSuspended() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        JacsServiceData testJacsServiceDataDependency = new JacsServiceData();
        testJacsServiceDataDependency.setId(TEST_ID.longValue() + 1);
        testJacsServiceDataDependency.setState(JacsServiceState.RUNNING);

        testJacsServiceData.addServiceDependency(testJacsServiceDataDependency);

        when(jacsServiceDataPersistence.findById(TEST_ID)).thenReturn(testJacsServiceData);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenReturn(testJacsServiceData)
                .thenAnswer(invocation -> {
                    testJacsServiceDataDependency.setState(JacsServiceState.SUCCESSFUL);
                    return testJacsServiceData;
                });

        testSuccessfullProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                    if (e == null) {
                        successful.accept(r);
                    } else {
                        failure.accept(e);
                    }
                });
        verify(successful).accept(any());
        verify(failure, never()).accept(any());
        assertThat(testJacsServiceData.getState(), equalTo(JacsServiceState.SUCCESSFUL));
    }

    @Test
    public void processingTimeout() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        JacsServiceData testJacsServiceDataDependency = new JacsServiceData();
        testJacsServiceDataDependency.setId(TEST_ID.longValue() + 1);
        testJacsServiceDataDependency.setState(JacsServiceState.RUNNING);

        testJacsServiceData.addServiceDependency(testJacsServiceDataDependency);
        testJacsServiceData.setServiceTimeout(1L);

        when(jacsServiceDataPersistence.findById(TEST_ID)).thenReturn(testJacsServiceData);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenReturn(testJacsServiceData);

        testSuccessfullProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                    if (e == null) {
                        successful.accept(r);
                    } else {
                        failure.accept(e);
                    }
                });
        verify(successful, never()).accept(any());
        verify(failure).accept(any());
        assertThat(testJacsServiceData.getState(), equalTo(JacsServiceState.TIMEOUT));
    }

    @Test
    public void failedProcessing() throws ComputationException {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        testFailedProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                    if (e == null) {
                        successful.accept(r);
                    } else {
                        failure.accept(e);
                    }
                });
        verify(successful, never()).accept(any());
        verify(failure).accept(any());
    }

}
