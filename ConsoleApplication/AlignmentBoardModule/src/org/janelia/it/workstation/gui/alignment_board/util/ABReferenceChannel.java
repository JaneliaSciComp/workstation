package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

public class ABReferenceChannel extends ABItem {
    public static final String REF_CHANNEL_TYPE_NAME = "Reference";

    public ABReferenceChannel(Sample sample) {
        super(sample);
    }

    public Integer getNumber() {
        return ((NeuronFragment)domainObject).getNumber();
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
        return REF_CHANNEL_TYPE_NAME;
    }
}