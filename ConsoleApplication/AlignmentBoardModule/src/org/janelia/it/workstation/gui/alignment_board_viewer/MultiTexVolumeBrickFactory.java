package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.nio.ByteOrder;
import java.util.Collection;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.buffering.VtxCoordBufMgr;
import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;

/**
 * Created with IntelliJ IDEA. User: fosterl Date: 9/3/13 Time: 5:16 PM
 *
 * Implementation of a volume brick factory that can supply a multi-tex one, and
 * can pre-configure the resulting class.
 */
public class MultiTexVolumeBrickFactory implements VolumeBrickFactory {
    private static final String READ_ONLY_VIOLATION = "Read Only: No Setters.";

    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model) {
        VtxCoordBufMgr bufferManager = new VtxCoordBufMgr();
        MultiTexVolumeBrick volumeBrick = new MultiTexVolumeBrick(model, bufferManager);
        return volumeBrick;
    }

    @Override
    public VolumeBrickI getVolumeBrick(VolumeModel model,
                                       TextureDataI maskTextureData,
                                       TextureDataI colorMapTextureData) {
        VtxCoordBufMgr bufferManager = new VtxCoordBufMgr();
        MultiTexVolumeBrick volumeBrick = new MultiTexVolumeBrick(model, bufferManager);
        volumeBrick.setMaskTextureData(maskTextureData);
        volumeBrick.setColorMapTextureData(colorMapTextureData);
        return volumeBrick;
    }

    /**
     * Creates a volume brick to cover only part of the volume: that chunk
     * indicated by the part-num.
     * 
     * @param model handoff to the brick.
     * @param signalTextureData larger whole from which chunk obtained.
     * @param maskTextureData larger whole from which chunk obtained.
     * @param colorMapTextureData for mapping masks to colors.
     * @param partNum chunk indicator.
     * @return ready actor.
     */
    @Override
    public VolumeBrickI getPartialVolumeBrick(VolumeModel model,
                                              TextureDataI signalTextureData,
                                              TextureDataI maskTextureData,
                                              TextureDataI colorMapTextureData,
                                              int partNum) {
        VtxCoordBufMgr bufferManager = new VtxCoordBufMgr();
        VolumeDataChunk chunk = signalTextureData.getTextureData().getVolumeChunks()[ partNum ];
        bufferManager.setSliceLimits(chunk.getStartZ(), chunk.getStartZ() + chunk.getDepth());
        MultiTexVolumeBrick volumeBrick = new MultiTexVolumeBrick(model, bufferManager);
        volumeBrick.setMaskTextureData(getSingleChunkTexture(maskTextureData, partNum));
        volumeBrick.setColorMapTextureData(colorMapTextureData);
        volumeBrick.setTextureData(getSingleChunkTexture(signalTextureData, partNum));
        return volumeBrick;
    }
    
    private TextureDataI getSingleChunkTexture( TextureDataI multiChunkTexture, int partNum ) {
        VolumeDataI chunkVolume = new SubChunkingVolumeDecorator( multiChunkTexture.getTextureData(), partNum );
        TextureDataI returnValue = new SubChunkingTexDecorator( multiChunkTexture, partNum );
        returnValue.setTextureData(chunkVolume);
        return returnValue;
    }
    
    public static class SubChunkingTexDecorator implements TextureDataI {
        private TextureDataI wrappedData;
        // These make all the difference!
        private VolumeDataI volumeData;
        private int sX;
        private int sY;
        private int sZ;
        
        private int totalChunkCount;        
        private int chunkNum;
        
        public SubChunkingTexDecorator(TextureDataI wrappedTextureData, int chunkNum) {
            this.wrappedData = wrappedTextureData;
            this.chunkNum = chunkNum;
            this.totalChunkCount = totalChunkCount;
        }

        @Override
        public void setTextureData(VolumeDataI textureData) {
            this.volumeData = textureData;
            VolumeDataChunk chunk = volumeData.getVolumeChunks()[0];
            this.sX = chunk.getWidth();
            this.sY = chunk.getHeight();
            this.sZ = chunk.getDepth();
        }

        @Override
        public VolumeDataI getTextureData() {
            return this.volumeData;
        }

        @Override
        public int getSx() {
            return sX;
        }

        @Override
        public int getSy() {
            return sY;
        }

        @Override
        public int getSz() {
            return sZ;
        }

        @Override
        public boolean isInverted() {
            return wrappedData.isInverted();
        }

        @Override
        public void setInverted(boolean inverted) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public void setSx(int sx) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public void setSy(int sy) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public void setSz(int sz) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public VolumeDataAcceptor.TextureColorSpace getColorSpace() {
            return wrappedData.getColorSpace();
        }

        @Override
        public void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public int getInterpolationMethod() {
            return wrappedData.getInterpolationMethod();
        }

        @Override
        public void setInterpolationMethod(int interpolationMethod) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Double[] getVolumeMicrometers() {
            return wrappedData.getVolumeMicrometers();
        }

        @Override
        public void setVolumeMicrometers(Double[] volumeMicrometers) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Double[] getVoxelMicrometers() {
            return wrappedData.getVoxelMicrometers();
        }

        @Override
        public void setVoxelMicrometers(Double[] voxelMicrometers) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public String getHeader() {
            return wrappedData.getHeader();
        }

        @Override
        public void setHeader(String header) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public ByteOrder getByteOrder() {
            return wrappedData.getByteOrder();
        }

        @Override
        public void setByteOrder(ByteOrder byteOrder) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public int getPixelByteCount() {
            return wrappedData.getPixelByteCount();
        }

        @Override
        public void setPixelByteCount(int pixelByteCount) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public String getFilename() {
            return wrappedData.getFilename();
        }

        @Override
        public void setFilename(String filename) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public int getChannelCount() {
            return wrappedData.getChannelCount();
        }

        @Override
        public void setChannelCount(int channelCount) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public float[] getCoordCoverage() {
            return wrappedData.getCoordCoverage();
        }

        @Override
        public void setCoordCoverage(float[] coverage) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Integer getExplicitVoxelComponentType() {
            return wrappedData.getExplicitVoxelComponentType();
        }

        @Override
        public void setExplicitVoxelComponentType(int format) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Integer getExplicitInternalFormat() {
            return wrappedData.getExplicitInternalFormat();
        }

        @Override
        public void setExplicitInternalFormat(Integer format) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Integer getExplicitVoxelComponentOrder() {
            return wrappedData.getExplicitVoxelComponentOrder();
        }

        @Override
        public void setExplicitVoxelComponentOrder(Integer order) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public void setRenderables(Collection<RenderableBean> renderables) {
            throw new UnsupportedOperationException(READ_ONLY_VIOLATION);
        }

        @Override
        public Collection<RenderableBean> getRenderables() {
            return wrappedData.getRenderables();
        }
        
    }
    
    public static class SubChunkingVolumeDecorator implements VolumeDataI {
        private VolumeDataI wrappedVolumeData;
        private int chunkNum;
        
        public SubChunkingVolumeDecorator( VolumeDataI wrappedVolumeData, int chunk ) {
            this.wrappedVolumeData = wrappedVolumeData;
            this.chunkNum = chunk;
        }

        @Override
        public boolean isVolumeAvailable() {
            return wrappedVolumeData.isVolumeAvailable();
        }

        @Override
        public VolumeDataChunk[] getVolumeChunks() {
            return new VolumeDataChunk[] {
                wrappedVolumeData.getVolumeChunks()[chunkNum]
            };
        }

        @Override
        public byte getValueAt(long location) {
            return wrappedVolumeData.getValueAt(location);
        }

        @Override
        public void setValueAt(long location, byte value) {
            wrappedVolumeData.setValueAt(location, value);
        }

        @Override
        public long length() {
            return wrappedVolumeData.length() / wrappedVolumeData.getVolumeChunks().length;
        }
        
    }
}
