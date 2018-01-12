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

public class HorizontalDualSliderActor extends GLAbstractActor {

    // todo: finish converting this from ColorBox2DActor template

    private final Logger logger = LoggerFactory.getLogger(HorizontalDualSliderActor.class);

    Vector3 v0;
    Vector3 v1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

//    IntBuffer imageTextureId=IntBuffer.allocate(1);
//    BufferedImage bufferedImage;
//    float alpha;

    Vector4 color;
    Vector4 hoverColor;
    Vector4 selectColor;

    AB2Renderer2D renderer2d;

    boolean isSelectable=false;

    public HorizontalDualSliderActor(AB2Renderer2D renderer, int actorId, Vector3 v0, Vector3 v1,
                           Vector4 color, Vector4 hoverColor, Vector4 selectColor) {
        super(renderer, actorId);
        this.renderer2d=renderer;
        this.v0=v0;
        this.v1=v1;
        this.color=color;
        this.hoverColor=hoverColor;
        this.selectColor=selectColor;
    }

    public Vector4 getColor() {
        return color;
    }

    public void setColor(Vector4 color) {
        this.color = color;
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

    public void updateVertices(Vector3 v0, Vector3 v1) {
        this.v0=v0;
        this.v1=v1;
        needsResize=true;
    }

    private float[] computeVertexData() {
        float[] vertexData = {

                v0.get(0), v0.get(1), v0.get(2),    // lower left
                v1.get(0), v0.get(1), v0.get(2),    // lower right
                v0.get(0), v1.get(1), v0.get(2),    // upper left

                v1.get(0), v0.get(1), v1.get(2),    // lower right
                v1.get(0), v1.get(1), v1.get(2),    // upper right
                v0.get(0), v1.get(1), v1.get(2)     // upper left
        };
        return vertexData;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        //logger.info("init() called");

        if (shader instanceof AB2Basic2DShader) {

            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;

            // This combines positional vertices interleaved with 2D texture coordinates. Note: z not used
            // but necessary for shader compatibility.
            float[] vertexData=computeVertexData();

            vertexFb=createGLFloatBuffer(vertexData);

            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        //logger.info("display() called");

        if (needsResize) {
            float[] vertexData=computeVertexData();
            vertexFb=createGLFloatBuffer(vertexData);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, vertexFb.capacity() * 4, vertexFb);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            needsResize=false;
        }

        if (shader instanceof AB2Basic2DShader) {
            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;
            basic2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            //logger.info("display() actorId="+actorId);
            if (isSelectable() && isSelected) {
                basic2DShader.setColor(gl, selectColor);
            }
            else if (isHoverable() && isHovered) {
                basic2DShader.setColor(gl, hoverColor);
            }
            else {
                basic2DShader.setColor(gl, color);
            }
        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            pickShader.setPickId(gl, actorId);
        }

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d3 glBindVertexArray()");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d4 glBindBuffer()");

        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d5 glVertexAttribPointer()");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d6 glEnableVertexAttribArray()");

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/2);
        checkGlError(gl, "d9 glDrawArrays()");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        checkGlError(gl, "d10 glBindBuffer()");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
        }
        super.dispose(gl, shader);
    }

}
