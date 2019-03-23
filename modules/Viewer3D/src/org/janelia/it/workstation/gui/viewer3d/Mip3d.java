package org.janelia.it.workstation.gui.viewer3d;

public class Mip3d extends Viewer3d {
	
	public Mip3d() {
        setActorRenderer( new MipRenderer() );
    }

}
