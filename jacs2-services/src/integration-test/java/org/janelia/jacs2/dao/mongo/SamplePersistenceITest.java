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

public class SamplePersistenceITest extends AbstractMongoDaoITest<Sample, Long> {

    private List<Sample> testData = new ArrayList<>();
    @Spy
    private ObjectMapper objectMapper = ObjectMapperFactory.instance().getObjectMapper();
    @Spy
    private TimebasedIdentifierGenerator idGenerator = new TimebasedIdentifierGenerator();
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
            delete(testDao, s);
        }
    }

    @Test
    public void persistSample() {
        Sample testSample = createTestSample();
        testDao.save(testSample);
        testData.add(0, testSample);
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
