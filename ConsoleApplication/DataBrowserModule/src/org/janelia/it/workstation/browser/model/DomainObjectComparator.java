package org.janelia.it.workstation.browser.model;

import java.util.Comparator;

import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.model.domain.DomainObject;

import com.google.common.collect.ComparisonChain;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectComparator implements Comparator<DomainObject> {
    @Override
    public int compare(DomainObject o1, DomainObject o2) {
        return ComparisonChain.start()
                .compareTrueFirst(ClientDomainUtils.isOwner(o1), ClientDomainUtils.isOwner(o2))
                .compare(o1.getOwnerKey(), o2.getOwnerKey())
                .compare(o1.getId(), o2.getId()).result();
    }
};
