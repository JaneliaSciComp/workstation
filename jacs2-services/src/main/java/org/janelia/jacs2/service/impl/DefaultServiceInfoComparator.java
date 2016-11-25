package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;

import java.util.Comparator;

public class DefaultServiceInfoComparator implements Comparator<TaskInfo> {

    @Override
    public int compare(TaskInfo ti1, TaskInfo ti2) {
        if (ti1.priority() < ti2.priority() || ti1.priority() > ti2.priority()) {
            return ti1.priority() - ti2.priority();
        } else {
            return ti1.getCreationDate().compareTo(ti2.getCreationDate());
        }
    }

}
