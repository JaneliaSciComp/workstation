package org.janelia.jacs2.service.dataservice.sample;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.jacs2.dao.DaoFactory;
import org.janelia.jacs2.dao.DomainObjectDao;
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
import org.janelia.jacs2.utils.DomainUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SampleDataService {

    @Inject
    private Logger logger;
    @Inject
    private SampleDao sampleDao;
    @Inject
    private SubjectDao subjectDao;
    @Inject
    private DaoFactory daoFactory;
    @Inject
    private ImageDao imageDao;

    public Sample getSampleById(String subjectName, Number sampleId) {
        Subject subject = null;
        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
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

    public Optional<AnatomicalArea> getAnatomicalAreaBySampleIdAndObjective(String subjectName, Number sampleId, String objective) {
        Preconditions.checkArgument(sampleId != null, "Sample ID must be specified for anatomical area retrieval");
        Preconditions.checkArgument(StringUtils.isNotBlank(objective), "Objective must be specified for anatomical area retrieval");
        Subject subject = null;
        Sample sample = getSampleById(subjectName, sampleId);
        Optional<ObjectiveSample> sampleObjective = Optional.empty();

        if (StringUtils.isNotBlank(subjectName)) {
            subject = subjectDao.findByName(subjectName);
        }

        if (sample != null) {
            sampleObjective = sample.lookupObjective(objective);
        }
        if (sampleObjective.isPresent()) {
            ObjectiveSample selectedObjective = sampleObjective.get();
            Map<String, LSMImage> indexedLsmSampleImages = retrieveAllLSMs(subject, selectedObjective);
            AnatomicalArea anatomicalArea = new AnatomicalArea();
            anatomicalArea.setName(sample.getName());
            anatomicalArea.setSampleId(sample.getId());
            anatomicalArea.setObjective(selectedObjective.getObjective());

            selectedObjective.getTiles().stream()
                            .forEach(t -> {
                                TileLsmPair lsmPair = getTileLsmPair(sample, selectedObjective, t, refId -> indexedLsmSampleImages.get(refId));
                                anatomicalArea.addLsmPair(lsmPair);
                            });
            return Optional.of(anatomicalArea);
        } else {
            return Optional.empty();
        }
    }

    private Map<String, LSMImage> retrieveAllLSMs(Subject subject, ObjectiveSample sampleObjective) {
        ListMultimap<String, Number> allLSMReferences = ArrayListMultimap.create();
        sampleObjective.getTiles().stream()
                .flatMap(t -> t.getLsmReferences().stream())
                .forEach(ref -> allLSMReferences.put(ref.getTargetClassname(), ref.getTargetId()));
        Map<String, LSMImage> indexedLsmResults = new LinkedHashMap<>();
        allLSMReferences.keySet().stream().flatMap(referenceName -> {
            DomainObjectDao<?> dao = daoFactory.createDomainObjectDao(referenceName);
            return dao.findByIds(subject, allLSMReferences.get(referenceName)).stream();
        }).forEach(dobj -> {
            indexedLsmResults.put(dobj.getEntityRefId(), (LSMImage) dobj);
        });
        return indexedLsmResults;
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

    public void updateLMSMetadata(LSMImage lsmImage, String lsmMetadata) {
        DomainUtils.setFileType(lsmImage, FileType.LsmMetadata, lsmMetadata);
        imageDao.updateImageFiles(lsmImage);
    }
}
