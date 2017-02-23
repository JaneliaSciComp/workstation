package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.DomainModelUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;

public class SampleMongoDaoITest extends AbstractDomainObjectDaoITest<Sample> {

    private List<Sample> testData = new ArrayList<>();
    private SampleDao testDao;

    @Before
    public void setUp() {
        testDao = new SampleMongoDao(testMongoDatabase, idGenerator, testObjectMapperFactory);
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
    public void findMatchingSamples() {
        Calendar currentCal = Calendar.getInstance();
        currentCal.add(Calendar.HOUR, -24);
        Date startDate = currentCal.getTime();
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
        currentCal.add(Calendar.HOUR, 48);
        Date endDate = currentCal.getTime();
        testDao.saveAll(testSamples);
        Sample testRequest = new Sample();
        testRequest.setDataSet("ds1");

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNumber(1);
        pageRequest.setPageSize(3);
        PageResult<Sample> retrievedSamples;

        retrievedSamples = testDao.findMatchingSamples(null, testRequest, new DataInterval<>(startDate, endDate), pageRequest);
        assertThat(retrievedSamples.getResultList(), hasSize(2)); // only 2 items are left on the last page
        assertThat(retrievedSamples.getResultList(), everyItem(hasProperty("dataSet", equalTo("ds1"))));
    }

    @Test
    public void persistSample() {
        Sample testSample = createTestSample("ds1", "sc1");
        testSample.getObjectiveSamples().addAll(ImmutableList.of(
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
        Sample newSample = testDao.findById(testSample.getId());
        changeAndUpdateSample(newSample);
        Sample retrievedSample = testDao.findById(testSample.getId());
        assertNull(retrievedSample.getFlycoreAlias());
        assertThat(retrievedSample, hasProperty("line", equalTo("Updated line")));
        assertThat(retrievedSample, hasProperty("dataSet", equalTo(newSample.getDataSet())));
    }

    @Test
    public void tryToUpdateALockedSampleWithoutTheKey() {
        Sample testSample = createTestSample("ds1", "sc1");
        testSample.setLockKey("LockKey");
        testDao.save(testSample);
        Sample newSample = testDao.findById(testSample.getId());
        for (String lockKey : new String[]{null, "WrongKey"}) {
            newSample.setLockKey(lockKey);
            changeAndUpdateSample(newSample);
            Sample retrievedSample = testDao.findById(testSample.getId());
            assertThat(retrievedSample.getFlycoreAlias(), allOf(equalTo(testSample.getFlycoreAlias()), not(equalTo(newSample.getFlycoreAlias()))));
            assertThat(retrievedSample.getLine(), allOf(equalTo(testSample.getLine()), not(equalTo(newSample.getLine()))));
            assertThat(retrievedSample.getDataSet(), allOf(equalTo(testSample.getDataSet()), not(equalTo(newSample.getDataSet()))));
        }
    }

    @Test
    public void tryToUpdateALockedSampleWithTheRightKey() {
        Sample testSample = createTestSample("ds1", "sc1");
        testSample.setLockKey("LockKey");
        testDao.save(testSample);
        Sample newSample = testDao.findById(testSample.getId());
        changeAndUpdateSample(newSample);
        Sample retrievedSample = testDao.findById(testSample.getId());
        assertThat(retrievedSample.getFlycoreAlias(), allOf(equalTo(newSample.getFlycoreAlias()), not(equalTo(testSample.getFlycoreAlias()))));
        assertThat(retrievedSample.getLine(), allOf(equalTo(newSample.getLine()), not(equalTo(testSample.getLine()))));
        assertThat(retrievedSample.getDataSet(), allOf(equalTo(newSample.getDataSet()), not(equalTo(testSample.getDataSet()))));
    }

    private void changeAndUpdateSample(Sample testSample) {
        testSample.setFlycoreAlias(null);
        testSample.setDataSet("newDataSet that has been changed");
        testSample.setLine("Updated line");
        testSample.setEffector("best effector");
        testSample.getObjectiveSamples().addAll(ImmutableList.of(
                createSampleObjective("new_o1"),
                createSampleObjective("new_o2"),
                createSampleObjective("new_o3")));
        testDao.update(testSample);

    }

    @Test
    public void lockAndUnlockAnUnlockedSample() {
        Sample testSample = createTestSample("ds1", "sc1");
        testDao.save(testSample);
        Sample savedSample = testDao.findById(testSample.getId());
        assertThat(savedSample.getLockKey(), nullValue());
        assertThat(savedSample.getLockTimestamp(), nullValue());
        // unlocking an unlocked sample has no effect
        assertTrue(testDao.unlockEntity("AnyKey", savedSample));
        savedSample = testDao.findById(testSample.getId());
        assertThat(savedSample.getLockKey(), nullValue());
        assertThat(savedSample.getLockTimestamp(), nullValue());
        // now place the lock
        assertTrue(testDao.lockEntity("LockKey", testSample));
        Sample savedLockedSample = testDao.findById(testSample.getId());
        assertThat(savedLockedSample.getLockKey(), equalTo("LockKey"));
        assertThat(savedLockedSample.getLockTimestamp(), not(nullValue()));
    }

    @Test
    public void lockAndUnlockAnAlreadyLockedSample() {
        Sample testSample = createTestSample("ds1", "sc1");
        testDao.save(testSample);
        assertTrue(testDao.lockEntity("LockKey", testSample));
        Sample savedLockedSample = testDao.findById(testSample.getId());
        assertThat(savedLockedSample.getLockKey(), equalTo("LockKey"));
        // I cannot place another lock on an already locked sample
        assertFalse(testDao.lockEntity("NewKey", savedLockedSample));
        savedLockedSample = testDao.findById(testSample.getId());
        assertThat(savedLockedSample.getLockKey(), equalTo("LockKey"));
        // The sample cannot be unlocked with the wrong key
        assertFalse(testDao.lockEntity("WrongKey", savedLockedSample));
        savedLockedSample = testDao.findById(testSample.getId());
        assertThat(savedLockedSample.getLockKey(), equalTo("LockKey"));
        // Now unlock it and test that the lock can be placed
        assertTrue(testDao.unlockEntity("LockKey", savedLockedSample));
        assertTrue(testDao.lockEntity("NewKey", savedLockedSample));
        savedLockedSample = testDao.findById(testSample.getId());
        assertThat(savedLockedSample.getLockKey(), equalTo("NewKey"));
    }

    private ObjectiveSample createSampleObjective(String o) {
        ObjectiveSample so = new ObjectiveSample();
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

    private ObjectiveSample createTestObjective() {
        ObjectiveSample objective = new ObjectiveSample();
        objective.setObjective("testObjective");
        objective.setChanSpec("rgb");
        objective.addTiles(createTile());
        return objective;
    }

    private SampleTile createTile() {
        SampleTile sampleTile = new SampleTile();
        sampleTile.addLsmReference(new Reference("LSMImage", dataGenerator.nextLong()));
        DomainModelUtils.setPathForFileType(sampleTile, FileType.ChanFile, "testChanFile");
        DomainModelUtils.setPathForFileType(sampleTile, FileType.MaskFile, "testMaskFile");
        return sampleTile;
    }

}
