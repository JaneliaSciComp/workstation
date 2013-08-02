package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import java.awt.Dimension;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.jogamp.opengl.util.gl2.GLUT;

public class TeapotDemo extends JFrame
{

	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	new TeapotDemo();
            }
        });
	}

	public TeapotDemo() {
    	setTitle("Teapot Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TeapotPanel teapotPanel = new TeapotPanel();
        getContentPane().add(teapotPanel);

        //Display the window.
        pack();
        setVisible(true);		
	}
	
	class TeapotPanel extends GLJPanel
	implements GLEventListener
	{
	    private final GLUT glut = new GLUT();
	    private final GLU glu = new GLU();

		TeapotPanel() {
			setPreferredSize(new Dimension(400,400));
			addGLEventListener(this);
		}

		@Override
		public void display(GLAutoDrawable glDrawable) {
	        final GL2 gl = glDrawable.getGL().getGL2();
	        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
	        paintTeapot(gl);
		}

		@Override
		public void dispose(GLAutoDrawable arg0) {
		}

		@Override
		public void init(GLAutoDrawable glDrawable) {
	        final GL2 gl = glDrawable.getGL().getGL2();
		    gl.glClearColor(0.9f,0.9f,1.0f,1);
		    gl.glEnable( GL2.GL_DEPTH_TEST ) ;
		    setUpLighting(gl);
		}

		public void paintTeapot(GL2 gl) {
			// due to a bug in glutSolidTeapot, triangle vertices are in CW order 
	        gl.glPushAttrib(GL2.GL_POLYGON_BIT); // remember current GL_FRONT_FACE indicator
	        gl.glFrontFace( GL2.GL_CW ); 
	        gl.glColor3f(0.40f, 0.27f, 0.00f);
	        glut.glutSolidTeapot(1.0);
	        gl.glPopAttrib(); // restore GL_FRONT_FACE
		}

		@Override
		public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) 
		{
	        final GL2 gl = glDrawable.getGL().getGL2();
			updateProjection(gl);
		}
		
		private void setUpLighting(GL2 gl) {
			final float YELLOW[]={1,1,0,0}, WHITE[]={1,1,1,0}, GREY[]={0.3f, 0.3f, 0.3f, 0 } ;
			    
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
		
		private void updateProjection(GL2 gl) {
			gl.glViewport(0, 0, getWidth(), getHeight());

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			double aspect = 1.0;
			if (getWidth()*getHeight() > 0)
				aspect = getWidth()/(double)getHeight();
		    glu.gluPerspective(45.0, // fovy
		    		aspect,
		    		0.1, // znear
		    		10.0); // zfar
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glTranslated(0, 0, -5);
		}
	};
}
