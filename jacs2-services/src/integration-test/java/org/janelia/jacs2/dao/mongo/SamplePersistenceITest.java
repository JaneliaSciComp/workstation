package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.domain.sample.SampleObjective;
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
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;

public class SamplePersistenceITest extends AbstractMongoDaoITest<Sample, Number> {

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
            delete(testDao, s);
        }
    }

    @Test
    public void persistSample() {
        Sample testSample = createTestSample();
        testSample.getObjectives().addAll(ImmutableList.of(
                createSampleObjective("o1"),
                createSampleObjective("o2"),
                createSampleObjective("o3")));
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

    @Test
    public void updateSample() {
        Sample testSample = createTestSample();
        testDao.save(testSample);
        testData.add(0, testSample);
        testSample.setOwnerKey("subject:verify");
        testSample.setFlycoreAlias(null);
        testSample.setDataSet("newDataSet that has been changed");
        testSample.setLine("Updated line");
        testSample.setEffector("best effector");
        testSample.getObjectives().addAll(ImmutableList.of(
                createSampleObjective("new_o1"),
                createSampleObjective("new_o2"),
                createSampleObjective("new_o3")));
        testDao.update(testSample);
        Sample retrievedSample = testDao.findById(testSample.getId());
        assertNull(testSample.getFlycoreAlias());
        assertThat(retrievedSample, hasProperty("ownerKey", equalTo("subject:verify")));
        assertThat(retrievedSample, hasProperty("dataSet", equalTo(testSample.getDataSet())));
    }

    private SampleObjective createSampleObjective(String o) {
        SampleObjective so = new SampleObjective();
        so.setObjective(o);
        so.setChanSpec("cs");
        return so;
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
