package org.janelia.it.workstation.ab2.actor;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Image3DActor extends Camera3DFollowBoxActor {

    private final Logger logger = LoggerFactory.getLogger(Image3DActor.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

    IntBuffer imageTextureId=IntBuffer.allocate(1);
    BufferedImage bufferedImage;

    Matrix4 textureModelMatrix;

    int dimX;
    int dimY;
    int dimZ;
    byte data3d[];

    public Image3DActor(AB23DRenderer renderer, int actorId, Vector3 v0, Vector3 v1, int dimX, int dimY, int dimZ, byte[] data3d) {
        super(renderer, actorId, v0, v1);
        this.dimX=dimX;
        this.dimY=dimY;
        this.dimZ=dimZ;
        this.data3d=data3d;
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {
//        super.init(gl, shader);
        if (shader instanceof AB2ActorShader) {

            // We need to provide a sequence of quads, which we will create using two triangles each.
            // These quads will populate the volume bounded by v0 and v1.

            float[] vd = new float[dimZ*6*6];

            float zvStep=(v1.getZ()-v0.getZ())/(1f*dimZ);
            float zvStart=v0.getZ()+zvStep/2f;
            float zStep=1f/(1f*dimZ);
            float zStart=zStep/2f;

            for (int zi=0;zi<dimZ;zi++) {
                float zv=zvStart+(zi*1f)*zvStep;
                float z=zStart+(zi*1f)*zStep;
                int o=zi*36;
                vd[o]   =v0.getX(); vd[o+1] =v0.getY(); vd[o+2] =zv; vd[o+3] =0f; vd[o+4] =0f; vd[o+5] =z; // lower left
                vd[o+6] =v1.getX(); vd[o+7] =v0.getY(); vd[o+8] =zv; vd[o+9] =1f; vd[o+10]=0f; vd[o+11]=z; // lower right
                vd[o+12]=v0.getX(); vd[o+13]=v1.getY(); vd[o+14]=zv; vd[o+15]=0f; vd[o+16]=1f; vd[o+17]=z; // upper left

                vd[o+18]=v1.getX(); vd[o+19]=v0.getY(); vd[o+20]=zv; vd[o+21]=1f; vd[o+22]=0f; vd[o+23]=z; // lower right
                vd[o+24]=v1.getX(); vd[o+25]=v1.getY(); vd[o+26]=zv; vd[o+27]=1f; vd[o+28]=1f; vd[o+29]=z; // upper right
                vd[o+30]=v0.getX(); vd[o+31]=v1.getY(); vd[o+32]=zv; vd[o+33]=0f; vd[o+34]=1f; vd[o+35]=z; // upper left
            }

            vertexFb=createGLFloatBuffer(vd);

            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            // Create texture

            ByteBuffer byteBuffer=ByteBuffer.allocate(data3d.length);
            for (int i=0;i<data3d.length;i++) {
                byteBuffer.put(i, data3d[i]);
            }

//            glBindTexture( GL_TEXTURE_3D, mu3DTex );
//            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE); -- deprecated, texture mixing intended to be handled in shaders
//            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
//            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
//            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
//            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            gl.glActiveTexture(GL4.GL_TEXTURE0);
            gl.glGenTextures(1, imageTextureId);
            gl.glBindTexture(GL4.GL_TEXTURE_3D, imageTextureId.get(0));
            gl.glTexImage3D(GL4.GL_TEXTURE_3D,0, GL4.GL_RGBA, dimX, dimY, dimZ,0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, byteBuffer);
            checkGlError(gl, "Uploading texture");
            gl.glTexParameteri(GL4.GL_TEXTURE_3D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_BORDER);
            gl.glTexParameteri(GL4.GL_TEXTURE_3D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_BORDER);
            gl.glTexParameteri(GL4.GL_TEXTURE_3D, GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_BORDER);
            gl.glTexParameteri( GL4.GL_TEXTURE_3D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST );
            gl.glTexParameteri( GL4.GL_TEXTURE_3D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST );
            gl.glBindTexture(GL4.GL_TEXTURE_3D, 0);

        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2ActorShader) {
            AB2ActorShader actorShader=(AB2ActorShader)shader;
            actorShader.setMVP2d(gl, getModelMatrix().multiply(renderer.getVp2d()));
            actorShader.setMVP3d(gl, getModelMatrix().multiply(renderer.getVp3d()));
            //actorShader.setTextureMVP3d(gl, getTextureModelMatrix().multiply(renderer.getVp3d()));
            actorShader.setTextureMVP3d(gl, getTextureModelMatrix().multiply(renderer.getRotationAsTransform()));
            actorShader.setTwoDimensional(gl, false);
            actorShader.setTextureType(gl, AB2ActorShader.TEXTURE_TYPE_3D_RGBA);

            Vector4 actorColor=renderer.getColorIdMap().get(actorId);
            if (actorColor!=null) {
                actorShader.setColor0(gl, actorColor);
            }

            gl.glActiveTexture(GL4.GL_TEXTURE0);
            checkGlError(gl, "d1 glActiveTexture");
            gl.glBindTexture(GL4.GL_TEXTURE_3D, imageTextureId.get(0));
            checkGlError(gl, "d2 glBindTexture()");
            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "d3 glBindVertexArray()");
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "d4 glBindBuffer()");
            gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
            checkGlError(gl, "d5 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(0);
            checkGlError(gl, "d6 glEnableVertexAttribArray()");
            gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
            checkGlError(gl, "d7 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(1);
            checkGlError(gl, "d8 glEnableVertexAttribArray()");
            gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/3);
            checkGlError(gl, "d9 glDrawArrays()");
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            checkGlError(gl, "d10 glBindBuffer()");
            gl.glBindTexture(GL4.GL_TEXTURE_3D, 0);
            checkGlError(gl, "d11 glBindTexture()");
        }
    }

    protected Matrix4 getTextureModelMatrix() {
        //if (textureModelMatrix==null) {
            Matrix4 translationMatrix = new Matrix4();
            float scale=1.0f;
            translationMatrix.set(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    -0.5f, -0.5f, -0.5f, 1.0f);
            Matrix4 scaleMatrix = new Matrix4();
            scaleMatrix.set(
                    scale, 0.0f, 0.0f, 0.0f,
                    0.0f, scale, 0.0f, 0.0f,
                    0.0f, 0.0f, scale, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
            textureModelMatrix=translationMatrix.multiply(scaleMatrix);
        //}
        return textureModelMatrix;
    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
//        super.dispose(gl, shader);
        if (shader instanceof AB2ActorShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteTextures(1, imageTextureId);
        }
    }

}
