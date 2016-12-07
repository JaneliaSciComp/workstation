package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.JacsServiceEventDao;
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

public class JacsServiceDataPersistenceITest extends AbstractJpaDaoITest {

    private List<JacsServiceData> testData;
    private JacsServiceDataDao serviceDataTestDao;
    private JacsServiceEventDao serviceEventTestDao;

    @Before
    public void setUp() {
        testData = new ArrayList<>();
        serviceDataTestDao = new JacsServiceDataJpaDao(testEntityManager);
        serviceEventTestDao = new JacsServiceEventJpaDao(testEntityManager);
        testEntityManager.getTransaction().begin();
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        for (JacsServiceData t : testData) {
            deleteServiceData(t);
        }
        testEntityManager.getTransaction().commit();
    }

    @Test
    public void persistServiceData() {
        JacsServiceData si = persistServiceWithEvents(createTestService("s", "t"),
                createTestServiceEvent("e1", "v1"),
                createTestServiceEvent("e2", "v2"));
        JacsServiceData retrievedSi = serviceDataTestDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
    }

    @Test
    public void persistServiceHierarchy() {
        JacsServiceData si1 = persistServiceWithEvents(createTestService("s1", "t1"));
        JacsServiceData retrievedSi1 = serviceDataTestDao.findById(si1.getId());
        assertThat(retrievedSi1, allOf(
                hasProperty("parentServiceId", nullValue(Long.class)),
                hasProperty("rootServiceId", nullValue(Long.class))
        ));
        JacsServiceData si1_1 = createTestService("s1.1", "t1.1");
        si1.addChildService(si1_1);
        serviceDataTestDao.save(si1_1);
        testData.add(0, si1_1);
        JacsServiceData retrievedSi1_1 = serviceDataTestDao.findById(si1_1.getId());
        assertThat(retrievedSi1_1, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2 = createTestService("s1.2", "t1.2");
        si1.addChildService(si1_2);
        serviceDataTestDao.save(si1_2);
        testData.add(0, si1_2);
        JacsServiceData retrievedSi1_2 = serviceDataTestDao.findById(si1_2.getId());
        assertThat(retrievedSi1_2, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2_1 = createTestService("s1.2.1", "t1.2.1");
        si1_2.addChildService(si1_2_1);
        serviceDataTestDao.save(si1_2_1);
        testData.add(0, si1_2_1);

        JacsServiceData retrievedSi1_2_1 = serviceDataTestDao.findById(si1_2_1.getId());
        assertThat(retrievedSi1_2_1, allOf(
                hasProperty("parentServiceId", equalTo(si1_2.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        commit();

        // we need to flush otherwise the statements are not sent to the DB and the query will not find them from the cache
        if (testEntityManager.getTransaction().isActive()) testEntityManager.flush();

        List<JacsServiceData> s1Children = serviceDataTestDao.findChildServices(si1.getId());
        assertThat(s1Children.size(), equalTo(2));
        assertThat(s1Children, everyItem(Matchers.hasProperty("parentServiceId", equalTo(si1.getId()))));

        List<JacsServiceData> s1Hierarchy = serviceDataTestDao.findServiceHierarchy(si1.getId());
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
        PageResult<JacsServiceData> retrievedQueuedServices = serviceDataTestDao.findServiceByState(ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));

        PageResult<JacsServiceData> retrievedRunningOrCanceledServices = serviceDataTestDao.findServiceByState(
                ImmutableSet.of(JacsServiceState.RUNNING, JacsServiceState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledServices.getResultList().size(), equalTo(servicesInRunningState.size() + servicesInCanceledState.size()));
    }

    private JacsServiceData persistServiceWithEvents(JacsServiceData si, JacsServiceEvent... jacsServiceEvents) {
        serviceDataTestDao.save(si);
        for (JacsServiceEvent se : jacsServiceEvents) {
            si.addEvent(se);
            serviceEventTestDao.save(se);
        }
        commit();
        testData.add(0, si);
        return si;
    }

    private JacsServiceData createTestService(String serviceName, String serviceType) {
        JacsServiceData si = new JacsServiceData();
        si.setName(serviceName);
        si.setServiceType(serviceType);
        si.addArg("I1");
        si.addArg("I2");
        return si;
    }

    private JacsServiceEvent createTestServiceEvent(String name, String value) {
        JacsServiceEvent se = new JacsServiceEvent();
        se.setName(name);
        se.setValue(value);
        return se;
    }

    private void deleteServiceData(JacsServiceData ti) {
        serviceDataTestDao.delete(ti);
    }

    private void commit() {
        testEntityManager.getTransaction().commit();
        testEntityManager.getTransaction().begin();
    }
}
