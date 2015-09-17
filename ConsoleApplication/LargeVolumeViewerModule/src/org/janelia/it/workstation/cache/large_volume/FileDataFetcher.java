package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;

/**
 * This implementation of a cache toolkit, will break out the contents of 
 * a TIF file, into something more handy to deal with.  The 'TIF' question
 * is settled early on.
 * 
 * @author fosterl
 */
public class FileDataFetcher {
    public TrivialFileResolver trivialFileResolver;
    boolean zOriginNegativeShift;
    
    public FileDataFetcher( boolean zOriginNegativeShift ) {
        this.zOriginNegativeShift = zOriginNegativeShift;
        trivialFileResolver = new TrivialFileResolver();
    }

    public byte[] fetch(String id) {
        TextureDataI idData = getDataForFile(id);
        final long textureLength = idData.getTextureData().length();
        if (textureLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("length of texture data exceeds int capacity");
        }
        byte[] bytes = null;
        VolumeDataChunk[] volumeChunks = idData.getTextureData().getVolumeChunks();
        if (volumeChunks.length == 1) {
            bytes = volumeChunks[0].getData();
        }
        else {
            bytes = new byte[(int)textureLength];
            int offset = 0;
            for (VolumeDataChunk chunk : idData.getTextureData().getVolumeChunks()) {
                byte[] nextChunk = chunk.getData();
                final int chunkLength = nextChunk.length;
                
                System.arraycopy(nextChunk, 0, bytes, offset, chunkLength);
                offset += chunkLength;
            }
        }
        
        return bytes;
        
    }

    private TextureDataI getDataForFile(String filePath) {
        final TextureDataI[] textureDataHolder = new TextureDataI[1];
        VolumeLoader volumeLoader = new VolumeLoader(trivialFileResolver);
        if (volumeLoader.loadVolume(filePath)) {
            volumeLoader.populateVolumeAcceptor(new VolumeDataAcceptor() {

                @Override
                public void setPrimaryTextureData(TextureDataI textureData) {
                    textureDataHolder[0] = textureData;
                }

                @Override
                public void addTextureData(TextureDataI textureData) {
                    // Do nothing.
                }

            });
        }
        
        return textureDataHolder[0];
    }

}
