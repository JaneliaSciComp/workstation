package org.janelia.it.workstation.ab2.actor;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Image2DActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(Image2DActor.class);

    Vector2 v0;
    Vector2 v1;
    int pickIndex=-1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

    IntBuffer imageTextureId=IntBuffer.allocate(1);
    BufferedImage bufferedImage;
    float alpha;

    public Image2DActor(int actorId, Vector2 v0, Vector2 v1, BufferedImage bufferedImage, float alpha) {
        this.actorId=actorId;
        this.v0=v0;
        this.v1=v1;
        this.bufferedImage=bufferedImage;
        this.alpha=alpha;
    }

    public int getPickIndex() { return pickIndex; }

    @Override
    public void init(GL4 gl) {
        if (this.mode == Mode.DRAW) {

            float[] vertexData = {

                    v0.get(0), v0.get(1), 0f,
                    v1.get(0), v0.get(1), 0f,
                    v0.get(0), v1.get(1), 0f,

                    v1.get(0), v0.get(1), 0f,
                    v1.get(0), v1.get(1), 0f,
                    v0.get(0), v1.get(1), 0f
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
            byte pixels[] = new byte[w*h*4];
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    int pixelInt=bufferedImage.getRGB(x,y);
                    int byteOffset=(y*w+x)*4;
                    // Convert to RGBA from ARGB
                    byte a=(byte)(pixelInt >>> 24); // ignore this byte
                    byte r=(byte)(pixelInt >>> 16);
                    byte g=(byte)(pixelInt >>> 8);
                    byte b=(byte)(pixelInt);
                    pixels[byteOffset]  =r;
                    pixels[byteOffset+1]=g;
                    pixels[byteOffset+2]=b;
                    pixels[byteOffset + 3] = (byte)(255f*alpha);
                }
            }

            // Create texture
            gl.glGenTextures(1, imageTextureId);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length);
            byteBuffer.wrap(pixels);

            gl.glTexImage2D(GL4.GL_TEXTURE_2D,0, GL4.GL_RGBA, w, h,0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, byteBuffer);
            checkGlError(gl, "Uploading texture");
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        } else if (this.mode == Mode.PICK) {
            if (pickIndex<0) {
                pickIndex = AB2Controller.getController().getNextPickIndex();
                AB2Controller.getController().setPickEvent(pickIndex, new AB2Image2DClickEvent(this));
                logger.info("Setting pickIndex="+pickIndex);
            }
        }

    }

    @Override
    public void display(GL4 gl) {
        if (this.mode==Mode.DRAW) {
            gl.glActiveTexture(0);
            checkGlError(gl, "d1 glActiveTexture(0)");
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));
            checkGlError(gl, "d2 glBindTexture()");
        }
        if (this.mode==Mode.DRAW || this.mode==Mode.PICK) {
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);
            gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/3);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        }
        if (this.mode==Mode.DRAW) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        }
    }

    @Override
    public void dispose(GL4 gl) {
        if (mode==Mode.DRAW) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteTextures(1, imageTextureId);
        }
    }

}
