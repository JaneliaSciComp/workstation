package org.janelia.workstation.gui.viewer3d;

public class OcclusiveViewer extends Viewer3d {
	
	public OcclusiveViewer() {
        setActorRenderer( new OcclusiveRenderer() );
    }
    
    public OcclusiveViewer( OcclusiveRenderer renderer ) {
        setActorRenderer(renderer);
    }
    
}
