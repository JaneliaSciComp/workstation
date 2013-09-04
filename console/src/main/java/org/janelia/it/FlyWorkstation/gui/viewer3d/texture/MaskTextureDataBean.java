package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture volume. This one is to meet the special needs of masking textures, such
 * as quick fetching of bytes.
 */

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;

public class MaskTextureDataBean implements TextureDataI {
    private String filename;
    private byte[] textureData;
    private Collection<RenderableBean> renderables;
    private Integer sx;
    private Integer sy;
    private Integer sz;

    private int interpolationMethod = GL2.GL_NEAREST;

    private float[] coordCoverage = { 1.0f, 1.0f, 1.0f};

    private VolumeDataAcceptor.TextureColorSpace colorSpace;
    private Double[] volumeMicrometers;
    private Double[] voxelMicrometers;

    // These settings may be avoided for most file types.
    private String header = "Not Available";
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private int pixelByteCount = 1;
    private int channelCount = 0;
    private Integer voxelComponentFormat = TextureDataI.UNSET_VALUE;
    private Integer explicitInternalFormat = TextureDataI.UNSET_VALUE;
    private Integer explicitVoxelComponentOrder = TextureDataI.UNSET_VALUE;

    private boolean inverted = true; // Most tested masks were inverted.

    public MaskTextureDataBean() {
        super();
    }

    public MaskTextureDataBean(byte[] textureData, int sx, int sy, int sz) {
        super();
        this.textureData = textureData;
        setSx( sx );
        setSy( sy );
        setSz( sz );
    }

    public MaskTextureDataBean(byte[] textureData, Integer[] voxels) {
        this(textureData, voxels[0], voxels[1], voxels[2]);
    }

    @Override
    public boolean equals( Object other ) {
        if ( other == null  ||  (! (other instanceof MaskTextureDataBean ) ) ) {
            return false;
        }
        else {
            return ((MaskTextureDataBean) other).getFilename().equals( getFilename() );
        }
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

    public void setTextureData( byte[] textureData ) {
        this.textureData = textureData;
    }

    public byte[] getTextureBytes() {
        return textureData;
    }

    public byte[] getTextureData() {
        return textureData;
    }

    public int getSx() {
        return sx;
    }

    public int getSy() {
        return sy;
    }

    public int getSz() {
        return sz;
    }

    public void setSx(int sx) {
        this.sx = sx;
    }

    public void setSy(int sy) {
        this.sy = sy;
    }

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    @Override
    public float[] getCoordCoverage() {
        return coordCoverage;
    }

    @Override
    public void setCoordCoverage(float[] coverage) {
        this.coordCoverage = coverage;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
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
        return explicitInternalFormat;
    }

    @Override
    public void setExplicitInternalFormat(Integer explicitInternalFormat) {
        this.explicitInternalFormat = explicitInternalFormat;
    }

    @Override
    public Integer getExplicitVoxelComponentOrder() {
        return explicitVoxelComponentOrder;
    }

    @Override
    public void setExplicitVoxelComponentOrder(Integer explicitVoxelComponentOrder) {
        this.explicitVoxelComponentOrder = explicitVoxelComponentOrder;
    }
}

