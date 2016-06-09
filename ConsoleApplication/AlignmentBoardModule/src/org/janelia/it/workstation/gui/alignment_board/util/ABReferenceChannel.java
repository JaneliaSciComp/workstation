package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

public class ABReferenceChannel extends ABItem {
    public static final String REF_CHANNEL_TYPE_NAME = "Reference";

    public ABReferenceChannel(Sample sample) {
        super(sample);
    }

    @Override
    public String getMaskPath() {
        return DomainUtils.getFilepath((HasFiles)domainObject, FileType.MaskFile);
    }

    @Override
    public String getChanPath() {
        return DomainUtils.getFilepath((HasFiles)domainObject, FileType.ChanFile);
    }

    @Override
    public String getType() {
        return REF_CHANNEL_TYPE_NAME;
    }
}