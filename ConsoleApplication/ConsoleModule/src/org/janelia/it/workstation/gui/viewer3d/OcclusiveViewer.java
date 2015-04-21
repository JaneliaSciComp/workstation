package org.janelia.it.workstation.gui.viewer3d;

public class OcclusiveViewer extends Viewer3d {
	
	public OcclusiveViewer() {
        setActorRenderer( new OcclusiveRenderer() );
    }

}
