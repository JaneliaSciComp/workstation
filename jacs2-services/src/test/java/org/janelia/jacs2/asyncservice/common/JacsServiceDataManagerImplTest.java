package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JacsServiceDataManagerImplTest {

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceDispatcher jacsServiceDispatcher;
    private JacsServiceDataManager jacsServiceDataManager;
    private int idSequence = 1;

    @Before
    public void setUp() {
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        jacsServiceDispatcher = mock(JacsServiceDispatcher.class);
        when(jacsServiceDispatcher.submitServiceAsync(any(JacsServiceData.class)))
                .thenAnswer(invocation -> {
                    JacsServiceData sd = invocation.getArgument(0);
                    sd.setId(idSequence++);
                    return sd;
                });
        jacsServiceDataManager = new JacsServiceDataManagerImpl(jacsServiceDataPersistence, jacsServiceDispatcher);
    }

    @Test
    public void prioritiesMustBeDescending() {
        List<JacsServiceData> services = ImmutableList.of(
                createServiceData("s1", 1),
                createServiceData("s2", 3),
                createServiceData("s3", 1)
        );
        List<JacsServiceData> newServices = jacsServiceDataManager.createMultipleServices(services);
        assertThat(newServices, contains(
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