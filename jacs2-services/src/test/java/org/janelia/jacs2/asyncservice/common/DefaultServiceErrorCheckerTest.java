package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultServiceErrorCheckerTest {

    private DefaultServiceErrorChecker serviceErrorChecker;

    @Before
    public void setUp() {
        Logger logger = mock(Logger.class);
        serviceErrorChecker = new DefaultServiceErrorChecker(logger);
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
        testData.forEach((l, r) -> assertThat(serviceErrorChecker.hasErrors(l), equalTo(r)));
    }

}
