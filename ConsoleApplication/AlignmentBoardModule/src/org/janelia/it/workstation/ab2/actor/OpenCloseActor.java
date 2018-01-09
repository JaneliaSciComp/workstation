package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCloseActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(OpenCloseActor.class);

    Vector3 position;
    float size;
    float pointSize=1.0f; // recomputed with glWindowResize

    IntBuffer backgroundVertexArrayId=IntBuffer.allocate(1);
    IntBuffer backgroundVertexBufferId=IntBuffer.allocate(1);

    IntBuffer openVertexArrayId=IntBuffer.allocate(1);
    IntBuffer openVertexBufferId=IntBuffer.allocate(1);

    IntBuffer closedVertexArrayId=IntBuffer.allocate(1);
    IntBuffer closedVertexBufferId=IntBuffer.allocate(1);

    FloatBuffer backgroundVertexFb;
    FloatBuffer openVertexFb;
    FloatBuffer closedVertexFb;

    Vector4 backgroundColor;
    Vector4 foregroundColor;
    Vector4 hoverColor;
    Vector4 selectColor;

    AB2Renderer2D renderer2d;

    boolean isOpen=false;

    boolean isSelectable=false;

    public OpenCloseActor(AB2Renderer2D renderer, int actorId, float size, Vector3 position, Vector4 foregroundColor,
                           Vector4 backgroundColor, Vector4 hoverColor, Vector4 selectColor) {
        super(renderer, actorId);
        this.renderer2d=renderer;
        this.size=size;
        this.position=position;
        this.foregroundColor=foregroundColor;
        this.backgroundColor=backgroundColor;
        this.hoverColor=hoverColor;
        this.selectColor=selectColor;
    }

    public Vector4 getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Vector4 color) {
        this.foregroundColor=foregroundColor;
    }

    public Vector4 getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Vector4 color) {
        this.backgroundColor=backgroundColor;
    }

    public Vector4 getHoverColor() {
        return hoverColor;
    }

    public void setHoverColor(Vector4 hoverColor) {
        this.hoverColor = hoverColor;
    }

    public Vector4 getSelectColor() {
        return selectColor;
    }

    public void setSelectColor(Vector4 selectColor) {
        this.selectColor = selectColor;
    }

    @Override
    public boolean isSelectable() {
        return isSelectable;
    }

    public void setSelectable(boolean isSelectable) {
        this.isSelectable=isSelectable;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen=isOpen;
    }

    @Override
    protected void glWindowResize(int width, int height) {
        pointSize=size*width;
        needsResize=true;
    }


    public void setSize(float size) {
        this.size=size;
        needsResize=true;
    }

    public void updatePosition(Vector3 position) {
        this.position=position;
        needsResize=true;
    }

    // In this case, we just need a single position, since we will try using a point
    // with a large size for the background.

    private float[] computeBackgroundVertexData() {

        float[] vertexData = {
                position.get(0), position.get(1), position.get(2)
        };

        return vertexData;
    }

    // Here, we just want a little squre in the middle of the point circle.

    private float[] computeForegroundOpenVertexData() {

        float sl=(this.size*0.8f)/2.0f;

        float cx=position.get(0);
        float cy=position.get(1);
        float cz=position.get(2);

        float[] vertexData = {
                cx-sl, cy-sl, cz-0.01f, // lower left
                cx-sl, cy+sl, cz-0.01f, // upper left

                cx-sl, cy+sl, cz-0.01f, // upper left
                cx+sl, cy+sl, cz-0.01f, // upper right

                cx+sl, cy+sl, cz-0.01f, // upper right
                cx+sl, cy-sl, cz-0.01f, // lower right

                cx+sl, cy-sl, cz-0.01f, // lower right
                cx-sl, cy-sl, cz-0.01f, // lower left
        };

        return vertexData;
    }

    // Here, we want a simple horizontal line.

    private float[] computeForegroundClosedVertexData() {

        float sl=(this.size*0.8f)/2.0f;

        float cx=position.get(0);
        float cy=position.get(1);
        float cz=position.get(2);

        float[] vertexData = {
                cx-sl, cy, cz-0.01f,
                cx+sl, cy, cz-0.01f
        };

        return vertexData;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic2DShader) {

            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;

            float[] backgroundVertexData=computeBackgroundVertexData();
            float[] openVertexData=computeForegroundOpenVertexData();
            float[] closedVertexData=computeForegroundClosedVertexData();

            backgroundVertexFb=createGLFloatBuffer(backgroundVertexData);
            openVertexFb=createGLFloatBuffer(openVertexData);
            closedVertexFb=createGLFloatBuffer(closedVertexData);

            gl.glGenVertexArrays(1, backgroundVertexArrayId);
            gl.glBindVertexArray(backgroundVertexArrayId.get(0));
            gl.glGenBuffers(1, backgroundVertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, backgroundVertexFb.capacity() * 4, backgroundVertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            gl.glGenVertexArrays(1, openVertexArrayId);
            gl.glBindVertexArray(openVertexArrayId.get(0));
            gl.glGenBuffers(1, openVertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, openVertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, openVertexFb.capacity() * 4, openVertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            gl.glGenVertexArrays(1, closedVertexArrayId);
            gl.glBindVertexArray(closedVertexArrayId.get(0));
            gl.glGenBuffers(1, closedVertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, closedVertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, closedVertexFb.capacity() * 4, closedVertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

    }

    private void updateVertexBuffers(GL4 gl) {

        float[] backgroundVertexData=computeBackgroundVertexData();
        float[] openVertexData=computeForegroundOpenVertexData();
        float[] closedVertexData=computeForegroundClosedVertexData();

        backgroundVertexFb=createGLFloatBuffer(backgroundVertexData);
        openVertexFb=createGLFloatBuffer(openVertexData);
        closedVertexFb=createGLFloatBuffer(closedVertexData);

        gl.glBindVertexArray(backgroundVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, backgroundVertexFb.capacity() * 4, backgroundVertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        gl.glBindVertexArray(openVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, openVertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, openVertexFb.capacity() * 4, openVertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        gl.glBindVertexArray(closedVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, closedVertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, closedVertexFb.capacity() * 4, closedVertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

    }

    private void drawBackground(GL4 gl) {
        gl.glPointSize(pointSize);
        gl.glBindVertexArray(backgroundVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_POINTS, 0, backgroundVertexFb.capacity() / 3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawOpenForeground(GL4 gl) {
        gl.glBindVertexArray(openVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, openVertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_LINES, 0,openVertexFb.capacity() / 3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawClosedForeground(GL4 gl) {
        gl.glBindVertexArray(closedVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, closedVertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_LINES, 0,closedVertexFb.capacity() / 3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }


    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        if (needsResize) {
            updateVertexBuffers(gl);
            needsResize=false;
        }

        if (shader instanceof AB2Basic2DShader) {
            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;
            basic2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));

            // First draw background
            if (isSelected) {
                basic2DShader.setColor(gl, selectColor);
            } else if (isHovered) {
                basic2DShader.setColor(gl, hoverColor);
            } else {
                basic2DShader.setColor(gl, backgroundColor);
            }

            drawBackground(gl);

            // Next, draw foreground
            basic2DShader.setColor(gl, foregroundColor);
            if (isOpen) {
                drawOpenForeground(gl);
            } else {
                drawClosedForeground(gl);
            }

        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            pickShader.setPickId(gl, actorId);
            drawBackground(gl);
        }

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {
            gl.glDeleteVertexArrays(1, backgroundVertexArrayId);
            gl.glDeleteVertexArrays(1, openVertexArrayId);
            gl.glDeleteVertexArrays(1, closedVertexArrayId);

            gl.glDeleteBuffers(1, backgroundVertexBufferId);
            gl.glDeleteBuffers(1, openVertexBufferId);
            gl.glDeleteBuffers(1, closedVertexBufferId);
        }
        super.dispose(gl, shader);
    }

}
