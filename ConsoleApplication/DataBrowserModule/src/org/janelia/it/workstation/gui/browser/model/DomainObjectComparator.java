package org.janelia.it.workstation.gui.browser.model;

import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectComparator implements Comparator<DomainObject> {
    @Override
    public int compare(DomainObject o1, DomainObject o2) {
        return ComparisonChain.start()
                .compareTrueFirst(DomainUtils.isOwner(o1), DomainUtils.isOwner(o2))
                .compare(o1.getOwnerKey(), o2.getOwnerKey())
                .compare(o1.getId(), o2.getId()).result();
    }
};
