package org.janelia.jacs2.dataservice;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.dao.DaoFactory;
import org.janelia.jacs2.dao.DomainObjectDao;

import javax.inject.Inject;
import java.util.stream.Stream;

public class DomainObjectService {

    private final DaoFactory daoFactory;

    @Inject
    public DomainObjectService(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public Stream<DomainObject> streamAllReferences(Subject subject, Stream<Reference> refStream) {
        return refStream.collect(
                ArrayListMultimap::<String, Number>create,
                (m, ref) -> m.put(ref.getTargetClassname(), ref.getTargetId()),
                (m1, m2) -> m1.putAll(m2)).asMap().entrySet().stream().flatMap(e -> {
                    DomainObjectDao<?> dao = daoFactory.createDomainObjectDao(e.getKey());
                    return dao.findByIds(subject, ImmutableList.copyOf(e.getValue())).stream();
                });
    }

}
