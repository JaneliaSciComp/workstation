package org.janelia.horta.volume;

import java.util.Map;
import org.janelia.gltools.texture.Texture3d;

/**
 *
 * @author Christopher Bruns
 */
public interface VolumeBrickCache 
extends Map<BrickInfo, Texture3d>
{
    // Special method for tiles we do not want to fall from the cache
    Texture3d putPersistently(BrickInfo brickInfo, Texture3d volumeRaster);
}
