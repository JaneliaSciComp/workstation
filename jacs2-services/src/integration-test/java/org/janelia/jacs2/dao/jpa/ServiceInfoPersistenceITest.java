package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.ServiceEventDao;
import org.janelia.jacs2.dao.ServiceInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.ServiceEvent;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceState;
import org.junit.Before;
import org.junit.Test;


import java.util.List;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;

public class ServiceInfoPersistenceITest extends AbstractJpaDaoITest {

    private ServiceInfoDao serviceInfoTestDao;
    private ServiceEventDao serviceEventTestDao;

    @Before
    public void setUp() {
        serviceInfoTestDao = new ServiceInfoJpaDao(testEntityManager);
        serviceEventTestDao = new ServiceEventJpaDao(testEntityManager);
        testEntityManager.getTransaction().begin();
    }

    @Test
    public void persistServiceInfo() {
        ServiceInfo si = persistServiceWithEvents(createTestServiceInfo("s", "t"),
                createTestServiceEvent("e1", "v1"),
                createTestServiceEvent("e2", "v2"));
        ServiceInfo retrievedSi = serviceInfoTestDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
    }

    @Test
    public void persistServiceHierarchy() {
        ServiceInfo si1 = persistServiceWithEvents(createTestServiceInfo("s1", "t1"));
        ServiceInfo retrievedSi1 = serviceInfoTestDao.findById(si1.getId());
        assertThat(retrievedSi1, allOf(
                hasProperty("parentServiceId", nullValue(Long.class)),
                hasProperty("rootServiceId", nullValue(Long.class))
        ));
        ServiceInfo si1_1 = createTestServiceInfo("s1.1", "t1.1");
        si1.addChildService(si1_1);
        serviceInfoTestDao.save(si1_1);
        ServiceInfo retrievedSi1_1 = serviceInfoTestDao.findById(si1_1.getId());
        assertThat(retrievedSi1_1, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        ServiceInfo si1_2 = createTestServiceInfo("s1.2", "t1.2");
        si1.addChildService(si1_2);
        serviceInfoTestDao.save(si1_2);
        ServiceInfo retrievedSi1_2 = serviceInfoTestDao.findById(si1_2.getId());
        assertThat(retrievedSi1_2, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        ServiceInfo si1_2_1 = createTestServiceInfo("s1.2.1", "t1.2.1");
        si1_2.addChildService(si1_2_1);
        serviceInfoTestDao.save(si1_2_1);

        ServiceInfo retrievedSi1_2_1 = serviceInfoTestDao.findById(si1_2_1.getId());
        assertThat(retrievedSi1_2_1, allOf(
                hasProperty("parentServiceId", equalTo(si1_2.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));
        // we need to flush otherwise the statements are not sent to the DB and the query will not find them from the cache
        testEntityManager.flush();

        List<ServiceInfo> s1Children = serviceInfoTestDao.findChildServices(si1.getId());
        assertThat(s1Children.size(), equalTo(2));
        assertThat(s1Children, everyItem(Matchers.hasProperty("parentServiceId", equalTo(si1.getId()))));

        List<ServiceInfo> s1Hierarchy = serviceInfoTestDao.findServiceHierarchy(si1.getId());
        assertThat(s1Hierarchy.size(), equalTo(3));
        assertThat(s1Hierarchy, everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));
    }

    @Test
    public void retrieveServicesByState() {
        List<ServiceInfo> servicesInQueuedState = ImmutableList.of(
            createTestServiceInfo("s1.1", "t1"),
            createTestServiceInfo("s1.2", "t1"),
            createTestServiceInfo("s1.3", "t1"),
            createTestServiceInfo("s1.4", "t1")
        );
        List<ServiceInfo> servicesInRunningState = ImmutableList.of(
                createTestServiceInfo("s2.4", "t1"),
                createTestServiceInfo("s2.5", "t1"),
                createTestServiceInfo("s2.6", "t1")
        );
        List<ServiceInfo> servicesInCanceledState = ImmutableList.of(
                createTestServiceInfo("s7", "t1"),
                createTestServiceInfo("s8", "t1"),
                createTestServiceInfo("s9", "t1")
        );
        servicesInQueuedState.stream().forEach(s -> {
            s.setState(ServiceState.QUEUED);
            persistServiceWithEvents(s);
        });
        servicesInRunningState.stream().forEach(s -> {
            s.setState(ServiceState.RUNNING);
            persistServiceWithEvents(s);
        });
        servicesInCanceledState.stream().forEach(s -> {
            s.setState(ServiceState.CANCELED);
            persistServiceWithEvents(s);
        });
        PageRequest pageRequest = new PageRequest();
        PageResult<ServiceInfo> retrievedQueuedServices = serviceInfoTestDao.findServicesByState(ImmutableSet.of(ServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(ServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));

        PageResult<ServiceInfo> retrievedRunningOrCanceledServices = serviceInfoTestDao.findServicesByState(
                ImmutableSet.of(ServiceState.RUNNING, ServiceState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledServices.getResultList().size(), equalTo(servicesInRunningState.size() + servicesInCanceledState.size()));
    }

    private ServiceInfo persistServiceWithEvents(ServiceInfo si, ServiceEvent... serviceEvents) {
        serviceInfoTestDao.save(si);
        for (ServiceEvent se : serviceEvents) {
            si.addEvent(se);
            serviceEventTestDao.save(se);
        }
        return si;
    }

    private ServiceInfo createTestServiceInfo(String serviceName, String serviceType) {
        ServiceInfo si = new ServiceInfo();
        si.setName(serviceName);
        si.setServiceType(serviceType);
        si.addArg("I1");
        si.addArg("I2");
        return si;
    }

    private ServiceEvent createTestServiceEvent(String name, String value) {
        ServiceEvent se = new ServiceEvent();
        se.setName(name);
        se.setValue(value);
        return se;
    }
}
