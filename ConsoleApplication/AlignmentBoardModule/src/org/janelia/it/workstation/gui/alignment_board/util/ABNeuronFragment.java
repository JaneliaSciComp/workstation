package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

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