package org.janelia.workstation.controller.tileimagery;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.rendering.RenderedVolumeLocation;

import java.net.URL;

abstract public class TileLoader {
    URL url;

    protected boolean loadDataFromURL(URL url) {
        return true;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public boolean loadData (TmSample sample) {
        return false;
    }

    public RenderedVolumeLocation getRenderedVolumeLocation(TmSample tmSample) {
        return null;
    }
}
