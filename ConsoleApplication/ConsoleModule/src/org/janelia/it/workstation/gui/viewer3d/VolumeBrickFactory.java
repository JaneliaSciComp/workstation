package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/3/13
 * Time: 4:36 PM
 *
 * Implement this to take inputs destined for a constructor, and make the right volume brick.
 */
public interface VolumeBrickFactory {
    VolumeBrickI getVolumeBrick( VolumeModel model );
    VolumeBrickI getVolumeBrick( VolumeModel model, TextureDataI maskTextureData, TextureDataI colorMapTextureData );
    VolumeBrickI getPartialVolumeBrick( VolumeModel model, TextureDataI signalTexureData, TextureDataI maskTextureData, TextureDataI colorMapTextureData, int partNum );
}
