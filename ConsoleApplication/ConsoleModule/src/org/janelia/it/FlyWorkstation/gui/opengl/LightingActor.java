package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class LightingActor implements GL3Actor {

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return null;
	}

	private void initGL2(GL2 gl2) {
		final float YELLOW[]={1,1,0,0}, WHITE[]={1,1,1,0}, GREY[]={0.3f, 0.3f, 0.3f, 0 } ;
	    
        GL gl = gl2.getGL();
	    // Set up the appropriate processing steps for lighting.
	    gl2.glPolygonMode( GL2.GL_FRONT, GL2.GL_FILL ) ;
	    gl2.glEnable( GL2.GL_NORMALIZE ) ; 
	    gl2.glEnable( GL2.GL_LIGHTING ) ;

	    // One Phong light source    
	    float lightPos[]= {1,1,1,0} ;  
	    gl2.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0) ;
	    gl2.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, WHITE, 0) ;
	    gl2.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, GREY, 0) ;
	    gl2.glEnable( GL2.GL_LIGHT0 ) ;

	    // The material for all the solids drawn will be default yellow.
	    gl2.glMaterialfv( GL2.GL_FRONT, GL2.GL_DIFFUSE,  YELLOW, 0);   
	}

    @Override
    public void display(GLActorContext context) {
    }

    @Override
    public void init(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        if (gl.isGL2()) {
            initGL2(gl.getGL2());
        }
        else {
            // TODO
        }
    }

    @Override
    public void dispose(GLActorContext context) {
    }

}
