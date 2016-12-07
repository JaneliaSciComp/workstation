package org.janelia.jacs2.service.dataservice.sample;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.janelia.jacs2.dao.DaoFactory;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.jacs2.dao.SubjectDao;
import org.janelia.jacs2.model.domain.Reference;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.model.domain.sample.LSMSampleImage;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.domain.sample.SampleObjective;
import org.janelia.jacs2.model.domain.sample.SampleTile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

public class SampleDataServiceTest {

    private static final Number TEST_SAMPLE_ID = 1L;
    private static final String TEST_SUBJECT = "testSubject";
    private static final String TEST_OBJECTIVE = "testObjective";

    @Mock
    private Logger logger;
    @Mock
    private SampleDao sampleDao;
    @Mock
    private SubjectDao subjectDao;
    @Mock
    private SampleImageDao sampleImageDao;
    @Mock
    private DaoFactory daoFactory;
    @InjectMocks
    private SampleDataService testService;

    @Before
    public void setUp() {
        testService = new SampleDataService();
        MockitoAnnotations.initMocks(this);
        when(daoFactory.createDomainObjectDao("LSMImage")).thenAnswer(invocation -> sampleImageDao);
    }

    @Test
    public void retrieveSampleWhenNoSubjectIsSpecified() {
        when(sampleDao.findById(TEST_SAMPLE_ID)).thenReturn(createTestSample());
        Sample testSample = testService.getSampleById(null, TEST_SAMPLE_ID);
        assertNotNull(testSample);
    }

    @Test
    public void retrieveSampleWhenSubjectDoesNotHaveReadPerms() {
        when(sampleDao.findById(TEST_SAMPLE_ID)).thenReturn(createTestSample());
        when(subjectDao.findByName(TEST_SUBJECT)).thenReturn(createTestSubject("other"));
        assertThatThrownBy(() -> testService.getSampleById(TEST_SUBJECT, TEST_SAMPLE_ID))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Subject user:other does not have read access to sample: " + TEST_SAMPLE_ID);
    }

    @Test
    public void retrieveSampleWhenSubjectHasReadPerms() {
        when(sampleDao.findById(TEST_SAMPLE_ID)).thenReturn(createTestSample());
        when(subjectDao.findByName(TEST_SUBJECT)).thenReturn(createTestSubject("other", "testGroup"));
        Sample testSample = testService.getSampleById(TEST_SUBJECT, TEST_SAMPLE_ID);
        assertNotNull(testSample);
    }

    @Test
    public void missingLSMsGenerateExceptionOnGetAnatomicalArea() {
        when(sampleDao.findById(TEST_SAMPLE_ID)).thenReturn(createTestSample(createSampleTile(1L, 2L)));
        assertThatThrownBy(() -> testService.getAnatomicalAreaBySampleIdAndObjective(null, TEST_SAMPLE_ID, TEST_OBJECTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No LSM found for LSMImage#1");
        when(sampleImageDao.findByIds(null, new ArrayList<>(ImmutableList.of(1L, 2L)))).thenReturn(ImmutableList.of(createLSMImage(1L, null)));
        assertThatThrownBy(() -> testService.getAnatomicalAreaBySampleIdAndObjective(null, TEST_SAMPLE_ID, TEST_OBJECTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No LSM found for LSMImage#2");
    }

    @Test
    public void getAnatomicalAreaWhenAllLSMsAreFound() {
        when(sampleDao.findById(TEST_SAMPLE_ID)).thenReturn(createTestSample(
                createSampleTile(1L, 2L),
                createSampleTile(3L),
                createSampleTile(5L, 6L)));
        when(sampleImageDao.findByIds(null, new ArrayList<>(ImmutableList.of(1L, 2L, 3L, 5L, 6L))))
                .thenReturn(
                        ImmutableList.of(
                                createLSMImage(1L, null),
                                createLSMImage(2L, null),
                                createLSMImage(3L, null),
                                createLSMImage(5L, 2),
                                createLSMImage(6L, null)
                        ));
        Optional<AnatomicalArea> anatomicaAreaResult = testService.getAnatomicalAreaBySampleIdAndObjective(null, TEST_SAMPLE_ID, TEST_OBJECTIVE);
        assertTrue(anatomicaAreaResult.isPresent());
        assertEquals(3, anatomicaAreaResult.get().getTileLsmPairs().size());
        assertEquals(1L, anatomicaAreaResult.get().getTileLsmPairs().get(0).getFirstLsm().getId());
        assertEquals(2L, anatomicaAreaResult.get().getTileLsmPairs().get(0).getSecondLsm().getId());
        assertEquals(3L, anatomicaAreaResult.get().getTileLsmPairs().get(1).getFirstLsm().getId());
        assertNull(anatomicaAreaResult.get().getTileLsmPairs().get(1).getSecondLsm());
        assertEquals(6L, anatomicaAreaResult.get().getTileLsmPairs().get(2).getFirstLsm().getId());
        assertEquals(5L, anatomicaAreaResult.get().getTileLsmPairs().get(2).getSecondLsm().getId());
    }

    private Sample createTestSample(SampleTile... sampleTiles) {
        Sample testSample = new Sample();
        testSample.setOwnerKey("user:testUser");
        testSample.addReader("group:testGroup");
        testSample.setName("test sample name");

        SampleObjective testObjective = new SampleObjective();
        testObjective.setObjective(TEST_OBJECTIVE);
        testObjective.addTiles(sampleTiles);
        testSample.addObjective(testObjective);
        return testSample;
    }

    private SampleTile createSampleTile(Long... lsmIds) {
        SampleTile sampleTile = new SampleTile();
        for (Long lsmId : lsmIds) {
            sampleTile.addLsmReference(new Reference("LSMImage", lsmId));
        }
        return sampleTile;
    }

    private LSMSampleImage createLSMImage(Long lsmId, Integer numChannels) {
        LSMSampleImage sampleImage = new LSMSampleImage();
        sampleImage.setId(lsmId);
        sampleImage.setNumChannels(numChannels);
        return sampleImage;
    }

    private Subject createTestSubject(String name, String... groups) {
        Subject testSubject = new Subject();
        testSubject.setName(name);
        testSubject.setKey("user:" + name);
        testSubject.setEmail(name +"@example.com");
        for (String group : groups) {
            testSubject.addGroup("group:" + group);
        }
        return testSubject;
    }
}
