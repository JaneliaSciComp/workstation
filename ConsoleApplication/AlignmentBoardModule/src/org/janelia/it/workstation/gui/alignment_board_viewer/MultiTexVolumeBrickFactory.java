package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/3/13
 * Time: 5:16 PM
 *
 * Implementation of a volume brick factory that can supply a multi-tex one, and can pre-configure the resulting class.
 */
public class MultiTexVolumeBrickFactory implements VolumeBrickFactory {
    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model) {
        MultiTexVolumeBrick volumeBrick = new MultiTexVolumeBrick( model );
        return volumeBrick;
    }

    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI
            colorMapTextureData) {
        MultiTexVolumeBrick volumeBrick = new MultiTexVolumeBrick( model );
        volumeBrick.setMaskTextureData( maskTextureData );
        volumeBrick.setColorMapTextureData( colorMapTextureData );
        return volumeBrick;
    }
}
