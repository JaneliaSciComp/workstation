package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractServiceProcessorTest {

    private static final String TEST_WORKING_DIR = "testDir";
    private static final Number TEST_ID = 1L;

    static class TestSuccessfulProcessor extends AbstractServiceProcessor<Void> {

        public TestSuccessfulProcessor(JacsServiceEngine jacsServiceEngine,
                                       ServiceComputationFactory computationFactory,
                                       JacsServiceDataPersistence jacsServiceDataPersistence,
                                       String defaultWorkingDir,
                                       Logger logger) {
            super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        protected ServiceComputation<Void> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData) {
            return computationFactory.newCompletedComputation(null);
        }

        @Override
        public ServiceMetaData getMetadata() {
            return new ServiceMetaData();
        }

        @Override
        public Void getResult(JacsServiceData jacsServiceData) {
            return null;
        }

        @Override
        public void setResult(Void result, JacsServiceData jacsServiceData) {
        }

        @Override
        protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
            return true;
        }

        @Override
        protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
            return null;
        }
    }

    static class TestFailedProcessor extends AbstractServiceProcessor<Void> {

        public TestFailedProcessor(JacsServiceEngine jacsServiceEngine,
                                   ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   String defaultWorkingDir,
                                   Logger logger) {
            super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        protected ServiceComputation<Void> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "test"));
        }

        @Override
        public ServiceMetaData getMetadata() {
            return new ServiceMetaData();
        }

        @Override
        public Void getResult(JacsServiceData jacsServiceData) {
            return null;
        }

        @Override
        public void setResult(Void result, JacsServiceData jacsServiceData) {
        }

        @Override
        protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
            return false;
        }

        @Override
        protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
            return null;
        }
    }

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceEngine jacsServiceEngine;
    private ServiceComputationFactory serviceComputationFactory;
    private Logger logger;

    private TestSuccessfulProcessor testSuccessfullProcessor;
    private TestFailedProcessor testFailedProcessor;

    private JacsServiceData testJacsServiceData;

    @Before
    public void setUp() {
        ExecutorService executor = mock(ExecutorService.class);
        ExecutorService suspendExecutor = mock(ExecutorService.class);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            Thread.sleep(1000);
            return null;
        }).when(suspendExecutor).execute(any(Runnable.class));
        serviceComputationFactory = new ServiceComputationFactory(executor, suspendExecutor);

        testJacsServiceData = new JacsServiceData();
        testJacsServiceData.setId(TEST_ID);
        jacsServiceEngine = mock(JacsServiceEngine.class);
        serviceComputationFactory = new ServiceComputationFactory(executor, executor);
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);

        logger = mock(Logger.class);
        testSuccessfullProcessor = new TestSuccessfulProcessor(
                jacsServiceEngine,
                serviceComputationFactory,
                jacsServiceDataPersistence,
                TEST_WORKING_DIR,
                logger);
        testFailedProcessor = new TestFailedProcessor(
            jacsServiceEngine,
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
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenAnswer(invocation -> testJacsServiceData)
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
    public void processingTimeout() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        JacsServiceData testJacsServiceDataDependency = new JacsServiceData();
        testJacsServiceDataDependency.setId(TEST_ID.longValue() + 1);
        testJacsServiceDataDependency.setState(JacsServiceState.RUNNING);

        testJacsServiceData.setServiceTimeout(100L);
        testJacsServiceData.addServiceDependency(testJacsServiceDataDependency);

        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID))
                .thenAnswer(invocation -> testJacsServiceData);

        testSuccessfullProcessor.process(testJacsServiceData)
                .whenComplete((r, e) -> {
                    if (e == null) {
                        successful.accept(r);
                    } else {
                        e.printStackTrace(System.out);
                        failure.accept(e);
                    }
                });
        verify(failure).accept(any());
        verify(successful, never()).accept(any());
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

    @Test
    public void waitAndCompleteSuccessfully() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        when(jacsServiceDataPersistence.findById(TEST_ID))
                .thenAnswer(invocation -> testJacsServiceData)
                .thenAnswer(invocation -> {
                    testJacsServiceData.setState(JacsServiceState.SUCCESSFUL);
                    return testJacsServiceData;
                });

        when(jacsServiceEngine.getServiceProcessor(testJacsServiceData)).thenReturn((ServiceProcessor) testSuccessfullProcessor);

        testSuccessfullProcessor.waitForCompletion(testJacsServiceData)
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
    public void waitAndFail() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        ExecutorService executor = mock(ExecutorService.class);
        ExecutorService suspendExecutor = mock(ExecutorService.class);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(suspendExecutor).execute(any(Runnable.class));
        serviceComputationFactory = new ServiceComputationFactory(executor, suspendExecutor);

        when(jacsServiceDataPersistence.findById(TEST_ID))
                .thenAnswer(invocation -> testJacsServiceData)
                .thenAnswer(invocation -> {
                    testJacsServiceData.setState(JacsServiceState.ERROR);
                    return testJacsServiceData;
                });

        when(jacsServiceEngine.getServiceProcessor(testJacsServiceData)).thenReturn((ServiceProcessor) testSuccessfullProcessor);

        testSuccessfullProcessor.waitForCompletion(testJacsServiceData)
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

    @Test
    public void waitAndTimeout() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        testJacsServiceData.setServiceTimeout(100L);
        when(jacsServiceDataPersistence.findById(TEST_ID)).thenReturn(testJacsServiceData);

        when(jacsServiceEngine.getServiceProcessor(testJacsServiceData)).thenReturn((ServiceProcessor) testSuccessfullProcessor);

        testSuccessfullProcessor.waitForCompletion(testJacsServiceData)
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
    public void collectResultSuccessfully() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        testSuccessfullProcessor.collectResult(null, testJacsServiceData)
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
    public void collectResultTimeout() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

        testJacsServiceData.setServiceTimeout(100L);

        testFailedProcessor.collectResult(null, testJacsServiceData)
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
}
