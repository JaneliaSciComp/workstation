/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.gl;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
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
public class SparseVolumePointActor extends GL4SimpleActor implements VolumeDataAcceptor
{
    private final Logger logger = LoggerFactory.getLogger(SparseVolumePointActor.class);
    
    File volumeFile;
    int volumeChannel;
    boolean fileLoaded=false;
    boolean fileLoadError=false;
    TextureDataI textureData;
    List<VolumeActor.ChannelStatus> channelStatusList = new ArrayList<>();
    int width;
    int height;
    int depth;
        
    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    Vector4 color=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);
    Matrix4 vertexRotation=null;

    private class viGroup {
        public float x;
        public float y;
        public float z;
        public float w; // intensity
    }

    List<viGroup> viList=new ArrayList<>();
    
    public void setColor(Vector4 color) {
        this.color=color;
    }
    
    public Vector4 getColor() {
        return color;
    }

    public SparseVolumePointActor(File volumeFile, int volumeChannel) {
        this.volumeFile=volumeFile;
        this.volumeChannel=volumeChannel;
    }
    
    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);


//        gl.glDisable(GL4.GL_DEPTH_TEST);
////        gl.glShadeModel(GL4.GL_SMOOTH);
////        gl.glDisable(GL4.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);

        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, viList.size() * 3 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");
        gl.glDrawArrays(GL4.GL_POINTS, 0, viList.size());
        checkGlError(gl, "d glDrawArrays error");

//        gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glDisable(GL4.GL_BLEND);
    }

    @Override
    public void init(GL4 gl) {
        
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
        
        // We want to do two passes through the volume data. On the first pass, we will
        // count how many vertices qualify as over-threshold. On the second pass, we will
        // populate the vertex array buffer.
        
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
            
            for (int i=0;i<channelSizeInBytes;i++) channelBuffer[i]=0;
            
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
                    tmpBuffer[i]=channelBuffer[i*2+1]; // just ignore lower byte
                }
                channelBuffer=tmpBuffer;
            }
            
            int totalVoxels=0;
            int selectedVoxels=0;
            int dCount=0;
            if (channelIndex==volumeChannel) {
                for (int z=0;z<depth;z++) {
                    float fz=(float) (z*1.0/depth*1.0);     
                    for (int y=0;y<height;y++) {
                        float fy=(float) (y*1.0/height*1.0);                       
                        for (int x=0;x<width;x++) {
                            float fx=(float) (x*1.0/width*1.0);
                            viGroup vi=new viGroup();
                            int p=z*height*width+y*width+x;
                            int cv=channelBuffer[p] & 0x000000ff;
                            if (dCount<1000 && channelBuffer[p] < 0) {
                                logger.info("channelBuffer p="+p+" ="+channelBuffer[p]+" cv="+cv);
                                dCount++;
                            }
                            vi.z=fz;
                            vi.y=fy;
                            vi.x=fx;
                            vi.w=(float) (cv*1.0/255.0);
                            if (dCount<1000 && vi.w < 0.0) {
                                logger.info("cv="+cv+" vi.w="+vi.w);
                                dCount++;
                            }
                            totalVoxels++;
                            if (vi.w > 0.20) {
                                viList.add(vi);
                                selectedVoxels++;
                            }
                        }
                    }
                }
                
                logger.info("totalVoxels="+totalVoxels);
                logger.info("selectedVoxels="+selectedVoxels);
            
                
                logger.info("Done loading channel index="+channelIndex);          
            }
            
        }      

        // We want to create a triangle for each face, picking from the vertices
        FloatBuffer fb=FloatBuffer.allocate(viList.size()*3*2); // 3 floats per vertex, and 3 floats for the normal data, which we need for shader compatibility
             
        // First, vertex information
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            fb.put(v*3,vg.x);
            fb.put(v*3+1,vg.y);
            fb.put(v*3+2,vg.z);
        }
        
        // Second, use first-two floats as flag for vertex shader to use last float as intensity value
        int v2offset=viList.size()*3;
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            float fc=2000000.0f;
            fb.put(v2offset+v*3,fc);
            fb.put(v2offset+v*3+1,fc);
            fb.put(v2offset+v*3+2,vg.z);
        }        

        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "glGenVertexArrays error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glGenBuffers(1, vertexBufferId);
        checkGlError(gl, "glGenBuffers error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, fb.capacity() * 4, fb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "glBufferData error");
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
