package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.geometric_search.gl.volume.SparseVolumeBaseActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by murphys on 7/20/2015.
 */
public class ArrayCubeActor extends SparseVolumeBaseActor
{
    private final Logger logger = LoggerFactory.getLogger(ArrayCubeActor.class);
    
    int downsampleLevel=1;
    int maxVoxels;
    public List<viGroup> voxelList=new ArrayList<>();

    
    public ArrayCubeActor(File volumeFile, int volumeChannel, float volumeCutoff, int maxVoxels) {
        super(volumeFile, volumeChannel, volumeCutoff);
        this.maxVoxels=maxVoxels;
    }

    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);

        gl.glDisable(GL4.GL_DEPTH_TEST);

        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");

        // VERTEX
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");

        // INTENSITY
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, voxelList.size() * 3 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");

        logger.info("display() calling glDrawArrays for GL4.GL_POINTS with viList.size="+voxelList.size());
        gl.glDrawArrays(GL4.GL_POINTS, 0, voxelList.size());
        checkGlError(gl, "d glDrawArrays error");
        
    }
    
    @Override
    public int getWidth() { return super.getWidth() / downsampleLevel; }
    
    @Override
    public int getHeight() { return super.getHeight() / downsampleLevel; }
    
    @Override
    public int getDepth() { return super.getDepth() / downsampleLevel; }

    @Override
    public void init(GL4 gl) {

        super.init(gl);

        //viList.clear();       
        //Random rn = new Random();      
        //for (int i=0;i<100000;i++) {
        //    viList.add(new viGroup(rn.nextFloat(), rn.nextFloat(), rn.nextFloat(), 0.1f));
        //}
        
        downsampleLevel=1;
        logger.info("Starting viList size="+viList.size());
        while(voxelList.size()==0 || voxelList.size()>maxVoxels) {
            if (voxelList.size()==0) {
                voxelList.addAll(viList);
            } else {
                // If here, we need to downsample
                voxelList.clear();
                downsampleLevel++;
                logger.info("Downsampling to level="+downsampleLevel);
                int X_SIZE = getWidth();
                int Y_SIZE = getHeight();
                int Z_SIZE = getDepth();
                ArrayList voxelMatrix[][][] = new ArrayList[Z_SIZE][Y_SIZE][X_SIZE];
                float vStep = getVoxelUnitSize();
                for (int vi=0;vi<viList.size();vi++) {
                    viGroup vg=viList.get(vi);
                    int xIndex=(int) (vg.x / vStep);
                    int yIndex=(int) (vg.y / vStep);
                    int zIndex=(int) (vg.z / vStep);
                    if (voxelMatrix[zIndex][yIndex][xIndex]==null) {
                        voxelMatrix[zIndex][yIndex][xIndex]=new ArrayList<viGroup>();
                    }
                    voxelMatrix[zIndex][yIndex][xIndex].add(vg);
                }
                for (int z=0;z<Z_SIZE;z++) {
                    for (int y=0;y<Y_SIZE;y++) {
                        for (int x=0;x<X_SIZE;x++) {
                            List<viGroup> vList=voxelMatrix[z][y][x];
                            if (vList!=null) {
                                float ax=1000000f;
                                float ay=1000000f;
                                float az=1000000f;
                                float aw=0.0f;
                                for (int v=0;v<vList.size();v++) {
                                    viGroup vg=vList.get(v);
                                    if (vg.x<ax) {
                                        ax=vg.x;
                                    }
                                    if (vg.y<ay) {
                                        ay=vg.y;
                                    }
                                    if (vg.z<az) {
                                        az=vg.z;
                                    }
                                    if (vg.w>aw) { // MIP
                                        aw=vg.w;
                                    }
                                }
                                float ls=new Float(vList.size());
                                voxelList.add(new viGroup(ax, ay, az, aw)); 
                            }
                        }
                    }
                }
            }
        }
        logger.info("Final downsample level="+downsampleLevel+" with voxelCount="+voxelList.size());

        FloatBuffer fb=FloatBuffer.allocate(voxelList.size()*4); // 3 floats per vertex, 1 for intensity

        logger.info("init() adding "+voxelList.size() +" vertices to FloatBuffer");

        // vertex information
        for (int v=0;v<voxelList.size();v++) {
            viGroup vg=voxelList.get(v);
            fb.put(v*3,vg.x);
            fb.put(v*3+1,vg.y);
            fb.put(v*3+2,vg.z);
        }

        // intensity information
        int intensityOffset = voxelList.size() * 3;
        for (int v=0;v<voxelList.size();v++) {
            viGroup vg=voxelList.get(v);
            fb.put(intensityOffset + v,vg.w);
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
        super.dispose(gl);
    }
    
    @Override
    public float getVoxelUnitSize() {
        return super.getVoxelUnitSize() * new Float(downsampleLevel);
    }

}
