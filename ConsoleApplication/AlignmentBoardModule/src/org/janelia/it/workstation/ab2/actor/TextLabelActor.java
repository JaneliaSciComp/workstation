package org.janelia.it.workstation.ab2.actor;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2TextLabelClickEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer3D;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextLabelActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(TextLabelActor.class);

    Vector3 v0 = new Vector3(0f, 0f, 0f);
    Vector3 v1 = new Vector3(0f, 0f, 0f);
    Vector3 centerPosition;

    String text;
    Vector4 textColor;
    Vector4 backgroundColor;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

    IntBuffer imageTextureId=IntBuffer.allocate(1);
    BufferedImage bufferedImage;

    AB2Renderer2D renderer2d;

    boolean isSelectable=false;

    static BufferedImage textResourceImage;

    int labelImageWidth;
    int labelImageHeight;
    boolean recomputeVertices=false;
    boolean recomputeAll=false;

    int glWidth=500;  // sane initialization value
    int glHeight=500; // sane initialization value

    Orientation orientation;

    public enum Orientation {
        NORMAL, VERTICAL_UP, VERTICAL_DOWN
    }

    // Load the resource image once
    static {
        try {
            textResourceImage=GLAbstractActor.getImageByFilename("UbuntuFont.png");
        } catch (Exception ex) {
            ex.printStackTrace();
            textResourceImage=null;
        }
    }

    // These values are all hand-tuned from the UbuntuFont.png file
    static final int UBUNTU_FONT_LEADING_OFFSET=4;
    static final int UBUNTU_FONT_BOTTOM_OFFSET=3;
    static final int UBUNTU_FONT_UNIT_WIDTH=9;
    static final int UBUNTU_FONT_UNIT_HEIGHT=16;
    static final int UBUNTU_FONT_THRESHOLD=190;
    static final int UBUNTU_LOW_VAL=118;
    static final int UBUNTU_HIGH_VAL=219;

    static final public String UBUNTU_FONT_STRING="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789)!@#$%^&*(-_+=[]{};:\'\""+
            ","+".<>"+"/?"+"\\"+"|"+"`~";

    public TextLabelActor(AB2Renderer2D renderer,
                          int actorId,
                          String text,
                          Vector3 centerPosition,
                          Vector4 textColor,
                          Vector4 backgroundColor,
                          Orientation orientation) {
        super(renderer, actorId);
        this.renderer2d=renderer;
        this.actorId=actorId;
        this.centerPosition=centerPosition;
        this.text=text;
        this.textColor=textColor;
        this.backgroundColor=backgroundColor;
        this.orientation=orientation;
    }

    @Override
    public boolean isSelectable() {
        return isSelectable;
    }

    public void setSelectable(boolean isSelectable) {
        this.isSelectable=isSelectable;
    }

    @Override
    protected void glWindowResize(int width, int height) {
        glWidth=width;
        glHeight=height;
        recomputeVertices=true;
    }

    public Vector4 getTextColor() { return textColor; }

    public Vector4 getBackgroundColor() { return backgroundColor; }

    public void setTextColor(Vector4 textColor) { this.textColor=textColor; }

    public void setBackgroundColor(Vector4 backgroundColor) { this.backgroundColor=backgroundColor; }

    public void setCenterPosition(Vector3 position) {
        this.centerPosition = position;
        recomputeVertices=true;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        recomputeVertices=true;
    }

    public void setText(String text) {
        this.text=text;
        recomputeAll=true;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    protected void computeVertices(GL4 gl, boolean refreshOnly) {
        float imageNormalHeight=(float)((labelImageHeight*1.0)/glHeight);
        float imageAspectRatio=(float)((labelImageWidth*1.0)/(labelImageHeight*1.0));
        float imageNormalWidth=imageAspectRatio*imageNormalHeight;

        if (orientation.equals(Orientation.NORMAL)) {
            v0.set(0, centerPosition.get(0) - imageNormalWidth/2.0f);
            v0.set(1, centerPosition.get(1) - imageNormalHeight/2.0f);
            v1.set(0, v0.get(0)+imageNormalWidth);
            v1.set(1, v0.get(1)+imageNormalHeight);
        } else if (orientation.equals(Orientation.VERTICAL_UP)) {
            v0.set(0, centerPosition.get(0) + imageNormalHeight/2.0f);
            v0.set(1, centerPosition.get(1) - imageNormalWidth/2.0f);
            v1.set(0, v0.get(0)-imageNormalHeight);
            v1.set(1, v0.get(1)+imageNormalWidth);
        } else if (orientation.equals(Orientation.VERTICAL_DOWN)) {
            v0.set(0, centerPosition.get(0)-imageNormalHeight/2.0f);
            v0.set(1, centerPosition.get(1)+imageNormalWidth/2.0f);
            v1.set(0, v0.get(0)+imageNormalHeight);
            v1.set(1, v0.get(1)-imageNormalWidth);
        }

        //logger.info("v0="+v0.get(0)+" "+v0.get(1)+" , v1="+v1.get(0)+" "+v1.get(1));

        //v0.set(1, v0.get(1)-0.01f);
        //v1.set(1, v1.get(1)-0.01f);

        // This combines positional vertices interleaved with 2D texture coordinates
        float[] vertexData = {

                v0.get(0), v0.get(1), v0.get(2),    0f, 0f, 0f, // lower left
                v1.get(0), v0.get(1), v0.get(2),    1f, 0f, 0f, // lower right
                v0.get(0), v1.get(1), v0.get(2),    0f, 1f, 0f, // upper left

                v1.get(0), v0.get(1), v1.get(2),    1f, 0f, 0f, // lower right
                v1.get(0), v1.get(1), v1.get(2),    1f, 1f, 0f, // upper right
                v0.get(0), v1.get(1), v1.get(2),    0f, 1f, 0f  // upper left
        };

        vertexFb=createGLFloatBuffer(vertexData);

        if (!refreshOnly) {
            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        } else {
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "Refresh - glBindBuffer");
            gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, vertexFb.capacity() * 4, vertexFb);
            checkGlError(gl, "Refresh - glBufferSubData");
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            checkGlError(gl, "Refresh - glBindBuffer 0");
        }
        recomputeVertices=false;
    }

    private void updateTextImage(GL4 gl) {
        byte[] labelPixels=createTextImage();

        if (imageTextureId.get(0)!=0) {
            gl.glDeleteTextures(1, imageTextureId);
        }

        // Create texture
        gl.glGenTextures(1, imageTextureId);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));

        ByteBuffer byteBuffer=ByteBuffer.allocate(labelPixels.length);
        for (int i=0;i<labelPixels.length;i++) {
            byteBuffer.put(i, labelPixels[i]);
        }

        byteBuffer.rewind();
        gl.glTexImage2D(GL4.GL_TEXTURE_2D,0, GL4.GL_RGBA, labelImageWidth, labelImageHeight,0,
                GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, byteBuffer);
        checkGlError(gl, "Uploading texture");
        gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST );
        gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST );
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Text2DShader) {
            updateTextImage(gl);
            computeVertices(gl, false);
        }
    }

    protected byte[] createTextImage() {
        // Step 1: get contents
        int labelLength=text.length();
        int characterPositions[] = new int[labelLength];
        for (int i=0;i<labelLength;i++) {
            char c=text.charAt(i);
            int j=0;
            for (;j<UBUNTU_FONT_STRING.length();j++) {
                if (c==UBUNTU_FONT_STRING.charAt(j)) {
                    break;
                }
            }
            if (j==UBUNTU_FONT_STRING.length()) {
                characterPositions[i]=-1; // unknown - space by convention
            } else {
                characterPositions[i]=j;
            }
        }
        // Step 2: allocate new image
        int wPad=UBUNTU_FONT_UNIT_WIDTH/2;
        int hPad=UBUNTU_FONT_UNIT_HEIGHT/4;
        int w=labelLength*UBUNTU_FONT_UNIT_WIDTH + wPad*2;
        int h=UBUNTU_FONT_UNIT_HEIGHT + hPad*2;
        byte labelPixels[]=new byte[w*h*4];
        int sourceHeight=textResourceImage.getHeight();
        int sourceWidth=textResourceImage.getWidth();
        int sourceHeightOffset=sourceHeight-(UBUNTU_FONT_UNIT_HEIGHT+UBUNTU_FONT_BOTTOM_OFFSET);
        for (int i=0;i<labelLength;i++) {
            int cp=characterPositions[i];
            if (cp>-1) {
                for (int y = 0; y < UBUNTU_FONT_UNIT_HEIGHT; y++) {
                    for (int x = 0; x < UBUNTU_FONT_UNIT_WIDTH; x++) {
                        int sX = UBUNTU_FONT_LEADING_OFFSET + cp * UBUNTU_FONT_UNIT_WIDTH + x;
                        int sY = sourceHeight - (sourceHeightOffset + y + 1);
                        int tX = wPad + UBUNTU_FONT_UNIT_WIDTH * i + x;
                        int tY = hPad + y;
                        if (sX<sourceWidth && sY<sourceHeight) {
                            int resourceRGB = textResourceImage.getRGB(sX, sY);
                            byte a = (byte) (resourceRGB >>> 24);
                            byte r = (byte) (resourceRGB >>> 16); // ONLY using r
                            byte g = (byte) (resourceRGB >>> 8);
                            byte b = (byte) (resourceRGB);
                            int R=r;
                            if (R<0) { R=256+R; }
                            double scaledValue=((R-UBUNTU_LOW_VAL)*1.0)/((UBUNTU_HIGH_VAL-UBUNTU_LOW_VAL)*1.0);
                            int iVal=(int)(255.0*scaledValue);
                            byte tVal=(byte)iVal;
                            int byteOffset=(tY*w+tX)*4;
                            labelPixels[byteOffset]=tVal;
                        }
                    }
                }
            }
        }
        this.labelImageHeight=h;
        this.labelImageWidth=w;
        return labelPixels;
    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        //logger.info("display() called");

        if (recomputeAll) {
            updateTextImage(gl);
            computeVertices(gl, true);
            recomputeAll=false;
        } else if (recomputeVertices) {
            computeVertices(gl, true);
            recomputeVertices=false;
        }
        if (shader instanceof AB2Text2DShader) {
            AB2Text2DShader text2DShader=(AB2Text2DShader)shader;
            text2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            text2DShader.setForegroundColor(gl, getTextColor());
            text2DShader.setBackgroundColor(gl, getBackgroundColor());

            gl.glActiveTexture(GL4.GL_TEXTURE0);
            checkGlError(gl, "d1 glActiveTexture");
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));
            checkGlError(gl, "d2 glBindTexture()");
        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, renderer2d.getVp2d());
            pickShader.setPickId(gl, getActorId());
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

        if (shader instanceof AB2Text2DShader) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
            checkGlError(gl, "d11 glBindTexture()");
        }
    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Text2DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteTextures(1, imageTextureId);
        }
        super.dispose(gl, shader);
    }

}
