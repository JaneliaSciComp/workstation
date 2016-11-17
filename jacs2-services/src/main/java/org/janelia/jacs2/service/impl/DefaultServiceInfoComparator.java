package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.Comparator;

public class DefaultServiceInfoComparator implements Comparator<ServiceComputation> {

    @Override
    public int compare(ServiceComputation sc1, ServiceComputation sc2) {
        ServiceInfo si1 = sc1.getComputationInfo();
        ServiceInfo si2 = sc2.getComputationInfo();
        if (si1.priority() < si2.priority() || si1.priority() > si2.priority()) {
            return si1.priority() - si2.priority();
        } else {
            return si1.getCreationDate().compareTo(si2.getCreationDate());
        }
    }

}
