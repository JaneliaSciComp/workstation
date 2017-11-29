package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.model.domain.sample.Sample;

public class ABSample extends ABItem {

    public ABSample(Sample sample) {
        super(sample);
    }

    @Override
    public String getType() {
        return "Sample";
    }
}