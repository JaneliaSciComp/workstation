package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ServiceRegistryTest {

    @Mock
    private Logger logger;
    @Mock
    private Instance<ServiceDescriptor> anyServiceSource;
    @Mock
    private ServiceDescriptor registeredService;
    @InjectMocks
    private ServiceRegistry testServiceRegistry;

    @Before
    public void setUp() {
        testServiceRegistry = new JacsServiceRegistry();
        MockitoAnnotations.initMocks(this);
        when(registeredService.getMetadata()).thenReturn(createTestServiceMetadata());
        when(anyServiceSource.iterator()).thenReturn(ImmutableList.of(registeredService).iterator());
    }

    private ServiceMetaData createTestServiceMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName("registered");
        return smd;
    }
    @Test
    public void getMetadataForRegisteredService() {
        ServiceMetaData smd = testServiceRegistry.getServiceDescriptor("registered");
        assertThat(smd, hasProperty("serviceName", equalTo("registered")));
    }

    @Test
    public void getMetadataForUnregisteredService() {
        ServiceMetaData smd = testServiceRegistry.getServiceDescriptor("unregistered");
        assertNull(smd);
    }

}
