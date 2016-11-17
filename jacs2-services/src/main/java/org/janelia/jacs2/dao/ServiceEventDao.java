package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.service.ServiceEvent;

import java.util.List;

public interface ServiceEventDao extends Dao<ServiceEvent, Long> {
    List<ServiceEvent> findAllEventsByServiceId(Long serviceId);
}
