package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Objects.isNull;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;

public class SamplePersistenceITest extends AbstractMongoDaoITest<Sample, Long> {

    private List<Sample> testData = new ArrayList<>();
    @Spy
    private ObjectMapper objectMapper = ObjectMapperFactory.instance().getObjectMapper();
    @Spy
    private TimebasedIdentifierGenerator idGenerator = new TimebasedIdentifierGenerator(0);
    @InjectMocks
    private SampleDao testDao;

    @Before
    public void setUp() {
        testData = new ArrayList<>();
        testDao = new SampleMongoDao(testMongoDatabase);
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        for (Sample s : testData) {
//            delete(testDao, s);
        }
    }

    @Test
    public void persistSample() {
        Sample testSample = createTestSample();
        testDao.save(testSample);
        testData.add(0, testSample);
        Sample retrievedSample = testDao.findById(testSample.getId());
        assertThat(retrievedSample, not(isNull(Sample.class)));
        assertThat(retrievedSample, not(same(testSample)));
        assertThat(retrievedSample.getId(), allOf(
                not(isNull(Long.class)),
                equalTo(testSample.getId())
        ));

    }

    private Sample createTestSample() {
        Sample testSample = new Sample();
        Date currentTime = new Date();
        testSample.setDataSet("testDataset");
        testSample.setSlideCode("slideCode1");
        testSample.setFlycoreAlias("testAlias");
        testSample.setCompletionDate(currentTime);
        testSample.setTmogDate(currentTime);
        return testSample;
    }
}
