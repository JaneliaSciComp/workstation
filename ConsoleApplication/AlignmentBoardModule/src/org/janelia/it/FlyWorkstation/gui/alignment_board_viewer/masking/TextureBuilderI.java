package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.janelia.it.FlyWorkstation.gui.alignment_board.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/1/13
 * Time: 3:08 PM
 *
 * Implement this to create texture data objects suitable for Mip3d.
 */
public interface TextureBuilderI extends MaskChanDataAcceptorI {
    TextureDataI buildTextureData();
}
