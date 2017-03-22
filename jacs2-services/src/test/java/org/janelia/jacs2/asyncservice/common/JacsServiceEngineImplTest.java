package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.ServerStats;
import org.janelia.jacs2.asyncservice.ServiceRegistry;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JacsServiceEngineImplTest {

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceQueue jacsServiceQueue;
    private JacsServiceEngine jacsServiceEngine;
    private Instance<ServiceRegistry> serviceRegistrarSource;
    private ServiceRegistry serviceRegistry;
    private Logger logger;
    private int idSequence = 1;

    @Before
    public void setUp() {
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        serviceRegistrarSource = mock(Instance.class);
        serviceRegistry = mock(ServiceRegistry.class);
        logger = mock(Logger.class);
        jacsServiceQueue = new InMemoryJacsServiceQueue(jacsServiceDataPersistence, 10, logger);
        doAnswer(invocation -> {
            JacsServiceData sd = invocation.getArgument(0);
            sd.setId(idSequence++);
            return null;
        }).when(jacsServiceDataPersistence).saveHierarchy(any(JacsServiceData.class));
        jacsServiceEngine = new JacsServiceEngineImpl(jacsServiceDataPersistence, jacsServiceQueue, serviceRegistrarSource, 10, logger);
        when(serviceRegistrarSource.get()).thenReturn(serviceRegistry);
    }

    @Test
    public void increaseNumberOfSlots() {
        PageResult<JacsServiceData> serviceDataPageResult = new PageResult<>();
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class))).thenReturn(serviceDataPageResult);
        int nSlots = 110;
        jacsServiceEngine.setProcessingSlotsCount(0);
        jacsServiceEngine.setProcessingSlotsCount(nSlots);
        ServerStats stats = jacsServiceEngine.getServerStats();
        assertThat(stats.getAvailableSlots(), equalTo(nSlots));
    }

    @Test
    public void serverStatsReflectCurrentQueues() {
        PageResult<JacsServiceData> waitingServices = new PageResult<>();
        List<JacsServiceData> waitingResults = ImmutableList.<JacsServiceData>builder()
                .add(createTestService(1L, "t1"))
                .add(createTestService(2L, "t2"))
                .build();
        waitingServices.setResultList(waitingResults);

        PageResult<JacsServiceData> runningServices = new PageResult<>();
        List<JacsServiceData> runningResults = ImmutableList.<JacsServiceData>builder()
                .add(createTestService(1L, "t1"))
                .add(createTestService(2L, "t2"))
                .add(createTestService(3L, "t3"))
                .build();
        runningServices.setResultList(runningResults);
        when(jacsServiceDataPersistence.findServicesByState(eq(EnumSet.of(JacsServiceState.QUEUED)), any(PageRequest.class)))
                .thenReturn(waitingServices);
        when(jacsServiceDataPersistence.findServicesByState(eq(EnumSet.of(JacsServiceState.RUNNING)), any(PageRequest.class)))
                .thenReturn(runningServices);
        ServerStats stats = jacsServiceEngine.getServerStats();
        assertThat(stats.getWaitingServices().size(), equalTo(2));
        assertThat(stats.getRunningServices().size(), equalTo(3));
    }

    @Test
    public void prioritiesMustBeDescending() {
        List<JacsServiceData> services = ImmutableList.of(
                createServiceData("s1", 1),
                createServiceData("s2", 3),
                createServiceData("s3", 1)
        );
        List<JacsServiceData> newServices = jacsServiceEngine.submitMultipleServices(services);
        Assert.assertThat(newServices, contains(
                allOf(hasProperty("id", equalTo(1)),
                        hasProperty("name", equalTo("s1")),
                        hasProperty("priority", equalTo(4))
                ),
                allOf(hasProperty("id", equalTo(2)),
                        hasProperty("name", equalTo("s2")),
                        hasProperty("priority", equalTo(3))
                ),
                allOf(hasProperty("id", equalTo(3)),
                        hasProperty("name", equalTo("s3")),
                        hasProperty("priority", equalTo(1))
                )
        ));
    }

    private JacsServiceData createServiceData(String name, int priority) {
        JacsServiceData sd = new JacsServiceData();
        sd.setName(name);
        sd.setPriority(priority);
        return sd;
    }

    private JacsServiceData createTestService(Long serviceId, String serviceName) {
        JacsServiceData testService = new JacsServiceData();
        testService.setId(serviceId);
        testService.setName(serviceName);
        return testService;
    }

}
