package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.ServerStats;
import org.janelia.jacs2.asyncservice.ServiceRegistry;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;

import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
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
        int nSlots = 110;
        jacsServiceEngine.setProcessingSlotsCount(0);
        jacsServiceEngine.setProcessingSlotsCount(nSlots);
        ServerStats stats = jacsServiceEngine.getServerStats();
        assertThat(stats.getAvailableSlots(), equalTo(nSlots));
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

}
