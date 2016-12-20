package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceEvent;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
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

public class JacsServiceDataMongoDaoITest extends AbstractMongoDaoITest<JacsServiceData> {

    private List<JacsServiceData> testData = new ArrayList<>();
    private JacsServiceDataDao testDao;

    @Before
    public void setUp() {
        testDao = new JacsServiceDataMongoDao(testMongoDatabase);
        setIdGeneratorAndObjectMapper((JacsServiceDataMongoDao) testDao);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void persistServiceData() {
        JacsServiceData si = persistServiceWithEvents(createTestService("s", "t"),
                createTestServiceEvent("e1", "v1"),
                createTestServiceEvent("e2", "v2"));
        JacsServiceData retrievedSi = testDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
    }

    @Test
    public void persistServiceHierarchy() {
        JacsServiceData si1 = persistServiceWithEvents(createTestService("s1", "t1"));
        JacsServiceData retrievedSi1 = testDao.findById(si1.getId());
        assertThat(retrievedSi1, allOf(
                hasProperty("parentServiceId", nullValue(Long.class)),
                hasProperty("rootServiceId", nullValue(Long.class))
        ));
        JacsServiceData si1_1 = createTestService("s1.1", "t1.1");
        si1_1.updateParentService(si1);
        testDao.save(si1_1);
        JacsServiceData retrievedSi1_1 = testDao.findById(si1_1.getId());
        assertThat(retrievedSi1_1, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2 = createTestService("s1.2", "t1.2");
        si1_2.updateParentService(si1);
        testDao.save(si1_2);
        JacsServiceData retrievedSi1_2 = testDao.findById(si1_2.getId());
        assertThat(retrievedSi1_2, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2_1 = createTestService("s1.2.1", "t1.2.1");
        si1_2_1.updateParentService(si1_2);
        testDao.save(si1_2_1);

        JacsServiceData retrievedSi1_2_1 = testDao.findById(si1_2_1.getId());
        assertThat(retrievedSi1_2_1, allOf(
                hasProperty("parentServiceId", equalTo(si1_2.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        List<JacsServiceData> s1Children = testDao.findChildServices(si1.getId());
        assertThat(s1Children.size(), equalTo(2));
        assertThat(s1Children, everyItem(Matchers.hasProperty("parentServiceId", equalTo(si1.getId()))));

        List<JacsServiceData> s1Hierarchy = testDao.findServiceHierarchy(si1.getId());
        assertThat(s1Hierarchy.size(), equalTo(3));
        assertThat(s1Hierarchy, everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));
    }

    @Test
    public void retrieveServicesByState() {
        List<JacsServiceData> servicesInQueuedState = ImmutableList.of(
                createTestService("s1.1", "t1"),
                createTestService("s1.2", "t1"),
                createTestService("s1.3", "t1"),
                createTestService("s1.4", "t1")
        );
        List<JacsServiceData> servicesInRunningState = ImmutableList.of(
                createTestService("s2.4", "t1"),
                createTestService("s2.5", "t1"),
                createTestService("s2.6", "t1")
        );
        List<JacsServiceData> servicesInCanceledState = ImmutableList.of(
                createTestService("s7", "t1"),
                createTestService("s8", "t1"),
                createTestService("s9", "t1")
        );
        servicesInQueuedState.stream().forEach(s -> {
            s.setState(JacsServiceState.QUEUED);
            persistServiceWithEvents(s);
        });
        servicesInRunningState.stream().forEach(s -> {
            s.setState(JacsServiceState.RUNNING);
            persistServiceWithEvents(s);
        });
        servicesInCanceledState.stream().forEach(s -> {
            s.setState(JacsServiceState.CANCELED);
            persistServiceWithEvents(s);
        });
        PageRequest pageRequest = new PageRequest();
        PageResult<JacsServiceData> retrievedQueuedServices = testDao.findServiceByState(ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));

        PageResult<JacsServiceData> retrievedRunningOrCanceledServices = testDao.findServiceByState(
                ImmutableSet.of(JacsServiceState.RUNNING, JacsServiceState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledServices.getResultList().size(), equalTo(servicesInRunningState.size() + servicesInCanceledState.size()));
    }

    private JacsServiceData persistServiceWithEvents(JacsServiceData si, JacsServiceEvent... jacsServiceEvents) {
        for (JacsServiceEvent se : jacsServiceEvents) {
            si.addEvent(se);
        }
        testDao.save(si);
        return si;
    }

    protected List<JacsServiceData> createMultipleTestItems(int nItems) {
        List<JacsServiceData> testItems = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            testItems.add(createTestService("s" + (i + 1), "t" + (i + 1)));
        }
        return testItems;
    }

    private JacsServiceData createTestService(String serviceName, String serviceType) {
        JacsServiceData si = new JacsServiceData();
        si.setName(serviceName);
        si.setServiceType(serviceType);
        si.addArg("I1");
        si.addArg("I2");
        testData.add(si);
        return si;
    }

    private JacsServiceEvent createTestServiceEvent(String name, String value) {
        JacsServiceEvent se = new JacsServiceEvent();
        se.setName(name);
        se.setValue(value);
        return se;
    }

}
