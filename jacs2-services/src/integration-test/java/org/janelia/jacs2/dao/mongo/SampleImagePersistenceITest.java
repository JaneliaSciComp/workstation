package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.jacs2.model.domain.sample.LSMSampleImage;
import org.janelia.jacs2.model.domain.sample.SampleImage;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;

public class SampleImagePersistenceITest extends AbstractMongoDaoITest<SampleImage> {
    private List<SampleImage> testData = new ArrayList<>();
    @InjectMocks
    private SampleImageDao testDao;

    @Before
    public void setUp() {
        testDao = new SampleImageMongoDao(testMongoDatabase);
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void persistLSMs() {
        List<SampleImage> testLSMs = createMultipleTestItems();
        testLSMs.forEach(si -> testDao.save(si));
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("line", SortDirection.ASC)));
        PageResult<SampleImage> res = testDao.findAll(pageRequest);

        assertThat(res.getResultList(), everyItem(
                allOf(
                        Matchers.<SampleImage>instanceOf(LSMSampleImage.class),
                        isIn(testLSMs)
                )));
    }

    @Test
    public void findByOwner() {
        findByOwner(testDao);
    }

    @Test
    public void findByIds() {
        findByIds(testDao);
    }

    @Override
    protected List<SampleImage> createMultipleTestItems() {
        return ImmutableList.of(
                createLSM("l1", "a1"),
                createLSM("l2", "a2"),
                createLSM("l3", "a3"),
                createLSM("l4", "a4"),
                createLSM("l5", "a5"),
                createLSM("l6", "a6"),
                createLSM("l7", "a7"),
                createLSM("l8", "a8"),
                createLSM("l9", "a9")
        );
    }

    private LSMSampleImage createLSM(String line, String area) {
        LSMSampleImage lsmImage = new LSMSampleImage();
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
