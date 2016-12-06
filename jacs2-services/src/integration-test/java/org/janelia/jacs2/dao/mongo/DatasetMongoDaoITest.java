package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.dao.DatasetDao;
import org.janelia.jacs2.model.domain.sample.DataSet;
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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;

public class DatasetMongoDaoITest extends AbstractDomainObjectDaoITest<DataSet> {

    private List<DataSet> testData = new ArrayList<>();
    private DatasetDao testDao;

    @Before
    public void setUp() {
        testDao = new DatasetMongoDao(testMongoDatabase);
        setIdGeneratorAndObjectMapper((DatasetMongoDao) testDao);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void findAll() {
        List<DataSet> testDatasets = createMultipleTestItems(9); //using only 9 items otherwise the alphanumeric sort messes up the check
        testDatasets.parallelStream().forEach(testDao::save);
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNumber(0);
        pageRequest.setPageSize(5);
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("name", SortDirection.ASC),
                new SortCriteria("creationDate", SortDirection.DESC)));
        PageResult<DataSet> res1 = testDao.findAll(pageRequest);
        assertThat(res1.getResultList(), hasSize(5));
        assertThat(res1.getResultList(), everyItem(hasProperty("name", startsWith("ds"))));
        assertThat(res1.getResultList().stream().map(s -> s.getId()).collect(Collectors.toList()), contains(
                testDatasets.get(0).getId(),
                testDatasets.get(1).getId(),
                testDatasets.get(2).getId(),
                testDatasets.get(3).getId(),
                testDatasets.get(4).getId()
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
    public void persistDataset() {
        DataSet testDataset = createTestDataset("ds1");
        testDao.save(testDataset);
        DataSet retrievedDatasets = testDao.findById(testDataset.getId());
        assertThat(retrievedDatasets, not(isNull(DataSet.class)));
        assertThat(retrievedDatasets, not(same(testDataset)));
        assertThat(retrievedDatasets.getId(), allOf(
                not(isNull(Long.class)),
                equalTo(testDataset.getId())
        ));
    }

    @Test
    public void updateDataset() {
        DataSet testDataset = createTestDataset("ds1");
        testDao.save(testDataset);
        testDataset.setOwnerKey("subject:verify");
        testDao.update(testDataset);
        DataSet retrievedDataSet = testDao.findById(testDataset.getId());
        assertThat(retrievedDataSet, hasProperty("ownerKey", equalTo("subject:verify")));
        assertThat(retrievedDataSet, hasProperty("name", equalTo(testDataset.getName())));
    }

    protected List<DataSet> createMultipleTestItems(int nItems) {
        List<DataSet> testItems = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            testItems.add(createTestDataset("ds" + (i + 1)));
        }
        return testItems;
    }

    private DataSet createTestDataset(String dataset) {
        DataSet testDataset = new DataSet();
        Date currentTime = new Date();
        testDataset.setIdentifier(dataset + "Id");
        testDataset.setName(dataset);
        testDataset.setSageConfigPath(dataset);
        testDataset.setSageGrammarPath(dataset);
        testDataset.setCreationDate(currentTime);
        testDataset.setOwnerKey(TEST_OWNER_KEY);
        testData.add(testDataset);
        return testDataset;
    }

}
