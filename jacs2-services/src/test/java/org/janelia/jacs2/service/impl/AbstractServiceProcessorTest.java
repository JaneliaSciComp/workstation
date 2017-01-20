package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractServiceProcessorTest {

    private static final String TEST_WORKING_DIR = "testDir";
    private static final Number TEST_ID = 1L;

    static class TestSuccessfulProcessor extends AbstractServiceProcessor<Void> {

        public TestSuccessfulProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                       ServiceComputationFactory computationFactory,
                                       JacsServiceDataPersistence jacsServiceDataPersistence,
                                       String defaultWorkingDir,
                                       Logger logger) {
            super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        protected ServiceComputation<Void> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData) {
            return computationFactory.newCompletedComputation(null);
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

        public TestFailedProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                   ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   String defaultWorkingDir,
                                   Logger logger) {
            super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        }

        @Override
        protected ServiceComputation<Void> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "test"));
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

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceDispatcher jacsServiceDispatcher;
    private ServiceComputationFactory serviceComputationFactory;
    private Logger logger;

    private TestSuccessfulProcessor testSuccessfullProcessor;
    private TestFailedProcessor testFailedProcessor;

    private JacsServiceData testJacsServiceData;

    @Before
    public void setUp() {
        ExecutorService executor = mock(ExecutorService.class);

        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        });

        serviceComputationFactory = new ServiceComputationFactory(executor);

        testJacsServiceData = new JacsServiceData();
        testJacsServiceData.setId(TEST_ID);
        jacsServiceDispatcher = mock(JacsServiceDispatcher.class);
        serviceComputationFactory = new ServiceComputationFactory(executor);
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);

        logger = mock(Logger.class);
        testSuccessfullProcessor = new TestSuccessfulProcessor(
                jacsServiceDispatcher,
                serviceComputationFactory,
                jacsServiceDataPersistence,
                TEST_WORKING_DIR,
                logger);
        testFailedProcessor = new TestFailedProcessor(
            jacsServiceDispatcher,
                serviceComputationFactory,
                jacsServiceDataPersistence,
                TEST_WORKING_DIR,
                logger);
    }

    @Test
    public void testSuccessfulProcessing() {
        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);

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
    public void testFailedProcessing() throws ComputationException {
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
