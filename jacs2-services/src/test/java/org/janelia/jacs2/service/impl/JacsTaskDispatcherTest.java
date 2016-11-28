package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
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
import static org.hamcrest.MatcherAssert.assertThat;
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

public class JacsTaskDispatcherTest {

    private static final Long TEST_ID = 101L;

    @Mock
    private Logger logger;
    @Spy
    private ExecutorService taskExecutor;
    @Mock
    private TaskInfoPersistence taskInfoPersistence;
    @Mock
    private Instance<ServiceRegistry> serviceRegistrarSource;
    @Mock
    private ServiceRegistry serviceRegistry;
    @InjectMocks
    private JacsTaskDispatcher testDispatcher;

    @Before
    public void setUp() {
        taskExecutor = Executors.newFixedThreadPool(25);
        MockitoAnnotations.initMocks(this);
        when(serviceRegistrarSource.get()).thenReturn(serviceRegistry);
        Answer<Void> taskInfoSave = invocation -> {
            TaskInfo ti = invocation.getArgument(0);
            ti.setId(TEST_ID);
            return null;
        };
        doAnswer(taskInfoSave).when(taskInfoPersistence).save(any(TaskInfo.class));
    }

    @Test
    public void mainTaskAsyncSubmit() {
        TaskInfo serviceTask = submitTestTask("test", null);

        assertThat(serviceTask.getId(), equalTo(TEST_ID));
    }

    private TaskInfo createTestTask(Long taskId, String taskName) {
        TaskInfo testTask = new TaskInfo();
        testTask.setId(taskId);
        testTask.setName(taskName);
        return testTask;
    }

    private TaskInfo submitTestTask(String taskName, TaskInfo testParentTask) {
        TaskInfo testTask = createTestTask(null, taskName);
        return testDispatcher.submitTaskAsync(testTask, testParentTask == null ? Optional.<TaskInfo>empty() : Optional.of(testParentTask));
    }

    @Test
    public void subTaskTaskAsyncSubmit() {
        TaskInfo mainTask = new TaskInfo();
        mainTask.setId(1L);
        mainTask.setName("main");

        TaskInfo serviceTask = submitTestTask("test", mainTask);

        assertThat(serviceTask.getId(), equalTo(TEST_ID));
        assertThat(serviceTask.getParentTaskId(), equalTo(mainTask.getId()));
    }

    @Test
    public void dispatchServiceWhenNoSlotsAreAvailable() {
        testDispatcher.setAvailableSlots(0);
        submitTestTask("test", null);
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
    public void runSubmittedTask() {
        TaskInfo testTask = submitTestTask("submittedTask", null);

        when(taskInfoPersistence.findTasksByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(new PageResult<TaskInfo>());

        verifyDispatch(testTask);
    }

    @Test
    public void runTaskFromPersistenceStore() {
        TaskInfo testTask = createTestTask(1L, "persistedTask");

        PageResult<TaskInfo> nonEmptyPageResult = new PageResult<>();
        nonEmptyPageResult.setResultList(ImmutableList.of(testTask));
        when(taskInfoPersistence.findTasksByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(nonEmptyPageResult)
                .thenReturn(new PageResult<TaskInfo>());

        verifyDispatch(testTask);
    }

    private static class TaskSyncer implements Runnable {
        volatile boolean done = false;
        @Override
        public void run() {
            while (!done) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void verifyDispatch(TaskInfo testTask) {
        CompletionStage<TaskInfo> process = CompletableFuture.completedFuture(testTask);
        TaskSyncer done = new TaskSyncer();
        Thread joiner = new Thread(done);
        joiner.start();

        Answer<Void> doneAnswer = invocation -> {
            done.done = true;
            return null;
        };

        ServiceComputation testComputation = prepareComputations(testTask, null, process, doneAnswer);

        testDispatcher.dispatchServices();
        try {
            joiner.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        verify(logger).info("Dequeued task {}", testTask);
        verify(testComputation).preProcessData(testTask);
        verify(testComputation).isReady(testTask);
        verify(testComputation).processData(testTask);
        verify(testComputation).isDone(testTask);
        verify(taskInfoPersistence, atLeast(2)).update(testTask);
        assertThat(testTask.getState(), equalTo(TaskState.SUCCESSFUL));
    }

    private ServiceComputation prepareComputations(TaskInfo testTask, Throwable exc, CompletionStage<TaskInfo> processingStage, Answer<Void> doneAnswer) {
        ServiceDescriptor testDescriptor = mock(ServiceDescriptor.class);
        ServiceComputation testComputation = mock(ServiceComputation.class);

        when(serviceRegistry.lookupService(testTask.getName())).thenReturn(testDescriptor);
        when(testDescriptor.createComputationInstance()).thenReturn(testComputation);

        when(testComputation.preProcessData(testTask)).thenReturn(CompletableFuture.completedFuture(testTask));
        when(testComputation.isReady(testTask)).thenReturn(CompletableFuture.completedFuture(testTask));
        when(testComputation.processData(testTask)).thenReturn(processingStage);
        when(testComputation.isDone(testTask)).thenReturn(CompletableFuture.completedFuture(testTask));
        doAnswer(doneAnswer).when(testComputation).postProcessData(same(testTask), exc != null ? any(Throwable.class) : isNull());
        return testComputation;
    }

    @Test
    public void taskProcessingError() {
        TaskInfo testTask = submitTestTask("submittedTask", null);
        when(taskInfoPersistence.findTasksByState(any(Set.class), any(PageRequest.class)))
                .thenReturn(new PageResult<TaskInfo>());
        TaskSyncer done = new TaskSyncer();
        Thread joiner = new Thread(done);
        joiner.start();

        CompletableFuture<TaskInfo> process = new CompletableFuture<>();
        ComputationException processException = new ComputationException("test exception");
        process.completeExceptionally(processException);

        Answer<Void> doneAnswer = invocation -> {
            done.done = true;
            return null;
        };

        ServiceComputation testComputation = prepareComputations(testTask, processException, process, doneAnswer);

        testDispatcher.dispatchServices();
        try {
            joiner.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        verify(logger).info("Dequeued task {}", testTask);
        verify(testComputation).preProcessData(testTask);
        verify(testComputation).isReady(testTask);
        verify(testComputation).processData(testTask);
        verify(testComputation, never()).isDone(testTask);
        verify(taskInfoPersistence, times(3)).update(testTask);
        assertThat(testTask.getState(), equalTo(TaskState.ERROR));
    }

    @Test
    public void syncServiceQueue() {
        PageResult<TaskInfo> taskInfoPageResult = new PageResult<>();
        List<TaskInfo> taskResults = ImmutableList.<TaskInfo>builder()
                .add(createTestTask(1L, "t1"))
                .add(createTestTask(2L, "t2"))
                .add(createTestTask(3L, "t3"))
                .add(createTestTask(4L, "t4"))
                .add(createTestTask(5L, "t5"))
                .add(createTestTask(6L, "t6"))
                .add(createTestTask(7L, "t7"))
                .build();
        taskInfoPageResult.setResultList(taskResults);
        when(taskInfoPersistence.findTasksByState(any(Set.class), any(PageRequest.class))).thenReturn(taskInfoPageResult);
        testDispatcher.syncServiceQueue();
        ServerStats stats = testDispatcher.getServerStats();
        assertThat(stats.getWaitingTasks(), equalTo(taskResults.size()));
    }
}
