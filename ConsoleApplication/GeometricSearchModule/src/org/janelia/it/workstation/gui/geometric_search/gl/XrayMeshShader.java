package org.janelia.it.workstation.gui.geometric_search.gl;

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
 * Created by murphys on 3/17/15.
 */
public class XrayMeshShader extends AbstractShader implements GLActor {
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "XrayMeshShaderVertex.glsl";
    public static final String FRAGMENT_SHADER = "XrayMeshShaderFragment.glsl";

    private int previousShader = 0;

    private float ambientAdjustment = 0.0f;
    private float edgefalloffAdjustment = 1.0f;
    private float intensityAdjustment = 0.1f;

    private boolean bIsInitialized=false;

    private final Logger logger = LoggerFactory.getLogger(XrayMeshShader.class);

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

        pushAmbientUniform(gl, shaderProgram);
        pushEdgefalloffUniform(gl, shaderProgram);
        pushIntensityUniform(gl, shaderProgram);
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    private void pushAmbientUniform( GL2 gl, int shaderProgram ) {
        int hasAmbient = gl.glGetUniformLocation(shaderProgram, "ambient");
        if ( hasAmbient == -1 ) {
            throw new RuntimeException( "Failed to find ambient" );
        }
        gl.glUniform1f(hasAmbient, ambientAdjustment);
    }

    private void pushEdgefalloffUniform( GL2 gl, int shaderProgram ) {
        int hasEdgefalloff = gl.glGetUniformLocation(shaderProgram, "ambient");
        if ( hasEdgefalloff == -1 ) {
            throw new RuntimeException( "Failed to find edgefalloff" );
        }
        gl.glUniform1f( hasEdgefalloff, edgefalloffAdjustment );
    }

    private void pushIntensityUniform( GL2 gl, int shaderProgram ) {
        int hasIntensity = gl.glGetUniformLocation(shaderProgram, "ambient");
        if ( hasIntensity == -1 ) {
            throw new RuntimeException( "Failed to find intensity" );
        }
        gl.glUniform1f( hasIntensity, intensityAdjustment );
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        if (!bIsInitialized) {
            GL2 gl = glDrawable.getGL().getGL2();
            reportError(gl, "XrayMeshShader init() pre");
            try {
                logger.info("init() calling super.init()");
                super.init(gl);
                reportError(gl, "XrayMeshShader init() post");
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
        reportError(gl, "XrayMeshShader display() start");
        if (!bIsInitialized)
            init(glDrawable);

        gl.glClearColor(0.0f, 0.0f, 0.1f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl.glClearColor(0.501f, 0.501f, 0.501f, 1.0f);
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL2.GL_GREATER, 0.5f);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        load(gl);
        for (GLActor actor : actorList) {
            actor.display(glDrawable);
        }
        unload(gl);

        reportError(gl, "XrayMeshShader display() end");
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

