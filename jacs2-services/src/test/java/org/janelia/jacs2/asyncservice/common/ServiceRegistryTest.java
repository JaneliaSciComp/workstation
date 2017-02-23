package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.xml.ws.Service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceRegistryTest {
    private ServiceRegistry testServiceRegistry;

    @Before
    public void setUp() {
        Logger logger = mock(Logger.class);
        Instance<ServiceProcessor<?>> anyServiceSource = mock(Instance.class);
        ServiceProcessor<?> registeredService = mock(ServiceProcessor.class);
        testServiceRegistry = new JacsServiceRegistry(anyServiceSource, logger);
        when(registeredService.getMetadata()).thenReturn(createTestServiceMetadata());
        when(anyServiceSource.iterator()).thenReturn(ImmutableList.<ServiceProcessor<?>>of(registeredService).iterator());
    }

    private ServiceMetaData createTestServiceMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName("registered");
        return smd;
    }
    @Test
    public void getMetadataForRegisteredService() {
        ServiceMetaData smd = testServiceRegistry.getServiceMetadata("registered");
        assertThat(smd, hasProperty("serviceName", equalTo("registered")));
    }

    @Test
    public void getMetadataForUnregisteredService() {
        ServiceMetaData smd = testServiceRegistry.getServiceMetadata("unregistered");
        assertNull(smd);
    }

}
