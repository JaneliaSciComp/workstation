package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;

public interface StereoMode {

    void display(GLActorContext actorContext, GLSceneComposer composer);

    void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height);

	void reshape(int width, int height);

}
