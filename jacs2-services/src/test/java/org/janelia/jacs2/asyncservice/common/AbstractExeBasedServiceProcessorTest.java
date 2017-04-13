package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractExeBasedServiceProcessorTest {

    private static final String TEST_WORKING_DIR = "testDir";
    private static final String TEST_EXE_DIR = "testToolsDir";

    private static class TestExternalProcessor extends AbstractExeBasedServiceProcessor<Void, Void> {

        public TestExternalProcessor(ServiceComputationFactory computationFactory,
                                     JacsServiceDataPersistence jacsServiceDataPersistence,
                                     Instance<ExternalProcessRunner> serviceRunners,
                                     String defaultWorkingDir,
                                     String executablesBaseDir,
                                     Logger logger) {
            super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
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
        protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
            return new ExternalCodeBlock();
        }

        @Override
        protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
            return Collections.emptyMap();
        }
    }

    private AbstractExeBasedServiceProcessor<Void, Void> testProcessor;
    private ExternalProcessRunner processRunner;

    @Before
    public void setUp() {
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
        Logger logger = mock(Logger.class);
        ServiceComputationFactory serviceComputationFactory = new ServiceComputationFactory(serviceComputationQueue, logger);
        JacsServiceDataPersistence jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        Instance<ExternalProcessRunner> serviceRunners = mock(Instance.class);
        processRunner = mock(ExternalProcessRunner.class);
        when(processRunner.supports(ProcessingLocation.LOCAL)).thenReturn(true);
        when(serviceRunners.iterator()).thenReturn(ImmutableList.of(processRunner).iterator());
        testProcessor = new TestExternalProcessor(
                            serviceComputationFactory,
                            jacsServiceDataPersistence,
                            serviceRunners,
                            TEST_WORKING_DIR,
                            TEST_EXE_DIR,
                            logger);
    }

    @Test
    public void processing() {
        JacsServiceData testServiceData = new JacsServiceData();
        testServiceData.setName("test");
        testServiceData.setProcessingLocation(ProcessingLocation.LOCAL);
        ExeJobInfo jobInfo = mock(ExeJobInfo.class);
        when(jobInfo.isDone()).thenReturn(true);
        when(processRunner.runCmds(any(ExternalCodeBlock.class), any(Map.class), any(String.class), any(JacsServiceData.class))).thenReturn(jobInfo);

        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        testProcessor.processing(new JacsServiceResult<>(testServiceData))
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
    public void processingError() {
        JacsServiceData testServiceData = new JacsServiceData();
        testServiceData.setName("test");
        testServiceData.setServiceTimeout(10L);
        testServiceData.setProcessingLocation(ProcessingLocation.LOCAL);
        ExeJobInfo jobInfo = mock(ExeJobInfo.class);
        when(jobInfo.isDone()).thenReturn(true);
        when(jobInfo.hasFailed()).thenReturn(true);
        when(processRunner.runCmds(any(ExternalCodeBlock.class), any(Map.class), any(String.class), any(JacsServiceData.class))).thenReturn(jobInfo);

        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        testProcessor.processing(new JacsServiceResult<>(testServiceData))
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
        JacsServiceData testServiceData = new JacsServiceData();
        testServiceData.setName("test");
        testServiceData.setServiceTimeout(10L);
        testServiceData.setProcessingLocation(ProcessingLocation.LOCAL);
        ExeJobInfo jobInfo = mock(ExeJobInfo.class);
        when(jobInfo.isDone()).thenReturn(false);
        when(processRunner.runCmds(any(ExternalCodeBlock.class), any(Map.class), any(String.class), any(JacsServiceData.class))).thenReturn(jobInfo);

        Consumer successful = mock(Consumer.class);
        Consumer failure = mock(Consumer.class);
        testProcessor.processing(new JacsServiceResult<>(testServiceData))
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
}
