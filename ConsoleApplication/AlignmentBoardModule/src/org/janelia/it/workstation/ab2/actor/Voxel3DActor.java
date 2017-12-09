package org.janelia.it.workstation.ab2.actor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Voxel3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Voxel3DActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(Voxel3DActor.class);

    List<Vector3> voxels;
    List<Vector4> colors;
    int dimX;
    int dimY;
    int dimZ;
    int[] xyBounds=new int[] { 0, 0, 10000, 10000 }; // effectively no limits

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

    IntBuffer colorBufferId=IntBuffer.allocate(1);

    ShortBuffer vertexFb;
    ByteBuffer colorFb;

    public void setXYBounds(int x0, int y0, int x1, int y1) {
        xyBounds[0]=x0;
        xyBounds[1]=y0;
        xyBounds[2]=x1;
        xyBounds[3]=y1;
    }

    public Voxel3DActor(AB23DRenderer renderer, int actorId, List<Vector3> voxels, List<Vector4> colors,
                        int dimX, int dimY, int dimZ) {
        super(renderer);
        this.actorId=actorId;
        this.voxels=voxels;
        this.colors=colors;
        this.dimX=dimX;
        this.dimY=dimY;
        this.dimZ=dimZ;
    }

    public Voxel3DActor(AB23DRenderer renderer, int actorId, float threshold, byte[] dataXYZRGBA) {
        super(renderer);
        this.actorId=actorId;
        voxels=new ArrayList<>();
        colors=new ArrayList<>();
        byte t=(byte)(threshold*255f);
        byte[] xyzData=new byte[12];
        for (int i=0;i<12;i++) {
            xyzData[i]=dataXYZRGBA[i];
        }
        ByteBuffer xyzBB=ByteBuffer.wrap(xyzData);
        IntBuffer xyzIB=xyzBB.asIntBuffer();
        dimX=xyzIB.get(0);
        dimY=xyzIB.get(1);
        dimZ=xyzIB.get(2);
        float dimXf=(float)dimX;
        float dimYf=(float)dimY;
        float dimZf=(float)dimZ;
        int p=12;
        int xI=dimX;
        int yI=dimX*dimY;
        int xd=0;
        int yd=0;
        int zd=0;
        int q=0;
        while (p<dataXYZRGBA.length) {
            q=(p-12)/4;
            if (q%xI==0) {
                xd=0;
                yd++;
            }
            if (q%yI==0) {
                yd=0;
                zd++;
            }
            int r=dataXYZRGBA[p++];
            int g=dataXYZRGBA[p++];
            int b=dataXYZRGBA[p++];
            int a=dataXYZRGBA[p++];
            if (r<0) r+=256;
            if (g<0) g+=256;
            if (b<0) b+=256;
            if (a<0) a+=256;
            if (r>t || g>t || b>t) {
                Vector3 v=new Vector3((xd*1f)/dimXf, (yd*1f)/dimYf, (zd*1f)/dimZf);
                voxels.add(v);
                colors.add(new Vector4( (r*1f)/255f, (g*1f)/255f, (b*1f)/255f, (a*1f)/255f));
            }
            xd++;
        }
        logger.info("Found "+voxels.size()+" qualifying voxels");
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        logger.info("init() start");

        if (shader instanceof AB2Voxel3DShader) {

            System.out.println("Voxel3DActor init() start");
            System.out.flush();

            int maxDim=getMaxDim();

            if (voxels.size()!=colors.size()) {
                logger.error("voxel and color array must be same size");
                return;
            }

            AB2Voxel3DShader voxel3DShader = (AB2Voxel3DShader) shader;

            // 10 bytes : RGBA @ 8-bit = 4, + XYZ @ 16-bit = 6
            short[] xyzData = new short[voxels.size() * 3];
            byte[] colorData = new byte[colors.size() * 4];

            float dimXf=(float)dimX;
            float dimYf=(float)dimY;
            float dimZf=(float)dimZ;

            int xOffset=(maxDim-dimX)/2;
            int yOffset=(maxDim-dimY)/2;
            int zOffset=(maxDim-dimZ)/2;

            float roundErrorCompensation=0.05f;

            for (int i = 0; i < voxels.size(); i++) {
                Vector3 v = voxels.get(i);
                Vector4 c = colors.get(i);

                float xf=v.getX()*dimXf+roundErrorCompensation;
                float yf=v.getY()*dimYf+roundErrorCompensation;
                float zf=v.getZ()*dimZf+roundErrorCompensation;

                int xd=(int)xf;
                int yd=(int)yf;
                int zd=(int)zf;

                //logger.info(""+xf+" "+xd+" "+yf+" "+yd+" "+zf+" "+zd);

                xyzData[i * 3]     = (short) (xd+xOffset);
                xyzData[i * 3 + 1] = (short) (yd+yOffset);
                xyzData[i * 3 + 2] = (short) (zd+zOffset);

                colorData[i * 4]     = (byte) (c.get(0) * 255);
                colorData[i * 4 + 1] = (byte) (c.get(1) * 255);
                colorData[i * 4 + 2] = (byte) (c.get(2) * 255);
                colorData[i * 4 + 3] = (byte) (c.get(3) * 255);
            }

            vertexFb = GLAbstractActor.createGLShortBuffer(xyzData);
            colorFb = GLAbstractActor.createGLByteBuffer(colorData);

            // Vertex VBO

            gl.glGenVertexArrays(1, vertexArrayId);
            checkGlError(gl, "i1 glGenVertexArrays error");

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, vertexBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 2, vertexFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            // Color VBO

            gl.glGenBuffers(1, colorBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, colorFb.capacity(), colorFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            System.out.println("Voxel3DActor init() end");
            System.out.flush();

            logger.info("init() done");

        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        //logger.info("display() start");

        //System.out.println("Voxel3DActor display() start");
        //System.out.flush();

        if (shader instanceof AB2Voxel3DShader) {

            if (voxels.size()!=colors.size()) {
                logger.error("voxels and colors arrays must be same size");
                return;
            }

            AB2Voxel3DShader voxel3DShader = (AB2Voxel3DShader) shader;
            if (this.postProjectionMatrix!=null) {
                voxel3DShader.setMVP(gl, getModelMatrix().multiply(renderer.getVp3d()).multiply(postProjectionMatrix));
            } else {
                voxel3DShader.setMVP(gl, getModelMatrix().multiply(renderer.getVp3d()));
            }
            voxel3DShader.setDimXYZ(gl, dimX, dimY, dimZ);
            voxel3DShader.setGLBoundsXY(gl, xyBounds[0], xyBounds[1], xyBounds[2], xyBounds[3]);
            int dimMax=getMaxDim();
            float voxelUnitSize=1f/(1f*dimMax);
            voxel3DShader.setVoxelSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));

            //System.out.println("Voxel3DActor Check1");
            //System.out.flush();

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "d1 glBindVertexArray() error");

            //System.out.println("Voxel3DActor Check2");
            //System.out.flush();

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "d2 glBindBuffer error");

            //System.out.println("Voxel3DActor Check3");
            //System.out.flush();

            gl.glVertexAttribPointer(0, 3, GL4.GL_SHORT, false, 0, 0);
            checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

            //System.out.println("Voxel3DActor Check4");
            //System.out.flush();

            gl.glEnableVertexAttribArray(0);
            checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

            //System.out.println("Voxel3DActor Check5");
            //System.out.flush();

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorBufferId.get(0));
            checkGlError(gl, "d2 glBindBuffer error");

            //System.out.println("Voxel3DActor Check6");
            //System.out.flush();

            gl.glVertexAttribPointer(1, 4, GL4.GL_UNSIGNED_BYTE, true, 0, 0);
            checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

            //System.out.println("Voxel3DActor Check7");
            //System.out.flush();

            gl.glEnableVertexAttribArray(1);
            checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

            int count=vertexFb.capacity()/3;
            //System.out.println("Voxel3DActor Check8 - count="+count);
            //System.out.flush();

            gl.glDrawArrays(GL4.GL_POINTS, 0, count);
            checkGlError(gl, "d7 glDrawArrays() error");

            //System.out.println("Voxel3DActor Check9");
            //System.out.flush();

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

        //System.out.println("Voxel3DActor display() end");
        //System.out.flush();

        //logger.info("display() end");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Voxel3DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteBuffers(1, colorBufferId);
        }
    }

    public int getMaxDim() {
        int maxDim=dimX;
        if (dimY>maxDim) {
            maxDim=dimY;
        }
        if (dimZ>maxDim) {
            maxDim=dimZ;
        }
        return maxDim;
    }

}
