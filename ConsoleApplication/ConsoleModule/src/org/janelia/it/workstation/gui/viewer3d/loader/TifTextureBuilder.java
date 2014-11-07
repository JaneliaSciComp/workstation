package org.janelia.it.workstation.gui.viewer3d.loader;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Handles TIFF via Loci reading capability.
 */
public class TifTextureBuilder extends TextureDataBuilder implements VolumeFileLoaderI {
    
    @Override
    public TextureDataI createTextureDataBean() {
        TextureDataBean textureDataBean;
        AbstractVolumeFileLoader volumeFileLoader = super.getVolumeFileLoader();
        if ( volumeFileLoader.getPixelBytes() < 4 ) {
            textureDataBean = new TextureDataBean( volumeFileLoader.getTextureByteArray(), volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        else {
            textureDataBean = new TextureDataBean( volumeFileLoader.getArgbTextureIntArray(), volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        textureDataBean.setPixelByteCount(volumeFileLoader.getPixelBytes());
        return textureDataBean;
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        getVolumeFileLoader().loadVolumeFile(fileName);
    }
}
