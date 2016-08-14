package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.enums.FileType;

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
        return compartment.getNumber();
    }

    public String getDefaultColor() {
        return compartment.getColor();
    }

    @Override
    public String getMaskPath() {
        return compartment.getFiles().get(FileType.MaskFile).toString();
    }

    @Override
    public String getChanPath() {
        return compartment.getFiles().get(FileType.ChanFile).toString();
    }

    @Override
    public String getType() {
        return "Compartment";
    }
}
