package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

public class ABCompartment extends ABItem {
    private Compartment compartment;

    public ABCompartment(Compartment compartment) {
        super(compartment.getParent());
        this.compartment = compartment;
    }

    public Long getId() {
        return compartment.getId();
    }

    public String getName() {
        return compartment.getName()+" ("+compartment.getCode()+")";
    }

    public Integer getNumber() {
        return ((Compartment)domainObject).getNumber();
    }

    public String getDefaultColor() {
        return compartment.getColor();
    }

    @Override
    public String getMaskPath() {
        return DomainUtils.getFilepath((NeuronFragment)domainObject, FileType.MaskFile);
    }

    @Override
    public String getChanPath() {
        return DomainUtils.getFilepath((NeuronFragment)domainObject, FileType.ChanFile);
    }

    @Override
    public String getType() {
        return "Compartment";
    }
}
