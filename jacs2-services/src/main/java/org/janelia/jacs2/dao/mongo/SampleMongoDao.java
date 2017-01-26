package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.DomainModelUtils;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;

public class SampleMongoDao extends AbstractDomainObjectDao<Sample> implements SampleDao {
    @Inject
    public SampleMongoDao(MongoDatabase mongoDatabase, TimebasedIdentifierGenerator idGenerator, ObjectMapper objectMapper) {
        super(mongoDatabase, idGenerator, objectMapper);
    }

    @Override
    public PageResult<Sample> findMatchingSamples(Subject subject, Sample pattern, DataInterval<Date> tmogInterval, PageRequest pageRequest) {
        ImmutableList.Builder<Bson> filtersBuilder = new ImmutableList.Builder<>();
        if (pattern.getId() != null) {
            filtersBuilder.add(eq("_id", pattern.getId()));
        }
        if (StringUtils.isNotBlank(pattern.getName())) {
            filtersBuilder.add(eq("name", pattern.getName()));
        }
        if (StringUtils.isNotBlank(pattern.getOwnerKey())) {
            filtersBuilder.add(eq("ownerKey", pattern.getOwnerKey()));
        }
        if (StringUtils.isNotBlank(pattern.getAge())) {
            filtersBuilder.add(eq("age", pattern.getAge()));
        }
        if (StringUtils.isNotBlank(pattern.getEffector())) {
            filtersBuilder.add(eq("effector", pattern.getEffector()));
        }
        if (StringUtils.isNotBlank(pattern.getDataSet())) {
            filtersBuilder.add(eq("dataSet", pattern.getDataSet()));
        }
        if (StringUtils.isNotBlank(pattern.getLine())) {
            filtersBuilder.add(eq("line", pattern.getLine()));
        }
        if (StringUtils.isNotBlank(pattern.getSlideCode())) {
            filtersBuilder.add(eq("slideCode", pattern.getSlideCode()));
        }
        if (StringUtils.isNotBlank(pattern.getGender())) {
            filtersBuilder.add(eq("gender", pattern.getGender()));
        }
        if (StringUtils.isNotBlank(pattern.getStatus())) {
            filtersBuilder.add(eq("status", pattern.getStatus()));
        }
        if (tmogInterval.hasFrom()) {
            filtersBuilder.add(gte("tmogDate", tmogInterval.getFrom()));
        }
        if (tmogInterval.hasTo()) {
            filtersBuilder.add(lt("tmogDate", tmogInterval.getTo()));
        }
        if (DomainModelUtils.isNotAdmin(subject)) {
            filtersBuilder.add(createSubjectPermissionFilter(subject));
        }

        ImmutableList<Bson> filters = filtersBuilder.build();

        Bson bsonFilter = null;
        if (!filters.isEmpty()) bsonFilter = and(filters);
        List<Sample> results = find(bsonFilter, createBsonSortCriteria(pageRequest.getSortCriteria()), pageRequest.getOffset(), pageRequest.getPageSize(), Sample.class);
        return new PageResult<>(pageRequest, results);
    }
}
