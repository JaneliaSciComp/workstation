/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
