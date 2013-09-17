package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class LightingActor implements GLActor {

	@Override
	public void display(GLAutoDrawable glDrawable) {}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return null;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		final float YELLOW[]={1,1,0,0}, WHITE[]={1,1,1,0}, GREY[]={0.3f, 0.3f, 0.3f, 0 } ;
	    
        GL2 gl = glDrawable.getGL().getGL2();
	    // Set up the appropriate processing steps for lighting.
	    gl.glPolygonMode( GL2.GL_FRONT, GL2.GL_FILL ) ;
	    gl.glEnable( GL2.GL_NORMALIZE ) ; 
	    gl.glEnable( GL2.GL_LIGHTING ) ;

	    // One Phong light source    
	    float lightPos[]= {1,1,1,0} ;  
	    gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0) ;  
	    gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, WHITE, 0) ;
	    gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, GREY, 0) ;
	    gl.glEnable( GL2.GL_LIGHT0 ) ;

	    // The material for all the solids drawn will be default yellow.
	    gl.glMaterialfv( GL2.GL_FRONT, GL2.GL_DIFFUSE,  YELLOW, 0);   
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {}

}
