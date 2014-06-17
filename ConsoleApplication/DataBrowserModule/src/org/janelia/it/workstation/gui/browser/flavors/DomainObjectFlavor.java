package org.janelia.it.workstation.gui.browser.flavors;

import java.awt.datatransfer.DataFlavor;
import org.janelia.it.jacs.model.domain.DomainObject;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectFlavor extends DataFlavor {

    public static final DataFlavor DOMAIN_OBJECT_FLAVOR = new DomainObjectFlavor();

    public DomainObjectFlavor() {
         super(DomainObject.class, "DomainObject");
    }

} 