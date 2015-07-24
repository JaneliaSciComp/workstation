package org.janelia.it.workstation.gui.geometric_search.gl.volume;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.gl.VolumeActor;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 7/8/2015.
 */
public abstract class SparseVolumeBaseActor extends GL4SimpleActor implements VolumeDataAcceptor
{
    private final Logger logger = LoggerFactory.getLogger(SparseVolumeBaseActor.class);

    File volumeFile;
    int volumeChannel;
    float volumeCutoff;
    boolean fileLoaded=false;
    boolean fileLoadError=false;
    TextureDataI textureData;
    List<VolumeActor.ChannelStatus> channelStatusList = new ArrayList<>();
    int width;
    int height;
    int depth;

    public IntBuffer vertexArrayId=IntBuffer.allocate(1);
    public IntBuffer vertexBufferId=IntBuffer.allocate(1);
    public Vector4 color=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);
    public Matrix4 vertexRotation=null;

    public class viGroup {
        
        public viGroup() {}
        
        public viGroup(float x, float y, float z, float w) {
            this.x=x;
            this.y=y;
            this.z=z;
            this.w=w;
        }
        
        public float x;
        public float y;
        public float z;
        public float w; // intensity
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }

    public List<viGroup> viList=new ArrayList<>();

    public void setColor(Vector4 color) {
        this.color=color;
    }

    public Vector4 getColor() {
        return color;
    }

    public SparseVolumeBaseActor(File volumeFile, int volumeChannel, float volumeCutoff) {
        this.volumeFile=volumeFile;
        this.volumeChannel=volumeChannel;
        this.volumeCutoff=volumeCutoff;
    }

    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
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
        
        if (textureData==null) return;

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
        logger.info("Texture data - InternalFormat="+ TextureMediator.getConstantName(textureData.getExplicitInternalFormat()));
        logger.info("Texture data - VoxelComponentOrder="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentOrder()));
        logger.info("Texture data - ComponentType="+TextureMediator.getConstantName(textureData.getExplicitVoxelComponentType()));

        int channelSizeInBytes=textureData.getPixelByteCount() * textureData.getSx() * textureData.getSy() * textureData.getSz();

        logger.info("Using texture channel byte count="+channelSizeInBytes);

        byte[] channelBuffer = new byte[channelSizeInBytes];
        VolumeDataChunk[] volumeChunks = textureData.getTextureData().getVolumeChunks();
        int chunkIndex=0;
        int chunkOffset=0;

        for (int channelIndex=0;channelIndex<textureData.getChannelCount();channelIndex++) {
            logger.info("Populating texture for channel="+channelIndex);

            for (int i=0;i<channelSizeInBytes;i++) channelBuffer[i]=0;

            int channelOffset=0;

            while(channelOffset<channelSizeInBytes) {
                if (chunkIndex==volumeChunks.length) {
                    logger.error("Unexpectedly ran out of VolumeDataChunk indices");
                    return;
                }
                byte[] chunkData=volumeChunks[chunkIndex].getData();
                logger.info("Retrieved "+chunkData.length+" bytes from chunk index="+chunkIndex);
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

            logger.info("Read "+channelOffset+" bytes for channel "+channelIndex);

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
//            int dCount=0;
            float vs = getVoxelUnitSize();
            if (channelIndex==volumeChannel) {
                for (int z=0;z<depth;z++) {
                    float fz=z*vs;
                    for (int y=0;y<height;y++) {
                        float fy=y*vs;
                        for (int x=0;x<width;x++) {
                            float fx=x*vs;
                            viGroup vi=new viGroup();
                            int p=z*height*width+y*width+x;
                            int cv=channelBuffer[p] & 0x000000ff;
//                            if (dCount<1000 && channelBuffer[p] < -1) {
//                                logger.info("channelBuffer p="+p+" ="+channelBuffer[p]+" cv="+cv);
//                                dCount++;
//                            }
                            vi.z=fz;
                            vi.y=fy;
                            vi.x=fx;
                            if (cv<0 || cv>255) {
                                logger.info("VALUE ERROR cv="+cv);
                            }
                            vi.w=(float) (cv*1.0/255.0);
//                            if (vi.x>0.99f) {
//                                vi.w=1.0f;
//                            }
//                            if (dCount<1000 && vi.w > 0.5 && vi.w < 1.0) {
//                                logger.info("cv="+cv+" vi.w="+vi.w);
//                                dCount++;
//                            }
                            totalVoxels++;
                            if (vi.w > volumeCutoff) {
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
    
    public float getVoxelUnitSize() {
        if (width==0.0f) {
            return 0.0f;
        } else {
            return 1.0f/width;
        }
    }

}
