package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;

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
