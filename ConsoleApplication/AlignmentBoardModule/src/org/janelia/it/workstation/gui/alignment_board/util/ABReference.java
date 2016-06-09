package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;

public class ABReference extends ABItem {
    private NeuronSeparation separation;

    public ABReference(NeuronSeparation separation) {
        super(separation.getParentRun().getParent().getParent());
        this.separation = separation;
    }

    public Long getId() {
        return separation.getId();
    }

    public String getName() {
        return "Reference";
    }

    @Override
    public String getMaskPath() {
        return separation.getFilepath()+"/archive/maskChan/ref.mask";
    }

    @Override
    public String getChanPath() {
        return separation.getFilepath()+"/archive/maskChan/ref.chan";
    }

    @Override
    public String getType() {
        return "Reference";
    }
}
