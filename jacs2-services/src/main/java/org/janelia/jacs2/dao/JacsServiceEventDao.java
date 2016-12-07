package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.service.JacsServiceEvent;

import java.util.List;

public interface JacsServiceEventDao extends Dao<JacsServiceEvent, Long> {
    List<JacsServiceEvent> findAllEventsByServiceId(Long serviceId);
}
