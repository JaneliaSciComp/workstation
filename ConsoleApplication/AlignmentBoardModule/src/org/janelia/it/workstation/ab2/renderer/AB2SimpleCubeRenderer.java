package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;

public class AB2SimpleCubeRenderer extends AB2Basic3DRenderer {

    public AB2SimpleCubeRenderer() {
        super(new AB2Basic3DShader());
    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                AB2Basic3DShader cubeShader=(AB2Basic3DShader)shader;
                cubeShader.setMVP(gl, mvp);
                gl.glPointSize(10.0f);
                gl.glDrawArrays(gl.GL_POINTS, 0, 1);
            }
        };
    }

}
