package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.jacs.shared.loader.renderable.MaskChanRenderableData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/15/13
 * Time: 2:23 PM
 *
 * Call this to load the volume.
 */
public interface RenderableDataLoader {
    void loadRenderableData( MaskChanRenderableData metaData ) throws Exception;
}
