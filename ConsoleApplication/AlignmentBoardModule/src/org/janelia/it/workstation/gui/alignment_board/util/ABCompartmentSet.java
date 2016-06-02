package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;

public class ABCompartmentSet extends ABItem {

    public ABCompartmentSet(CompartmentSet compartmentSet) {
        super(compartmentSet);
    }

    @Override
    public String getType() {
        return "CompartmentSet";
    }

    public String getAlignmentSpace() {
        return ((CompartmentSet)domainObject).getAlignmentSpace();
    }

    public String getImageSize() {
        return ((CompartmentSet)domainObject).getImageSize();
    }

    public String getOpticalResolution() {
        return ((CompartmentSet)domainObject).getOpticalResolution();
    }
}
