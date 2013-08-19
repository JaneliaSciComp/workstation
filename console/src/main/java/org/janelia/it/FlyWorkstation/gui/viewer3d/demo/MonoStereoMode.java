package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import javax.media.opengl.GLEventListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

public class MonoStereoMode extends AbstractStereoMode 
{
    public MonoStereoMode(
    		ObservableCamera3d camera,
    		GLEventListener monoActor)
	{
    	super(camera, monoActor);
	}

}
