package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AbstractExternalProcessComputationTest {

    private static class TestExternalProcessComputation extends AbstractExternalProcessComputation<Void> {
        @Override
        protected List<String> prepareCmdArgs(JacsService<Void> jacsService) {
            return Collections.emptyList();
        }

        @Override
        protected Map<String, String> prepareEnvironment(JacsService<Void> jacsServiceData) {
            return Collections.emptyMap();
        }
    }

    @Mock
    private Logger logger;
    @InjectMocks
    private AbstractExternalProcessComputation<?> testComputation;

    @Before
    public void setUp() {
        testComputation = new TestExternalProcessComputation();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void checkOutputErrors() {
        ImmutableMap<String, Boolean> testData =
                new ImmutableMap.Builder<String, Boolean>()
                        .put("This has an error here", true)
                        .put("This has an ERROR here", true)
                        .put("This has an exception here", true)
                        .put("No Exception", true)
                        .put("OK here", false)
                        .put("\n", false)
                        .build();
        testData.forEach((l, r) -> assertThat(testComputation.checkForErrors(l), equalTo(r)));
    }

}
