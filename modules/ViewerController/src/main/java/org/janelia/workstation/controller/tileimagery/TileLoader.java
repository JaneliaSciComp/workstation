package org.janelia.workstation.controller.tileimagery;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.rendering.RenderedVolumeLocation;

import java.net.URL;

abstract public class TileLoader {

    protected boolean loadDataFromURL(URL url) {
        return true;
    }

    public boolean loadData (TmSample sample) {
        return false;
    }

    RenderedVolumeLocation getRenderedVolumeLocation(TmSample tmSample) {
        return null;
    }
}
