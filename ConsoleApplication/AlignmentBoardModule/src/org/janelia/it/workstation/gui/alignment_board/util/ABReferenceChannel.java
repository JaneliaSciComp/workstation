package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;

public class ABReferenceChannel extends ABItem {

    public static final String REF_CHANNEL_TYPE_NAME = "Reference";

    private NeuronSeparation separation;

    public ABReferenceChannel(NeuronSeparation separation) {
        super(separation.getParentRun().getParent().getParent());
        this.separation = separation;
    }

    public Long getId() {
        return separation.getId();
    }

    public String getName() {
        return REF_CHANNEL_TYPE_NAME;
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
        return REF_CHANNEL_TYPE_NAME;
    }
}
