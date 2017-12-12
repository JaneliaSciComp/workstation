package org.janelia.it.workstation.ab2.actor;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer3D;
import org.janelia.it.workstation.ab2.shader.AB2Image2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Image2DActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(Image2DActor.class);

    Vector2 v0;
    Vector2 v1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

    IntBuffer imageTextureId=IntBuffer.allocate(1);
    BufferedImage bufferedImage;
    float alpha;
    AB2Renderer2D renderer2d;


    public Image2DActor(AB2Renderer2D renderer, int actorId, Vector2 v0, Vector2 v1, BufferedImage bufferedImage, float alpha) {
        super(renderer);
        this.renderer2d=renderer;
        this.actorId=actorId;
        this.v0=v0;
        this.v1=v1;
        this.bufferedImage=bufferedImage;
        this.alpha=alpha;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Image2DShader) {

            AB2Image2DShader image2DShader=(AB2Image2DShader)shader;

            // This combines positional vertices interleaved with 2D texture coordinates. Note: z not used
            // but necessary for shader compatibility.
            float[] vertexData = {

                    v0.get(0), v0.get(1), 0f,    0f, 0f, 0f,  // lower left
                    v1.get(0), v0.get(1), 0f,    1f, 0f, 0f,  // lower right
                    v0.get(0), v1.get(1), 0f,    0f, 1f, 0f,  // upper left

                    v1.get(0), v0.get(1), 0f,    1f, 0f, 0f,  // lower right
                    v1.get(0), v1.get(1), 0f,    1f, 1f, 0f,  // upper right
                    v0.get(0), v1.get(1), 0f,    0f, 1f, 0f,  // upper left
            };

            vertexFb=createGLFloatBuffer(vertexData);

            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            // Produce image pixels
            int w=bufferedImage.getWidth();
            int h=bufferedImage.getHeight();
            logger.info("image w="+w+" h="+h);
            byte pixels[] = new byte[w*h*4];
            int iCount=0;
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    int pixelInt=bufferedImage.getRGB(x,h-y-1); // flip y
                    int byteOffset=(y*w+x)*4;
                    // Convert to RGBA from ARGB
                    byte a=(byte)(pixelInt >>> 24); // ignore this byte
                    byte r=(byte)(pixelInt >>> 16);
                    byte g=(byte)(pixelInt >>> 8);
                    byte b=(byte)(pixelInt);

                    int ai=(int)(255f*alpha);
                    if (ai>127) {
                        ai=ai-256;
                    }

                    pixels[byteOffset]   = r;
                    pixels[byteOffset+1] = g;
                    pixels[byteOffset+2] = b;
                    pixels[byteOffset+3] = (byte)ai;

//                    pixels[byteOffset]   =53;     // r - validated
//                    pixels[byteOffset+1] =53;     // g - validated
//                    pixels[byteOffset+2] =53;     // b - validated
//                    pixels[byteOffset+3] =53;    // a

                    iCount++;
                }
            }
            logger.info("pixel count="+iCount);

            // Create texture
            gl.glGenTextures(1, imageTextureId);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));

            //ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length);
            //byteBuffer.wrap(pixels);

            ByteBuffer byteBuffer=ByteBuffer.allocate(pixels.length);
            for (int i=0;i<pixels.length;i++) {
                byteBuffer.put(i, pixels[i]);
            }

            gl.glTexImage2D(GL4.GL_TEXTURE_2D,0, GL4.GL_RGBA, w, h,0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, byteBuffer);
            checkGlError(gl, "Uploading texture");
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST );
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST );
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        } else if (shader instanceof AB2PickShader) {
            if (pickIndex<0) {
                pickIndex = AB2Controller.getController().getNextPickIndex();
                AB2Controller.getController().setPickEvent(pickIndex, new AB2Image2DClickEvent(this));
                logger.info("Setting pickIndex="+pickIndex);
            }
        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Image2DShader) {
            AB2Image2DShader image2DShader=(AB2Image2DShader)shader;
            image2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            checkGlError(gl, "d1 glActiveTexture");
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));
            checkGlError(gl, "d2 glBindTexture()");
        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP2d(gl, renderer2d.getVp2d());
            pickShader.setPickId(gl, getPickIndex());
        }

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
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/2);
        checkGlError(gl, "d9 glDrawArrays()");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        checkGlError(gl, "d10 glBindBuffer()");

        if (shader instanceof AB2Image2DShader) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
            checkGlError(gl, "d11 glBindTexture()");
        }
    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Image2DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteTextures(1, imageTextureId);
        }
    }

}
