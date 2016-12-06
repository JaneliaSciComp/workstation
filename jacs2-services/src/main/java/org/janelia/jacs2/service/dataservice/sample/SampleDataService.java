package org.janelia.jacs2.service.dataservice.sample;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dao.Dao;
import org.janelia.jacs2.dao.DaoFactory;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.dao.SubjectDao;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.domain.sample.SampleObjective;

import javax.inject.Inject;
import java.util.Optional;

public class SampleDataService {

    @Inject
    private SampleDao sampleDao;
    @Inject
    private SubjectDao subjectDao;
    @Inject
    private DaoFactory daoFactory;

    public Sample getSampleById(String subjectName, Number sampleId) {
        return sampleDao.findById(sampleId);
    }

    public Optional<SampleObjective> getLSMsBySampleIdAndObjective(String subjectName, Number sampleId, String objective) {
        Subject subject = null;
        Sample sample = getSampleById(subjectName, sampleId);
        Optional<SampleObjective> sampleObjective = Optional.empty();

        if (StringUtils.isNotBlank(subjectName)) {
            subjectDao.findByName(subjectName);
        }

        if (sample != null) {
            sampleObjective = sample.lookupObjective(objective);
        }
        if (sampleObjective.isPresent()) {
            SampleObjective selectedObjective = sampleObjective.get();
            ListMultimap<String, Number> allLSMReferences = ArrayListMultimap.create();
            selectedObjective.getTiles().stream()
                            .flatMap(t -> t.getLsmReferences().stream())
                            .forEach(ref -> allLSMReferences.put(ref.getTargetClassname(), ref.getTargetId()));
            // TODO
            allLSMReferences.keySet().stream().flatMap(referenceName -> {
                DomainObjectDao<?> dao = daoFactory.createDomainObjectDao(referenceName);
                return dao.findByIds(subject, allLSMReferences.get(referenceName)).stream();
            });
        }
        return sampleObjective;
    }
}
