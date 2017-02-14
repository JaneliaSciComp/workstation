package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemoryJacsServiceQueueTest {

    private static final Long TEST_ID = 101L;

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceQueue jacsServiceQueue;
    private Logger logger;

    @Before
    public void setUp() {
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        logger = mock(Logger.class);
        jacsServiceQueue = new InMemoryJacsServiceQueue(jacsServiceDataPersistence, logger);
        Answer<Void> saveServiceData = invocation -> {
            JacsServiceData ti = invocation.getArgument(0);
            ti.setId(TEST_ID);
            return null;
        };
        doAnswer(saveServiceData).when(jacsServiceDataPersistence).saveHierarchy(any(JacsServiceData.class));
    }

    @Test
    public void syncServiceQueue() {
        PageResult<JacsServiceData> serviceDataPageResult = new PageResult<>();
        List<JacsServiceData> serviceResults = ImmutableList.<JacsServiceData>builder()
                .add(createTestService(1L, "t1"))
                .add(createTestService(2L, "t2"))
                .add(createTestService(3L, "t3"))
                .add(createTestService(4L, "t4"))
                .add(createTestService(5L, "t5"))
                .add(createTestService(6L, "t6"))
                .add(createTestService(7L, "t7"))
                .build();
        serviceDataPageResult.setResultList(serviceResults);
        when(jacsServiceDataPersistence.findServicesByState(any(Set.class), any(PageRequest.class))).thenReturn(serviceDataPageResult);
        jacsServiceQueue.refreshServiceQueue();
        assertThat(jacsServiceQueue.getReadyServicesSize(), equalTo(serviceResults.size()));
    }

    private JacsServiceData createTestService(Long serviceId, String serviceName) {
        JacsServiceData testService = new JacsServiceData();
        testService.setId(serviceId);
        testService.setName(serviceName);
        return testService;
    }

}
