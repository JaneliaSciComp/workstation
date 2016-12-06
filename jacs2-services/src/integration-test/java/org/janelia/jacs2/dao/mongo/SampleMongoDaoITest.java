package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.model.domain.DataFile;
import org.janelia.jacs2.model.domain.FileType;
import org.janelia.jacs2.model.domain.Reference;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.domain.sample.SampleObjective;
import org.janelia.jacs2.model.domain.sample.SampleTile;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;

public class SampleMongoDaoITest extends AbstractDomainObjectDaoITest<Sample> {

    private List<Sample> testData = new ArrayList<>();
    private SampleDao testDao;

    @Before
    public void setUp() {
        testDao = new SampleMongoDao(testMongoDatabase);
        setIdGeneratorAndObjectMapper((SampleMongoDao) testDao);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void findAll() {
        List<Sample> testSamples = ImmutableList.of(
            createTestSample("ds1", "sc1"),
            createTestSample("ds1", "sc2"),
            createTestSample("ds1", "sc3"),
            createTestSample("ds1", "sc4"),
            createTestSample("ds1", "sc5"),
            createTestSample("ds2", "sc1"),
            createTestSample("ds2", "sc2"),
            createTestSample("ds2", "sc3"),
            createTestSample("ds2", "sc4"),
            createTestSample("ds2", "sc5")
        );
        testSamples.parallelStream().forEach(s -> testDao.save(s));
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNumber(0);
        pageRequest.setPageSize(5);
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("dataSet", SortDirection.ASC),
                new SortCriteria("slideCode", SortDirection.DESC),
                new SortCriteria("completionDate", SortDirection.DESC)));
        PageResult<Sample> res1 = testDao.findAll(pageRequest);
        assertThat(res1.getResultList(), hasSize(5));
        assertThat(res1.getResultList(), everyItem(hasProperty("dataSet", equalTo("ds1"))));
        assertThat(res1.getResultList().stream().map(s -> s.getId()).collect(Collectors.toList()), contains(
                testSamples.get(4).getId(),
                testSamples.get(3).getId(),
                testSamples.get(2).getId(),
                testSamples.get(1).getId(),
                testSamples.get(0).getId()
        ));
        pageRequest.setPageNumber(1);
        PageResult<Sample> res2 = testDao.findAll(pageRequest);
        assertThat(res2.getResultList(), hasSize(5));
        assertThat(res2.getResultList(), everyItem(hasProperty("dataSet", equalTo("ds2"))));
        assertThat(res2.getResultList().stream().map(s -> s.getId()).collect(Collectors.toList()), contains(
                testSamples.get(9).getId(),
                testSamples.get(8).getId(),
                testSamples.get(7).getId(),
                testSamples.get(6).getId(),
                testSamples.get(5).getId()
        ));
    }

    @Test
    public void findByOwnerWithoutSubject() {
        findByOwner(null, testDao);
    }

    @Test
    public void findByIdsWithNoSubject() {
        findByIdsWithNoSubject(testDao);
    }

    @Test
    public void findByIdsWithSubject() {
        findByIdsWithSubject(testDao);
    }

    @Test
    public void persistSample() {
        Sample testSample = createTestSample("ds1", "sc1");
        testSample.getObjectives().addAll(ImmutableList.of(
                createSampleObjective("o1"),
                createSampleObjective("o2"),
                createSampleObjective("o3")));
        testDao.save(testSample);
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
        Sample testSample = createTestSample("ds1", "sc1");
        testDao.save(testSample);
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

    protected List<Sample> createMultipleTestItems(int nItems) {
        List<Sample> testItems = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            testItems.add(createTestSample("ds" + (i + 1), "sc" + (i + 1)));
        }
        return testItems;
    }

    private Sample createTestSample(String dataset, String slideCode) {
        Sample testSample = new Sample();
        Date currentTime = new Date();
        testSample.setDataSet(dataset);
        testSample.setSlideCode(slideCode);
        testSample.setFlycoreAlias("testAlias");
        testSample.setCompletionDate(currentTime);
        testSample.setTmogDate(currentTime);
        testSample.setOwnerKey(TEST_OWNER_KEY);
        testSample.addObjective(createTestObjective());
        testData.add(testSample);
        return testSample;
    }

    private SampleObjective createTestObjective() {
        SampleObjective objective = new SampleObjective();
        objective.setObjective("testObjective");
        objective.setChanSpec("rgb");
        objective.addTile(createTile());
        return objective;
    }

    private SampleTile createTile() {
        SampleTile sampleTile = new SampleTile();
        sampleTile.addLsmReference(new Reference("LSMImage", dataGenerator.nextLong()));
        sampleTile.addDataFile(createDataFile());
        return sampleTile;
    }

    private DataFile createDataFile() {
        DataFile dataFile = new DataFile();
        dataFile.setFileName("testFile");
        dataFile.setFileType(FileType.ChanFile);
        return dataFile;
    }
}
