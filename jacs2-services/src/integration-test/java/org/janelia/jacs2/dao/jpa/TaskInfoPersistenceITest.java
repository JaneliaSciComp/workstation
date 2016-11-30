package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.TaskEventDao;
import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.service.TaskEvent;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;

public class TaskInfoPersistenceITest extends AbstractJpaDaoITest {

    private List<TaskInfo> testData;
    private TaskInfoDao taskInfoTestDao;
    private TaskEventDao taskEventTestDao;

    @Before
    public void setUp() {
        testData = new ArrayList<>();
        taskInfoTestDao = new TaskInfoJpaDao(testEntityManager);
        taskEventTestDao = new TaskEventJpaDao(testEntityManager);
        testEntityManager.getTransaction().begin();
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        for (TaskInfo t : testData) {
            deleteTask(t);
        }
        testEntityManager.getTransaction().commit();
    }

    @Test
    public void persistTaskInfo() {
        TaskInfo si = persistTaskWithEvents(createTestTaskInfo("s", "t"),
                createTestTaskEvent("e1", "v1"),
                createTestTaskEvent("e2", "v2"));
        TaskInfo retrievedSi = taskInfoTestDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
    }

    @Test
    public void persistTaskHierarchy() {
        TaskInfo si1 = persistTaskWithEvents(createTestTaskInfo("s1", "t1"));
        TaskInfo retrievedSi1 = taskInfoTestDao.findById(si1.getId());
        assertThat(retrievedSi1, allOf(
                hasProperty("parentTaskId", nullValue(Long.class)),
                hasProperty("rootTaskId", nullValue(Long.class))
        ));
        TaskInfo si1_1 = createTestTaskInfo("s1.1", "t1.1");
        si1.addSubTask(si1_1);
        taskInfoTestDao.save(si1_1);
        testData.add(0, si1_1);
        TaskInfo retrievedSi1_1 = taskInfoTestDao.findById(si1_1.getId());
        assertThat(retrievedSi1_1, allOf(
                hasProperty("parentTaskId", equalTo(si1.getId())),
                hasProperty("rootTaskId", equalTo(si1.getId()))
        ));

        TaskInfo si1_2 = createTestTaskInfo("s1.2", "t1.2");
        si1.addSubTask(si1_2);
        taskInfoTestDao.save(si1_2);
        testData.add(0, si1_2);
        TaskInfo retrievedSi1_2 = taskInfoTestDao.findById(si1_2.getId());
        assertThat(retrievedSi1_2, allOf(
                hasProperty("parentTaskId", equalTo(si1.getId())),
                hasProperty("rootTaskId", equalTo(si1.getId()))
        ));

        TaskInfo si1_2_1 = createTestTaskInfo("s1.2.1", "t1.2.1");
        si1_2.addSubTask(si1_2_1);
        taskInfoTestDao.save(si1_2_1);
        testData.add(0, si1_2_1);

        TaskInfo retrievedSi1_2_1 = taskInfoTestDao.findById(si1_2_1.getId());
        assertThat(retrievedSi1_2_1, allOf(
                hasProperty("parentTaskId", equalTo(si1_2.getId())),
                hasProperty("rootTaskId", equalTo(si1.getId()))
        ));

        commit();

        // we need to flush otherwise the statements are not sent to the DB and the query will not find them from the cache
        if (testEntityManager.getTransaction().isActive()) testEntityManager.flush();

        List<TaskInfo> s1Children = taskInfoTestDao.findSubTasks(si1.getId());
        assertThat(s1Children.size(), equalTo(2));
        assertThat(s1Children, everyItem(Matchers.hasProperty("parentTaskId", equalTo(si1.getId()))));

        List<TaskInfo> s1Hierarchy = taskInfoTestDao.findTaskHierarchy(si1.getId());
        assertThat(s1Hierarchy.size(), equalTo(3));
        assertThat(s1Hierarchy, everyItem(Matchers.hasProperty("rootTaskId", equalTo(si1.getId()))));
    }

    @Test
    public void retrieveTasksByState() {
        List<TaskInfo> tasksInQueuedState = ImmutableList.of(
                createTestTaskInfo("s1.1", "t1"),
                createTestTaskInfo("s1.2", "t1"),
                createTestTaskInfo("s1.3", "t1"),
                createTestTaskInfo("s1.4", "t1")
        );
        List<TaskInfo> tasksInRunningState = ImmutableList.of(
                createTestTaskInfo("s2.4", "t1"),
                createTestTaskInfo("s2.5", "t1"),
                createTestTaskInfo("s2.6", "t1")
        );
        List<TaskInfo> tasksInCanceledState = ImmutableList.of(
                createTestTaskInfo("s7", "t1"),
                createTestTaskInfo("s8", "t1"),
                createTestTaskInfo("s9", "t1")
        );
        tasksInQueuedState.stream().forEach(s -> {
            s.setState(TaskState.QUEUED);
            persistTaskWithEvents(s);
        });
        tasksInRunningState.stream().forEach(s -> {
            s.setState(TaskState.RUNNING);
            persistTaskWithEvents(s);
        });
        tasksInCanceledState.stream().forEach(s -> {
            s.setState(TaskState.CANCELED);
            persistTaskWithEvents(s);
        });
        PageRequest pageRequest = new PageRequest();
        PageResult<TaskInfo> retrievedQueuedTasks = taskInfoTestDao.findTasksByState(ImmutableSet.of(TaskState.QUEUED), pageRequest);
        assertThat(retrievedQueuedTasks.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(TaskState.QUEUED))));
        assertThat(retrievedQueuedTasks.getResultList().size(), equalTo(tasksInQueuedState.size()));

        PageResult<TaskInfo> retrievedRunningOrCanceledTasks = taskInfoTestDao.findTasksByState(
                ImmutableSet.of(TaskState.RUNNING, TaskState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledTasks.getResultList().size(), equalTo(tasksInRunningState.size() + tasksInCanceledState.size()));
    }

    private TaskInfo persistTaskWithEvents(TaskInfo si, TaskEvent... taskEvents) {
        taskInfoTestDao.save(si);
        for (TaskEvent se : taskEvents) {
            si.addEvent(se);
            taskEventTestDao.save(se);
        }
        commit();
        testData.add(0, si);
        return si;
    }

    private TaskInfo createTestTaskInfo(String taskName, String serviceType) {
        TaskInfo si = new TaskInfo();
        si.setName(taskName);
        si.setServiceType(serviceType);
        si.addArg("I1");
        si.addArg("I2");
        return si;
    }

    private TaskEvent createTestTaskEvent(String name, String value) {
        TaskEvent se = new TaskEvent();
        se.setName(name);
        se.setValue(value);
        return se;
    }

    private void deleteTask(TaskInfo ti) {
        taskInfoTestDao.delete(ti);
    }

    private void commit() {
        testEntityManager.getTransaction().commit();
        testEntityManager.getTransaction().begin();
    }
}
