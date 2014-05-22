package org.janelia.it.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;

public interface StereoMode {

    void display(org.janelia.it.workstation.gui.opengl.GLActorContext actorContext, org.janelia.it.workstation.gui.opengl.GLSceneComposer composer);

    void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height);

	void reshape(int width, int height);

    boolean isEyesSwapped();

    void setEyesSwapped(boolean eyesSwapped);

    void dispose(GLAutoDrawable glDrawable);
}
