package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GL3Actor;

public class MonoStereoMode extends AbstractStereoMode 
{
    public MonoStereoMode(
    		ObservableCamera3d camera,
    		GL3Actor monoActor)
	{
    	super(camera, monoActor);
	}

}
