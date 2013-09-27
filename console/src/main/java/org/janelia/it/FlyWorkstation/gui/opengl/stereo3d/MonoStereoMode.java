package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;

public class MonoStereoMode extends AbstractStereoMode 
{
    public MonoStereoMode(
    		ObservableCamera3d camera,
    		GLActor monoActor)
	{
    	super(camera, monoActor);
	}

}
