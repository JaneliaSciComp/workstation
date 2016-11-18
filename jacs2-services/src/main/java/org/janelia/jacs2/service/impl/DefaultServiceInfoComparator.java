package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.Comparator;

public class DefaultServiceInfoComparator implements Comparator<QueuedService> {

    @Override
    public int compare(QueuedService sc1, QueuedService sc2) {
        ServiceInfo si1 = sc1.getServiceInfo();
        ServiceInfo si2 = sc2.getServiceInfo();
        if (si1.priority() < si2.priority() || si1.priority() > si2.priority()) {
            return si1.priority() - si2.priority();
        } else {
            return si1.getCreationDate().compareTo(si2.getCreationDate());
        }
    }

}
