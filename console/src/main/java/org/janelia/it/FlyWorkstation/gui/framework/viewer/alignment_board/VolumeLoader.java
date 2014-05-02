package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.jacs.compute.access.loader.renderable.MaskChanRenderableData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/15/13
 * Time: 2:23 PM
 *
 * Call this to load the volume.
 */
public interface VolumeLoader {
    void loadVolume( MaskChanRenderableData metaData ) throws Exception;
}
