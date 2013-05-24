package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:10 PM
 *
 * Implement this to make something that can build a mask texture volume suitable for use in the alignment board viewer.
 */
public interface MaskBuilderI {
    int GPU_MULTIBYTE_DIVISIBILITY_VALUE = 4;

    byte[] getVolumeData();
    Integer[] getVolumeMaskVoxels();
    ByteOrder getPixelByteOrder();
    int getPixelByteCount();
    TextureDataI getCombinedTextureData();
    VolumeDataAcceptor.TextureColorSpace getTextureColorSpace();

}
