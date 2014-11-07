package org.janelia.it.workstation.gui.viewer3d.loader;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * This may be extended for any data builder that needs a loci read method.
 */
public class LociTextureBuilder extends TextureDataBuilder implements VolumeFileLoaderI {
    
    @Override
    public TextureDataI createTextureDataBean() {
        AbstractVolumeFileLoader volumeFileLoader = super.getVolumeFileLoader();
        final int[] argbTextureIntArray = volumeFileLoader.getArgbTextureIntArray();
        final byte[] textureByteArray = volumeFileLoader.getTextureByteArray();
        if ( argbTextureIntArray != null ) {
            return new TextureDataBean(argbTextureIntArray, volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        else if ( textureByteArray != null ) {
            return new TextureDataBean(textureByteArray, volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        else {
            return null;
        }
    }

    @Override
    public void loadVolumeFile(String fileName) throws Exception {
        AbstractVolumeFileLoader volumeFileLoader = super.getVolumeFileLoader();
        volumeFileLoader.loadVolumeFile(fileName);
    }

}
