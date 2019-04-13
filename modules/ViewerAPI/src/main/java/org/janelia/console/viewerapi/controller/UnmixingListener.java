package org.janelia.console.viewerapi.controller;

/**
 * notifies interested listeners when the imagecolormodel changes in the SliderPanel
 * as well as sending the new params for handy updating.
 *
 * @author schauderd
 */
public interface UnmixingListener {
    void unmixingParametersChanged(float[] unmixingParams);
}
