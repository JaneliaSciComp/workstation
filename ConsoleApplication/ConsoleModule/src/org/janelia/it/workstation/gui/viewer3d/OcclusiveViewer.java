package org.janelia.it.workstation.gui.viewer3d;

//import org.janelia.it.workstation.publication_quality.mesh.actor.MeshRenderer;

public class OcclusiveViewer extends Viewer3d {
	
	public OcclusiveViewer() {
        setActorRenderer( new OcclusiveRenderer() );
    }
    
    public OcclusiveViewer( OcclusiveRenderer renderer ) {
        setActorRenderer(renderer);
    }

}
