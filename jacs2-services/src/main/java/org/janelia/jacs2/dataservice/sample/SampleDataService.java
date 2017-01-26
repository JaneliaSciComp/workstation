package org.janelia.jacs2.dataservice.sample;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.dao.ImageDao;
import org.janelia.jacs2.dao.SubjectDao;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.sample.TileLsmPair;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.dataservice.DomainObjectService;
import org.janelia.jacs2.model.DomainModelUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SampleDataService {

    private final DomainObjectService domainObjectService;
    private final SampleDao sampleDao;
    private final SubjectDao subjectDao;
    private final ImageDao imageDao;
    private final Logger logger;

    @Inject
    public SampleDataService(DomainObjectService domainObjectService, SampleDao sampleDao, SubjectDao subjectDao, ImageDao imageDao, Logger logger) {
        this.domainObjectService = domainObjectService;
        this.sampleDao = sampleDao;
        this.subjectDao = subjectDao;
        this.imageDao = imageDao;
        this.logger = logger;
    }

    public Sample getSampleById(String subjectName, Number sampleId) {
        Subject subject = null;
        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
            Preconditions.checkArgument(subject != null);
        }
        Sample sample = sampleDao.findById(sampleId);
        if (subject != null) {
            if (sample != null && subject.canRead(sample)) {
                return sample;
            } else {
                throw new SecurityException("Subject " + subject.getKey() + " does not have read access to sample: "+ sampleId);
            }
        } else {
            return sample;
        }
    }

    public List<LSMImage> getLSMsByIds(String subjectName, List<Number> lsmIds) {
        Subject subject = null;
        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
            Preconditions.checkArgument(subject != null);
        }
        return imageDao.findSubtypesByIds(subject, lsmIds, LSMImage.class);
    }

    public List<AnatomicalArea> getAnatomicalAreasBySampleIdAndObjective(String subjectName, Number sampleId, String objective) {
        Preconditions.checkArgument(sampleId != null, "Sample ID must be specified for anatomical area retrieval");
        Subject subject = null;
        Sample sample = getSampleById(subjectName, sampleId);
        if (sample == null) {
            logger.info("Invalid sampleId {} or subject {} has no access", sampleId, subjectName);
            return Collections.emptyList();
        }
        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
        }
        final Subject currentSubject = subject;
        Map<String, LSMImage> indexedLsms = new LinkedHashMap<>();
        Predicate<ObjectiveSample> objectiveFilter = objectiveSample -> {
            if (StringUtils.isBlank(objective)) {
                return true;
            } else {
                return objective.equals(objectiveSample.getObjective());
            }
        };
        sample.getObjectiveSamples().stream()
                .filter(objectiveFilter)
                .flatMap(objectiveSample -> streamAllLSMs(currentSubject, objectiveSample))
                .forEach(lsm -> indexedLsms.put(lsm.getEntityRefId(), lsm));

        return sample.getObjectiveSamples().stream()
                .filter(objectiveFilter)
                .map(objectiveSample -> {
                        AnatomicalArea anatomicalArea = new AnatomicalArea();
                        anatomicalArea.setName(sample.getName());
                        anatomicalArea.setSampleId(sample.getId());
                        anatomicalArea.setObjective(objectiveSample.getObjective());
                        objectiveSample.getTiles().stream()
                                .forEach(t -> {
                                    TileLsmPair lsmPair = getTileLsmPair(sample, objectiveSample, t, indexedLsms::get);
                                    anatomicalArea.addLsmPair(lsmPair);
                                });
                        return anatomicalArea;
                    })
                .collect(Collectors.toList());
    }

    private Stream<LSMImage> streamAllLSMs(Subject subject, ObjectiveSample sampleObjective) {
        return domainObjectService
                .streamAllReferences(subject, sampleObjective.getTiles().stream().flatMap(t -> t.getLsmReferences().stream()))
                .map(dobj -> (LSMImage) dobj);
    }

    private TileLsmPair getTileLsmPair(Sample sample, ObjectiveSample sampleObjective, SampleTile tile, Function<String, LSMImage> lsmByRefIdRetriever) {
        Reference firstLsmRef = tile.getLsmReferenceAt(0);
        Reference secondLsmRef = tile.getLsmReferenceAt(1);
        if (firstLsmRef == null) {
            logger.error("No LSMs set for tile {} in sample {} objective {}", tile, sample, sampleObjective);
            throw new IllegalStateException("No LSMs set for tile " + tile + " in sample " + sample + " objective " + sampleObjective);
        }
        LSMImage firstLSM = lsmByRefIdRetriever.apply(firstLsmRef.getTargetRefId());
        if (firstLSM == null) {
            logger.error("No LSM found for {} - first LSM for tile {} in sample {} objective {}",
                    firstLsmRef.getTargetRefId(), tile, sample, sampleObjective);
            throw new IllegalStateException("No LSM found for " + firstLsmRef.getTargetRefId());
        }
        LSMImage secondLSM = null;
        if (secondLsmRef != null) {
            secondLSM = lsmByRefIdRetriever.apply(secondLsmRef.getTargetRefId());
            if (secondLSM == null) {
                logger.error("No LSM found for {} - second LSM for tile {} in sample {} objective {}",
                        secondLsmRef.getTargetRefId(), tile, sample, sampleObjective);
                throw new IllegalStateException("No LSM found for " + secondLsmRef.getTargetRefId());
            }
            if (Optional.ofNullable(firstLSM.getNumChannels()).orElse(0) == 2) {
                // Switch the LSMs so that the 3 channel image always comes first
                LSMImage tmp = firstLSM;
                firstLSM = secondLSM;
                secondLSM = tmp;
            }
        }
        TileLsmPair lsmPair = new TileLsmPair();
        lsmPair.setTileName(tile.getName());
        lsmPair.setFirstLsm(firstLSM);
        lsmPair.setSecondLsm(secondLSM);
        return lsmPair;
    }

    public void updateLSMMetadataFile(LSMImage lsmImage, String lsmMetadata) {
        DomainModelUtils.setPathForFileType(lsmImage, FileType.LsmMetadata, lsmMetadata);
        imageDao.updateImageFiles(lsmImage);
    }

    public PageResult<Sample> searchSamples(String subjectName, Sample pattern, DataInterval<Date> tmogInterval, PageRequest pageRequest) {
        Subject subject = null;
        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
            Preconditions.checkArgument(subject != null);
        }
        return sampleDao.findMatchingSamples(subject, pattern, tmogInterval, pageRequest);
    }


    public void updateLSM(LSMImage lsmImage) {
        imageDao.update(lsmImage);
    }
}
