package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.jacs2.dao.ImageDao;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

public class ImageMongoDaoITest extends AbstractDomainObjectDaoITest<Image> {
    private List<Image> testData = new ArrayList<>();
    private ImageDao testDao;

    @Before
    public void setUp() {
        testDao = new ImageMongoDao(testMongoDatabase, idGenerator, testObjectMapperFactory);
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
    }

    @Test
    public void persistLSMWithSmallerFileSize() {
        LSMImage testImage = createLSM("line", "area");
        testImage.setFileSize(100L);
        testDao.save(testImage);
        Image retrievedImage = testDao.findById(testImage.getId());
        assertThat(retrievedImage, equalTo(testImage));
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

    @Test
    public void updateImageFiles() {
        Image testLSM = createLSM("line", "area");
        testDao.save(testLSM);
        testLSM.setFileName(FileType.ChanFile, "chan");
        testLSM.setFileName(FileType.MaskFile, "mask");
        testDao.updateImageFiles(testLSM);
        Image retrievedLSM = testDao.findById(testLSM.getId());
        assertThat(retrievedLSM.getFiles(), hasEntry(FileType.ChanFile, "chan"));
        assertThat(retrievedLSM.getFiles(), hasEntry(FileType.MaskFile, "mask"));
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
        lsmImage.setFileSize(8234568900000000000L);
        testData.add(lsmImage);
        return lsmImage;
    }
}
