/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.gl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author murphys
 */
public class VolumeActor extends GL4SimpleActor implements VolumeDataAcceptor {
    private final Logger logger = LoggerFactory.getLogger(VolumeActor.class);
        
    File volumeFile;
    boolean fileLoaded=false;
    boolean fileLoadError=false;
    boolean drawLines=false;
    IntBuffer textureId=IntBuffer.allocate(1);
    TextureDataI textureData;
    List<ChannelStatus> channelStatusList = new ArrayList<>();
    int width;
    int height;
    int depth;
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public static class ChannelStatus {
        public boolean loaded=false;
        public boolean display=true;
        public int textureId=0;
        public int textureUnitBinding=0;
    }

    public VolumeActor(File volumeFile) {
        this.volumeFile=volumeFile;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines=drawLines;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
    }

    @Override
    public void init(GL4 gl) {
        
        logger.info("VolumeActor - init() begin");
        
        // First, load the file into main memory
        if (!fileLoaded) {
            try {
                loadVolumeFile();
            } catch (Exception ex) {
                logger.error("Could not load file "+volumeFile.getAbsolutePath());
                ex.printStackTrace();
                fileLoadError=true;
                return;
            }
            width=textureData.getSx();
            height=textureData.getSy();
            depth=textureData.getSz();
            fileLoaded=true;
        }
        
        // Next, iterate through channels and create textureIds
        if ( textureData.getTextureData().getVolumeChunks() == null ) {
            logger.info("No entries found in textureData from file="+textureData.getFilename());
            return;
        }
        
        logger.info("Texture data - x size="+textureData.getSx());
        logger.info("Texture data - y size="+textureData.getSy());
        logger.info("Texture data - z size="+textureData.getSz());
        logger.info("Texture data - c size="+textureData.getChannelCount());
        logger.info("Texture data - v size="+textureData.getPixelByteCount());
        logger.info("Texture data - InternalFormat="+TextureMediator.getConstantName(textureData.getExplicitInternalFormat()));
        logger.info("Texture data - VoxelComponentOrder="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentOrder()));
        logger.info("Texture data - ComponentType="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentType()));
        
        int channelSizeInBytes=textureData.getPixelByteCount() * textureData.getSx() * textureData.getSy() * textureData.getSz(); 
        
        logger.info("Using texture channel byte count="+channelSizeInBytes);
        
        byte[] channelBuffer = new byte[channelSizeInBytes];
        VolumeDataChunk[] volumeChunks = textureData.getTextureData().getVolumeChunks();
        int chunkIndex=0;
        int chunkOffset=0;
        
        for (int channelIndex=0;channelIndex<textureData.getChannelCount();channelIndex++) {
            logger.error("Populating texture for channel="+channelIndex);
            int channelOffset=0;
            while(channelOffset<channelSizeInBytes) {
                if (chunkIndex==volumeChunks.length) {
                    logger.error("Unexpectedly ran out of VolumeDataChunk indices");
                    return;
                }
                byte[] chunkData=volumeChunks[chunkIndex].getData();
                int dataLengthNeeded=channelSizeInBytes-channelOffset;
                int chunkDataAvailable=chunkData.length-chunkOffset;
                if (chunkDataAvailable>=dataLengthNeeded) {
                    System.arraycopy(chunkData, chunkOffset, channelBuffer, channelOffset, dataLengthNeeded);
                    channelOffset+=dataLengthNeeded;
                    chunkOffset+=dataLengthNeeded;
                    if (chunkOffset==chunkData.length) {
                        chunkIndex++;
                    }
                } else {
                    System.arraycopy(chunkData, chunkOffset, channelBuffer, channelOffset, chunkDataAvailable);
                    channelOffset+=chunkDataAvailable;
                    chunkOffset+=chunkDataAvailable;
                    chunkIndex++;
                }
            }
            
            if (textureData.getPixelByteCount()==2) {
                int voxelCount=width*height*depth;
                byte[] tmpBuffer = new byte[voxelCount];
                for (int i=0;i<voxelCount;i++) {
                    int v=256*channelBuffer[i*2] + channelBuffer[i*2+1];
                    int bv = v/256;
                    byte b = (byte) (bv & 0x000000ff);
                    tmpBuffer[i]=b;
                }
                channelBuffer=tmpBuffer;
            }
             
            ByteBuffer data = ByteBuffer.wrap( channelBuffer );
            data.rewind();
            
            ChannelStatus cs=new ChannelStatus();
        
            IntBuffer textureIdBuffer=IntBuffer.allocate(1);
            gl.glGenTextures(1, textureIdBuffer);
            checkGlError(gl, "VolumeActor init() glGenTextures() error");
            cs.textureId=textureIdBuffer.get(0);
            

            gl.glBindTexture(GL4.GL_TEXTURE_3D, cs.textureId);
            checkGlError(gl, "VolumeActor init() glBindTexture() error");

            gl.glTexStorage3D(GL4.GL_TEXTURE_3D, 
                    1, 
                    GL4.GL_R8, 
                    width, 
                    height, 
                    depth);
            checkGlError(gl, "VolumeActor  glTexStorage3D() error");
            
            logger.info("Done with glTexStorage3D()");
        
            gl.glTexSubImage3D(GL4.GL_TEXTURE_3D, 
                0,
                0,
                0,
                0,
                width,
                height,
                depth,
                GL4.GL_RED,
                GL4.GL_BYTE,
                data);
            checkGlError(gl, "VolumeActor glTexSubImage3D() error");
            
            logger.info("Done loading channel index="+channelIndex);
        }

    }

    @Override
    public void dispose(GL4 gl) {
    }

    private void loadVolumeFile() throws Exception {
        logger.info("loadVolumeFile() begin");
        FileResolver resolver = new TrivialFileResolver();
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        volumeLoader.loadVolume(volumeFile.getAbsolutePath());
        volumeLoader.populateVolumeAcceptor(this);
        logger.info("loadVolumeFile() end");
    }

    @Override
    public void setPrimaryTextureData(TextureDataI textureData) {
        this.textureData=textureData;
    }

    @Override
    public void addTextureData(TextureDataI textureData) {
        // do nothing
    }


}

/////////////////////////////////////////////////////////////////////////////////

//   public boolean uploadTexture( GL2 gl ) {
//        boolean rtnVal = true;
//        if (reportError( "upon entry to uploadTexture", gl, textureName )) {
//            return false;
//        }
//        if ( ! isInitialized ) {
//            logger.error("Attempted to upload texture before mediator was initialized.");
//            throw new RuntimeException("Failed to upload texture");
//        }
//
//
//        if ( textureData.getTextureData().getVolumeChunks() != null ) {
//
//            logger.debug(
//                    "[" +
//                            textureData.getFilename() +
//                            "]: Coords are " + textureData.getSx() + " * " + textureData.getSy() + " * " + textureData.getSz()
//            );
//            int maxCoord = getMaxTexCoord(gl);
//            if ( textureData.getSx() > maxCoord  || textureData.getSy() > maxCoord || textureData.getSz() > maxCoord ) {
//                logger.warn(
//                        "Exceeding max coord in one or more size of texture data {}.  Results unpredictable.",
//                        textureData.getFilename()
//                );
//            }
//
//            gl.glActiveTexture( textureSymbolicId );
//            if (reportError( "glActiveTexture", gl, textureName )) {
//                return false;
//            }
//
//            gl.glEnable( GL2.GL_TEXTURE_3D );
//            if (reportError( "glEnable", gl, textureName )) {
//                return false;
//            }
//
//            gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
//            if (reportError( "glBindTexture", gl, textureName )) {
//                return false;
//            }
//
//            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
//            if (reportError( "glTexEnv MODE-REPLACE", gl, textureName )) {
//                return false;
//            }
//
//            try {
//                gl.glTexImage3D(
//                        GL2.GL_TEXTURE_3D,
//                        0, // mipmap level
//                        getInternalFormat(), // as stored INTO graphics hardware, w/ srgb info (GLint internal format)
//                        textureData.getSx(), // width
//                        textureData.getSy(), // height
//                        textureData.getSz(), // depth
//                        0, // border
//                        getVoxelComponentOrder(), // voxel component order (GLenum format)
//                        getVoxelComponentType(), // voxel component type=packed RGBA values(GLenum type)
//                        null
//                );
//                if (reportError("Tex-image-allocate", gl, textureName)) {
//                    return false;
//                }
//                
//                if ( 1==1 || logger.isDebugEnabled() ) {
//                    dumpGlTexImageCall(
//                        GL2.GL_TEXTURE_3D,
//                        0, // mipmap level
//                        getInternalFormat(), // as stored INTO graphics hardware, w/ srgb info (GLint internal format)
//                        textureData.getSx(), // width
//                        textureData.getSy(), // height
//                        textureData.getSz(), // depth
//                        0, // border
//                        getVoxelComponentOrder(), // voxel component order (GLenum format)
//                        getVoxelComponentType() // voxel component type=packed RGBA values(GLenum type)
//                    );
//                }
//
//                int expectedRemaining = textureData.getSx() * textureData.getSy() * textureData.getSz()
//                        * textureData.getPixelByteCount() * textureData.getChannelCount();
//                if ( expectedRemaining != textureData.getTextureData().length() ) {
//                    logger.warn( "Invalid remainder vs texture data dimensions.  Sx=" + textureData.getSx() +
//                            " Sy=" + textureData.getSy() + " Sz=" + textureData.getSz() +
//                            " storageFmtReq=" + getStorageFormatMultiplier() +
//                            " pixelByteCount=" + textureData.getPixelByteCount() +
//                            ";  total remaining is " +
//                            textureData.getTextureData().length() + " " + textureData.getFilename() +
//                            ";  expected remaining is " + expectedRemaining
//                    );
//                }
//
//                for ( VolumeDataChunk volumeDataChunk: textureData.getTextureData().getVolumeChunks() ) {
//                    ByteBuffer data = ByteBuffer.wrap( volumeDataChunk.getData() );
//                    data.rewind();
//
//                    logger.debug("Sub-image: {}, {}, " + volumeDataChunk.getStartZ(), volumeDataChunk.getStartX(), volumeDataChunk.getStartY() );
//                    gl.glTexSubImage3D(
//                            GL2.GL_TEXTURE_3D,
//                            0, // mipmap level
//                            volumeDataChunk.getStartX(),
//                            volumeDataChunk.getStartY(),
//                            volumeDataChunk.getStartZ(),
//                            volumeDataChunk.getWidth(), // width
//                            volumeDataChunk.getHeight(), // height
//                            volumeDataChunk.getDepth(), // depth
//                            getVoxelComponentOrder(), // voxel component order (GLenum format)
//                            getVoxelComponentType(), // voxel component type=packed RGBA values(GLenum type)
//                            data
//                    );
//                    if (reportError("Tex-sub-image", gl, textureName)) {
//                        return false;
//                    }
//
//                }
