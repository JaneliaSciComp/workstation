/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * This is an adapter to allow easy implementation of the vbf interface.
 * 
 * @author Leslie L Foster
 */
public abstract class AbstractVolumeBrickFactory implements VolumeBrickFactory {

    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model) {
        return null;
    }

    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI colorMapTextureData) {
        return null;
    }

    @Override
    public VolumeBrickI getPartialVolumeBrick(VolumeModel model, TextureDataI signalTexureData, TextureDataI maskTextureData, TextureDataI colorMapTextureData, int partNum) {
        return null;
    }
    
}
