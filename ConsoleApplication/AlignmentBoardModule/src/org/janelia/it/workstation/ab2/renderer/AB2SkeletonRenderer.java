package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.shader.AB2SkeletonShader;

public class AB2SkeletonRenderer extends AB2Basic3DRenderer {

    public AB2SkeletonRenderer() {
        super(new AB2SkeletonShader());
    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {

            }
        };
    }

}
