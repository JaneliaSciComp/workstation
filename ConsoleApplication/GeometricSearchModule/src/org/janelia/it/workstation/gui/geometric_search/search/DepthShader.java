package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;

/**
 * Created by murphys on 3/18/15.
 */
public class DepthShader extends AbstractShader implements GLActor {
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "DepthShaderVertex.glsl";
    public static final String FRAGMENT_SHADER = "DepthShaderFragment.glsl";

    private int previousShader = 0;

    private float zminAdjustment = 0.0f;
    private float zmaxAdjustment = 0.5f;

    private boolean bIsInitialized=false;

    private final Logger logger = LoggerFactory.getLogger(DepthShader.class);

    List<GLActor> actorList=new ArrayList<>();

    public void addActor(GLActor actor) {
        actorList.add(actor);
    }

    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void load(GL2 gl) {
        IntBuffer buffer = IntBuffer.allocate( 1 );
        gl.glGetIntegerv( GL2.GL_CURRENT_PROGRAM, buffer );
        previousShader = buffer.get();
        int shaderProgram = getShaderProgram();

        gl.glUseProgram( shaderProgram );

        pushZminUniform(gl, shaderProgram);
        pushZmaxUniform(gl, shaderProgram);
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    private void pushZminUniform( GL2 gl, int shaderProgram ) {
        int hasZmin = gl.glGetUniformLocation(shaderProgram, "zmin");
        if ( hasZmin == -1 ) {
            throw new RuntimeException( "Failed to find zmin" );
        }
        gl.glUniform1f(hasZmin, zminAdjustment);
    }

    private void pushZmaxUniform( GL2 gl, int shaderProgram ) {
        int hasZmax = gl.glGetUniformLocation(shaderProgram, "zmax");
        if ( hasZmax == -1 ) {
            throw new RuntimeException( "Failed to find zmax" );
        }
        gl.glUniform1f( hasZmax, zmaxAdjustment );
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        if (!bIsInitialized) {
            GL2 gl = glDrawable.getGL().getGL2();
            reportError(gl, "DepthShader init() pre");
            try {
                logger.info("init() calling super.init()");
                super.init(gl);
                reportError(gl, "DepthShader init() post");
            }
            catch (ShaderCreationException ex) {
                ex.printStackTrace();
            }
            for (GLActor actor : actorList) {
                actor.init(glDrawable);
            }
            bIsInitialized = true;
        }
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        reportError(gl, "DepthShader display() start");
        if (!bIsInitialized)
            init(glDrawable);

        load(gl);

        gl.glClearColor(0.501f, 0.501f, 0.501f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2.GL_SINGLE_COLOR);




        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL2.GL_GREATER, 0.5f);
        gl.glDisable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        for (GLActor actor : actorList) {
            actor.display(glDrawable);
        }

        unload(gl);

        //gl.glDisable(GL2.GL_BLEND);

        reportError(gl, "DepthShader display() end");
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        bIsInitialized = false;
        for (GLActor actor : actorList) {
            actor.dispose(glDrawable);
        }
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d boundingBox = new BoundingBox3d();
        for (GLActor actor : actorList) {
            boundingBox.include(actor.getBoundingBox3d());
        }
        if (boundingBox.isEmpty())
            boundingBox.include(new Vec3(0,0,0));
        return boundingBox;
    }

}
