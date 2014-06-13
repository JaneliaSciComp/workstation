package org.janelia.it.workstation.gui.viewer3d.texture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture volume.  Note that the texture offset for this is not known,
 * because masks like this can be switched on and off, changing their offsets at runtime.
 */

import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataBean;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;

import javax.media.opengl.GL2;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Collection;

public class TextureDataBean implements TextureDataI {
    private static final int INTEGER_NUM_BYTES = (Integer.SIZE / 8);

    private String remoteFilename;
    private VolumeDataI textureData;
    private Integer sx;
    private Integer sy;
    private Integer sz;

    private float[] coordCoverage = new float[] { 1.0f, 1.0f, 1.0f };

    private VolumeDataAcceptor.TextureColorSpace colorSpace;
    private Double[] volumeMicrometers;
    private Double[] voxelMicrometers;

    // These settings may be avoided for most file types.
    private String header = "Not Available";
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private int pixelByteCount = 1;
    private int channelCount = 0;

    private boolean inverted = true; // Tested stored images were inverted.
    private Integer voxelComponentFormat = UNSET_VALUE;
    private Integer internalFormat = UNSET_VALUE;
    private Integer voxelComponentOrder = UNSET_VALUE;

    private int interpolationMethod = GL2.GL_LINEAR;

    private Collection<RenderableBean> renderables;

    public TextureDataBean(VolumeDataI textureData, int sx, int sy, int sz) {
        this.textureData = textureData;
        setSx( sx );
        setSy( sy );
        setSz( sz );
    }

    public TextureDataBean(int[] argbData, int sx, int sy, int sz) {
        ByteBuffer intermediate = ByteBuffer.allocate( argbData.length * INTEGER_NUM_BYTES );
        intermediate.order( ByteOrder.LITTLE_ENDIAN );
        IntBuffer intBuffer = intermediate.asIntBuffer();
        intBuffer.put( argbData );
        byte[] array = intermediate.array();
        textureData = new VolumeDataBean( array, sx, sy, sz );
        setSx( sx );
        setSy( sy );
        setSz( sz );
    }

    @SuppressWarnings("ALL")
    @Override
    public boolean equals( Object other ) {
        if ( other == null  ||  (! (other instanceof TextureDataBean ) ) ) {
            return false;
        }
        else {
            return ((TextureDataBean) other).getFilename().equals( getFilename() );
        }
    }

    @Override
    public int hashCode() {
        return getFilename().hashCode();
    }

    @Override
    public void setTextureData( VolumeDataI textureData ) {
        this.textureData = textureData;
    }

    @Override
    public VolumeDataI getTextureData() {
        return textureData;
    }

    @Override
    public int getSx() {
        return sx;
    }

    @Override
    public int getSy() {
        return sy;
    }

    @Override
    public int getSz() {
        return sz;
    }

    @Override
    public void setSx(int sx) {
        this.sx = sx;
    }

    @Override
    public void setSy(int sy) {
        this.sy = sy;
    }

    @Override
    public void setSz(int sz) {
        this.sz = sz;
    }

    public VolumeDataAcceptor.TextureColorSpace getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }

    public Double[] getVolumeMicrometers() {
        if ( volumeMicrometers == null ) {
            volumeMicrometers = new Double[] {  (double)getSx(), (double)getSy(), (double)getSz() };
        }
        return volumeMicrometers;
    }

    public void setVolumeMicrometers(Double[] volumeMicrometers) {
        this.volumeMicrometers = volumeMicrometers;
    }

    public Double[] getVoxelMicrometers() {
        if ( voxelMicrometers == null ) {
            voxelMicrometers = new Double[] { 1.0, 1.0, 1.0 };
        }
        return voxelMicrometers;
    }

    public void setVoxelMicrometers(Double[] voxelMicrometers) {
        this.voxelMicrometers = voxelMicrometers;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public int getPixelByteCount() {
//new Exception("Pixel Byte Count is " + pixelByteCount).printStackTrace();
        return pixelByteCount;
    }

    public void setPixelByteCount(int pixelByteCount) {
//new Exception("Pixel Byte Count is " + pixelByteCount).printStackTrace();
        this.pixelByteCount = pixelByteCount;
    }

    @Override
    public String getFilename() {
        return remoteFilename;
    }

    public void setFilename(String filename) {
        this.remoteFilename = filename;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public float[] getCoordCoverage() {
        return coordCoverage;
    }

    @Override
    public void setCoordCoverage(float[] coverage) {
        this.coordCoverage = coverage;
    }

    @Override
    public Integer getExplicitVoxelComponentType() {
        return voxelComponentFormat;
    }

    @Override
    public void setExplicitVoxelComponentType(int format) {
        this.voxelComponentFormat = format;
    }

    @Override
    public void setRenderables(Collection<RenderableBean> renderables) {
        this.renderables = renderables;
    }

    @Override
    public Collection<RenderableBean> getRenderables() {
        return renderables;
    }

    @Override
    public int getInterpolationMethod() {
        return interpolationMethod;
    }

    @Override
    public void setInterpolationMethod(int interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @Override
    public Integer getExplicitInternalFormat() {
        return internalFormat;
    }

    @Override
    public void setExplicitInternalFormat(Integer internalFormat) {
        this.internalFormat = internalFormat;
    }

    @Override
    public Integer getExplicitVoxelComponentOrder() {
        return voxelComponentOrder;
    }

    @Override
    public void setExplicitVoxelComponentOrder(Integer voxelComponentOrder) {
        this.voxelComponentOrder = voxelComponentOrder;
    }
}

