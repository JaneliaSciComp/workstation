package org.janelia.it.workstation.gui.viewer3d;

import javax.media.opengl.GL2;

class OcclusiveRenderer 
    extends ActorRenderer
{
    // scene objects
    public OcclusiveRenderer() {
        super();
    }
    
    protected void displayBackground(GL2 gl) 
    {
        // paint solid background color
	    gl.glClearColor(
	    		backgroundColor.getRed()/255.0f,
	    		backgroundColor.getGreen()/255.0f,
	    		backgroundColor.getBlue()/255.0f,
	    		backgroundColor.getAlpha()/255.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);    		
    }
}