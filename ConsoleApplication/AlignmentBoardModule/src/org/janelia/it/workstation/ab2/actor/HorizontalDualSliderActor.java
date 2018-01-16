package org.janelia.it.workstation.ab2.actor;

import java.awt.Point;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.controller.AB2UserContext;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDragEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLSelectable;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorizontalDualSliderActor extends GLAbstractActor {

    // todo: finish converting this from ColorBox2DActor template

    private final Logger logger = LoggerFactory.getLogger(HorizontalDualSliderActor.class);

    private enum SliderSide { Left, Right };

    int glWidth=1000;
    int glHeight=1000;

    Vector3 v0;
    Vector3 v1;

    int slider1Id=0;
    int slider2Id=0;

    Vector4 backgroundColor;
    Vector4 guideColor;
    Vector4 sliderColor;
    Vector4 sliderHoverColor;

    float slider1Position=0.0f;
    float slider2Position=1.0f;

    IntBuffer backgroundVertexArrayId=IntBuffer.allocate(1);
    IntBuffer backgroundVertexBufferId=IntBuffer.allocate(1);
    FloatBuffer backgroundVertexFb;

    IntBuffer guideVertexArrayId=IntBuffer.allocate(1);
    IntBuffer guideVertexBufferId=IntBuffer.allocate(1);
    FloatBuffer guideVertexFb;

    IntBuffer slider1VertexArrayId=IntBuffer.allocate(1);
    IntBuffer slider1VertexBufferId=IntBuffer.allocate(1);
    FloatBuffer slider1VertexFb;

    IntBuffer slider2VertexArrayId=IntBuffer.allocate(1);
    IntBuffer slider2VertexBufferId=IntBuffer.allocate(1);
    FloatBuffer slider2VertexFb;

    AB2Renderer2D renderer2d;

    public HorizontalDualSliderActor(AB2Renderer2D renderer,
                                     int actorId,
                                     int slider1Id,
                                     int slider2Id,
                                     Vector3 v0,
                                     Vector3 v1,
                                     Vector4 backgroundColor,
                                     Vector4 guideColor,
                                     Vector4 sliderColor,
                                     Vector4 sliderHoverColor) {
        super(renderer, actorId);
        this.renderer2d=renderer;
        this.slider1Id=slider1Id;
        this.slider2Id=slider2Id;
        this.v0=v0;
        this.v1=v1;
        this.backgroundColor=backgroundColor;
        this.guideColor=guideColor;
        this.sliderColor=sliderColor;
        this.sliderHoverColor=sliderHoverColor;

        logger.info("-- actorId="+actorId+" slider1="+slider1Id+" slider2="+slider2Id);

        registerAlternateIdForActor(this, slider1Id);
        registerAlternateIdForActor(this, slider2Id);
    }

    public int getSlider1Id() { return slider1Id; }

    public int getSlider2Id() { return slider2Id; }

    @Override
    protected void glWindowResize(int width, int height) {
        this.glWidth=width;
        this.glHeight=height;
        needsResize=true;
    }

    public void updateVertices(Vector3 v0, Vector3 v1) {
        this.v0=v0;
        this.v1=v1;
        needsResize=true;
    }

    public void setSlider1Position(float p) {
        if (p<0.0f) {
            p=0.0f;
        } else if (p>1.0f-0.0001f) {
            p=1.0f-0.0001f;
        }
        if (p<slider2Position) {
            slider1Position=p;
        } else {
            slider1Position=slider2Position-0.0001f;
        }
        needsResize=true;
    }

    public void setSlider2Position(float p) {
        if (p<0.0001f) {
            p=0.000f;
        } else if (p>1.0f) {
            p=1.0f;
        }
        if (p>slider1Position) {
            slider2Position=p;
        } else {
            slider2Position=slider1Position+0.0001f;
        }
        needsResize=true;
    }


    private float[] computeBackgroundVertexData() {
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

    private float[] computeGuideVertexData() {

        float gh=(float)(1.0/(1.0*glHeight));
        float gw=(float)((0.9*(v1.getX()-v0.getX()))/2.0);
        float xC=(v1.getX()+v0.getX())/2.0f;
        float yC=(v1.getY()+v0.getY())/2.0f;
        float z=v0.get(2)-0.05f;

        float[] vertexData = {

                xC-gw, yC-gh, z,
                xC+gw, yC-gh, z,
                xC-gw, yC+gh, z,

                xC+gw, yC-gh, z,
                xC+gw, yC+gh, z,
                xC-gw, yC+gh, z
        };
        return vertexData;
    }

    private float[] computeSliderVertexData(float p, SliderSide sliderSide) {

        float sw=(float)(10.0/(1.0*glWidth));
        float sh=(float)((0.9*(v1.getY()-v0.getY()))/2.0);
        float xC=((v1.getX()-v0.getX())*p*0.9f)+v0.getX()+(v1.getX()-v0.getX())*0.05f;
        float yC=(v1.getY()+v0.getY())/2.0f;
        float z=v0.get(2)-0.1f;

        if (sliderSide.equals(SliderSide.Left)) {
            xC=xC-sw;
        } else {
            xC=xC+sw;
        }

        float[] vertexData = {

                xC-sw, yC-sh, z,
                xC+sw, yC-sh, z,
                xC-sw, yC+sh, z,

                xC+sw, yC-sh, z,
                xC+sw, yC+sh, z,
                xC-sw, yC+sh, z
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
            backgroundVertexFb=createGLFloatBuffer(backgroundVertexData);
            gl.glGenVertexArrays(1, backgroundVertexArrayId);
            gl.glBindVertexArray(backgroundVertexArrayId.get(0));
            gl.glGenBuffers(1, backgroundVertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, backgroundVertexFb.capacity() * 4, backgroundVertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            float[] guideVertexData=computeGuideVertexData();
            guideVertexFb=createGLFloatBuffer(guideVertexData);
            gl.glGenVertexArrays(1, guideVertexArrayId);
            gl.glBindVertexArray(guideVertexArrayId.get(0));
            gl.glGenBuffers(1, guideVertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, guideVertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, guideVertexFb.capacity() * 4, guideVertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            float[] slider1VertexData=computeSliderVertexData(slider1Position, SliderSide.Left);
            slider1VertexFb=createGLFloatBuffer(slider1VertexData);
            gl.glGenVertexArrays(1, slider1VertexArrayId);
            gl.glBindVertexArray(slider1VertexArrayId.get(0));
            gl.glGenBuffers(1, slider1VertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider1VertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, slider1VertexFb.capacity() * 4, slider1VertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            float[] slider2VertexData=computeSliderVertexData(slider2Position, SliderSide.Right);
            slider2VertexFb=createGLFloatBuffer(slider2VertexData);
            gl.glGenVertexArrays(1, slider2VertexArrayId);
            gl.glBindVertexArray(slider2VertexArrayId.get(0));
            gl.glGenBuffers(1, slider2VertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider2VertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, slider2VertexFb.capacity() * 4, slider2VertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

    }

    private void updateVertexBuffers(GL4 gl) {
        float[] backgroundVertexData=computeBackgroundVertexData();
        backgroundVertexFb=createGLFloatBuffer(backgroundVertexData);
        gl.glBindVertexArray(backgroundVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, backgroundVertexFb.capacity() * 4, backgroundVertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        float[] guideVertexData=computeGuideVertexData();
        guideVertexFb=createGLFloatBuffer(guideVertexData);
        gl.glBindVertexArray(guideVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, guideVertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER,0, guideVertexFb.capacity() * 4, guideVertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        float[] slider1VertexData=computeSliderVertexData(slider1Position, SliderSide.Left);
        slider1VertexFb=createGLFloatBuffer(slider1VertexData);
        gl.glBindVertexArray(slider1VertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider1VertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER,0, slider1VertexFb.capacity() * 4, slider1VertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        float[] slider2VertexData=computeSliderVertexData(slider2Position, SliderSide.Right);
        slider2VertexFb=createGLFloatBuffer(slider2VertexData);
        gl.glBindVertexArray(slider2VertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider2VertexBufferId.get(0));
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER,0, slider2VertexFb.capacity() * 4, slider2VertexFb);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawBackground(GL4 gl) {
        gl.glBindVertexArray(backgroundVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, backgroundVertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, backgroundVertexFb.capacity()/2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawGuide(GL4 gl) {
        gl.glBindVertexArray(guideVertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, guideVertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, guideVertexFb.capacity()/2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawLeftSlider(GL4 gl) {
        gl.glBindVertexArray(slider1VertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider1VertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, slider1VertexFb.capacity()/2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void drawRightSlider(GL4 gl) {
        gl.glBindVertexArray(slider2VertexArrayId.get(0));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, slider2VertexBufferId.get(0));
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, slider2VertexFb.capacity()/2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void pickBackground(GL4 gl, AB2PickShader shader) {
        shader.setPickId(gl, actorId);
        drawBackground(gl);
    }

    private void pickLeftSlider(GL4 gl, AB2PickShader shader) {
        shader.setPickId(gl, slider1Id);
        drawLeftSlider(gl);
    }

    private void pickRightSlider(GL4 gl, AB2PickShader shader) {
        shader.setPickId(gl, slider2Id);
        drawRightSlider(gl);
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

            basic2DShader.setColor(gl, backgroundColor);
            drawBackground(gl);

            basic2DShader.setColor(gl, guideColor);
            drawGuide(gl);

            if (hoverId==slider1Id || draggingIds.contains(slider1Id)) {
                basic2DShader.setColor(gl, sliderHoverColor);
            } else {
                basic2DShader.setColor(gl, sliderColor);
            }
            drawLeftSlider(gl);

            if (hoverId==slider2Id || draggingIds.contains(slider2Id)) {
                basic2DShader.setColor(gl, sliderHoverColor);
            } else {
                basic2DShader.setColor(gl, sliderColor);
            }
            drawRightSlider(gl);

        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            pickBackground(gl, pickShader);
            pickLeftSlider(gl, pickShader);
            pickRightSlider(gl, pickShader);
        }
    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {
            gl.glDeleteVertexArrays(1, backgroundVertexArrayId);
            gl.glDeleteBuffers(1, backgroundVertexBufferId);

            gl.glDeleteVertexArrays(1, guideVertexArrayId);
            gl.glDeleteBuffers(1, guideVertexBufferId);

            gl.glDeleteVertexArrays(1, slider1VertexArrayId);
            gl.glDeleteBuffers(1, slider1VertexBufferId);

            gl.glDeleteVertexArrays(1, slider2VertexArrayId);
            gl.glDeleteBuffers(1, slider2VertexBufferId);
        }
        super.dispose(gl, shader);
    }

    @Override
    public void processEvent(AB2Event event) {
        if (event instanceof AB2MouseDragEvent) {
            //logger.info("processEvent() received event="+event.getClass().getName());
            AB2UserContext userContext=AB2Controller.getController().getUserContext();
            List<Point> points=userContext.getPositionHistory();
            if (points.size()>1) {
                //logger.info("check 0.5");
                int maxIndex=points.size()-1;
                Point p1=points.get(maxIndex);
                Point p0=points.get(maxIndex-1);
                //logger.info("check 0.55");
                if (draggingIds.contains(slider1Id)) {
                    //logger.info("check1");
                    int xdiff=p1.x-p0.x;
                    if (xdiff>0) {
                        setSlider1Position(slider1Position+0.001f);
                    } else {
                        setSlider1Position(slider1Position-0.001f);
                    }
                }
                else if (draggingIds.contains(slider2Id)) {
                    //logger.info("check2");
                    int xdiff=p1.x-p0.x;
                    if (xdiff>0) {
                        setSlider2Position(slider2Position+0.001f);
                    } else {
                        setSlider2Position(slider2Position-0.001f);
                    }
                }
            } else {
                //logger.info("check 0.6");
            }
        }
    }

    @Override
    public boolean acceptsDropType(GLSelectable selectable) {
        if (selectable instanceof HorizontalDualSliderActor) {
            return true;
        }
        return false;
    }


}
