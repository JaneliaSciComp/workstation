package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;

import java.util.Comparator;

public class DefaultServiceInfoComparator implements Comparator<QueuedTask> {

    @Override
    public int compare(QueuedTask sc1, QueuedTask sc2) {
        TaskInfo si1 = sc1.getTaskInfo();
        TaskInfo si2 = sc2.getTaskInfo();
        if (si1.priority() < si2.priority() || si1.priority() > si2.priority()) {
            return si1.priority() - si2.priority();
        } else {
            return si1.getCreationDate().compareTo(si2.getCreationDate());
        }
    }

}
