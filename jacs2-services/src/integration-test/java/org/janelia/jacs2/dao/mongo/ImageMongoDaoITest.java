package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;

public class ImageMongoDaoITest extends AbstractDomainObjectDaoITest<Image> {
    private List<Image> testData = new ArrayList<>();
    private SampleImageDao testDao;

    @Before
    public void setUp() {
        testDao = new SampleImageMongoDao(testMongoDatabase);
        setIdGeneratorAndObjectMapper((SampleImageMongoDao) testDao);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void persistLSMs() {
        List<Image> testLSMs = createMultipleTestItems(10);
        testLSMs.forEach(si -> testDao.save(si));
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("line", SortDirection.ASC)));
        PageResult<Image> res = testDao.findAll(pageRequest);

        assertThat(res.getResultList(), everyItem(
                allOf(
                        Matchers.<Image>instanceOf(LSMImage.class),
                        isIn(testLSMs)
                )));
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

    @Override
    protected List<Image> createMultipleTestItems(int nItems) {
        List<Image> testItems = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            testItems.add(createLSM("l" + (i + 1), "a" + (i + 1)));
        }
        return testItems;
    }

    private LSMImage createLSM(String line, String area) {
        LSMImage lsmImage = new LSMImage();
        lsmImage.setChannelColors("cygkrgb");
        lsmImage.setChannelDyeNames("dye");
        lsmImage.setBrightnessCompensation("compensated");
        lsmImage.setSageId(dataGenerator.nextInt());
        lsmImage.setLine(line);
        lsmImage.setAnatomicalArea(area);
        lsmImage.setOwnerKey(TEST_OWNER_KEY);
        testData.add(lsmImage);
        return lsmImage;
    }
}
