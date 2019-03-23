package org.janelia.it.workstation.gui.opengl.stereo3d;

import org.janelia.it.workstation.gui.opengl.GLActorContext;
import org.janelia.it.workstation.gui.opengl.GLSceneComposer;

import javax.media.opengl.GLAutoDrawable;

public interface StereoMode {

    void display(GLActorContext actorContext, GLSceneComposer composer);

    void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height);

	void reshape(int width, int height);

    boolean isEyesSwapped();

    void setEyesSwapped(boolean eyesSwapped);

    void dispose(GLAutoDrawable glDrawable);
}
