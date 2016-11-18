package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.service.TaskEvent;

import java.util.List;

public interface TaskEventDao extends Dao<TaskEvent, Long> {
    List<TaskEvent> findAllEventsByTaskId(Long serviceId);
}
