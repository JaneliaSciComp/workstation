package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.NeuronFragment;

public class ABNeuronFragment extends ABItem {

    public ABNeuronFragment(NeuronFragment fragment) {
        super(fragment);
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
        return "NeuronFragment";
    }
}