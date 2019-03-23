package org.janelia.it.workstation.gui.viewer3d.loader;

import java.util.List;
import org.janelia.it.jacs.shared.img_3d_loader.VolumeFileLoaderI;
import org.janelia.it.jacs.shared.img_3d_loader.AbstractVolumeFileLoader;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.PiecewiseVolumeDataBean;

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
        final List<byte[]> textureByteArrays = volumeFileLoader.getTextureByteArrays();
        if ( argbTextureIntArray != null ) {
            return new TextureDataBean(argbTextureIntArray, volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        else if ( textureByteArray != null ) {
            return new TextureDataBean(textureByteArray, volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
        }
        else if ( textureByteArrays != null ) {
            final int pixelByteCount = 4;
            final int slicesPerSlab = 32;
            // Build up a Texture Data.
            //  Expect 4 bytes (ARGB) if a huge data file is presented.
            PiecewiseVolumeDataBean volumeDataBean = new PiecewiseVolumeDataBean( volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz(), pixelByteCount, slicesPerSlab);
            for (byte[] nextTextureByteArray: textureByteArrays) {
                volumeDataBean.addData(nextTextureByteArray);                
            }
            TextureDataI textureData = new TextureDataBean( volumeDataBean, volumeFileLoader.getSx(), volumeFileLoader.getSy(), volumeFileLoader.getSz() );
            textureData.setPixelByteCount(pixelByteCount);
            textureData.setChannelCount(1);  // Force channel count to one.
            
            return textureData;
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
