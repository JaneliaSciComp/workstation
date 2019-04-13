package org.janelia.console.viewerapi.controller;

/**
 * Implement this to hear about changes to the color sliders, which are
 * channel-specific.
 *
 * @author fosterl
 */
public interface ChannelColorChangeListener {
    void blackLevelChanged(Integer blackLevel);
    void gammaChanged(Double gamma);
    void whiteLevelChanged(Integer whiteLevel);
}
