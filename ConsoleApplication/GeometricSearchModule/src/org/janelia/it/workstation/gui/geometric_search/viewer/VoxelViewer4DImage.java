package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 8/17/2015.
 */
public class VoxelViewer4DImage implements VolumeDataAcceptor {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewer4DImage.class);

    int size[];
    byte[][] data8;
    short[][] data16;
    int voxelByteCount=0;
    boolean is12bits=true;

    public int[] getSize() {
        return size;
    }

    public int getVoxelByteCount() {
        return voxelByteCount;
    }

    public byte[] getData8ForChannel(int channel) {
        return data8[channel];
    }

    public short[] getData16ForChannel(int channel) {
        return data16[channel];
    }

    @Override
    public void setPrimaryTextureData(TextureDataI textureData) {
        if (textureData==null) return;

        if ( textureData.getTextureData().getVolumeChunks() == null ) {
            logger.info("No entries found in textureData from file="+textureData.getFilename());
            return;
        }

        logger.info("Texture data - x size="+textureData.getSx());
        logger.info("Texture data - y size="+textureData.getSy());
        logger.info("Texture data - z size="+textureData.getSz());
        logger.info("Texture data - c size="+textureData.getChannelCount());
        logger.info("Texture data - v size="+textureData.getPixelByteCount());
        logger.info("Texture data - InternalFormat="+ TextureMediator.getConstantName(textureData.getExplicitInternalFormat()));
        logger.info("Texture data - VoxelComponentOrder="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentOrder()));
        logger.info("Texture data - ComponentType="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentType()));

        voxelByteCount=textureData.getPixelByteCount();

        if (voxelByteCount!=1 && voxelByteCount!=2) {
            logger.info("Can only handle 1 or 2 bytes per voxel (8 or 16-bit)");
            return;
        }

        int channelUnitCount=textureData.getSx() * textureData.getSy() * textureData.getSz();
        int channelSizeInBytes=voxelByteCount * channelUnitCount;


        int maxSize=Integer.MAX_VALUE-5;
        if (channelSizeInBytes>maxSize) {
            logger.info("Exceeded maximum supported channel size of "+maxSize);
        }

        logger.info("Using texture channel byte count="+channelSizeInBytes);

        size=new int[4];
        size[0]=textureData.getChannelCount();
        size[1]=textureData.getSz();
        size[2]=textureData.getSy();
        size[3]=textureData.getSx();

        for (int c=0;c<textureData.getChannelCount();c++) {
            if (voxelByteCount==1) {
                data8[c]=new byte[channelUnitCount];
            } else {
                data16[c]=new short[channelUnitCount];
            }
        }

        VolumeDataChunk[] volumeChunks = textureData.getTextureData().getVolumeChunks();
        int chunkIndex=0;
        int chunkOffset=0;

        byte[] channelBuffer = new byte[channelSizeInBytes];

        for (int channelIndex=0;channelIndex<textureData.getChannelCount();channelIndex++) {
            logger.info("Populating texture for channel=" + channelIndex);

            int channelOffset = 0;

            while (channelOffset < channelSizeInBytes) {
                if (chunkIndex == volumeChunks.length) {
                    logger.error("Unexpectedly ran out of VolumeDataChunk indices");
                    return;
                }
                byte[] chunkData = volumeChunks[chunkIndex].getData();
                logger.info("Retrieved " + chunkData.length + " bytes from chunk index=" + chunkIndex);
                int dataLengthNeeded = channelSizeInBytes - channelOffset;
                int chunkDataAvailable = chunkData.length - chunkOffset;
                if (chunkDataAvailable >= dataLengthNeeded) {
                    System.arraycopy(chunkData, chunkOffset, channelBuffer, channelOffset, dataLengthNeeded);
                    channelOffset += dataLengthNeeded;
                    chunkOffset += dataLengthNeeded;
                    if (chunkOffset == chunkData.length) {
                        chunkIndex++;
                    }
                } else {
                    System.arraycopy(chunkData, chunkOffset, channelBuffer, channelOffset, chunkDataAvailable);
                    channelOffset += chunkDataAvailable;
                    chunkOffset += chunkDataAvailable;
                    chunkIndex++;
                }
            }

            logger.info("Read " + channelOffset + " bytes for channel " + channelIndex);

            if (voxelByteCount == 1) {
                System.arraycopy(channelBuffer, 0, data8[channelIndex], 0, channelSizeInBytes);
            } else {
                for (int i = 0; i < channelUnitCount; i++) {
                    int p = i * 2;
                    int i0 = channelBuffer[p]     & 0x000000ff;
                    int i1 = channelBuffer[p + 1] & 0x000000ff;
                    i1 = i1 << 8;
                    int i2 = i1 | i0;
                    short s = ((short) (i2 & 0x0000ffff));
                    if (s>4095 || s<0) {
                        is12bits=false;
                    }
                    data16[channelIndex][i] = s;
                }
            }
        }

    }

    @Override
    public void addTextureData(TextureDataI textureData) {
        // do nothing
    }

}
