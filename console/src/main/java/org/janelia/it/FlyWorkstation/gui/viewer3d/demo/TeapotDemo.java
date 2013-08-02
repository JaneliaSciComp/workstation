package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import javax.swing.JFrame;

import org.janelia.it.FlyWorkstation.gui.viewer3d.TeapotActor;

@SuppressWarnings("serial")
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

        // Create canvas for openGL display of teapot
        PerspectiveGLPanel teapotPanel = new PerspectiveGLPanel();
        // Apply mouse interactions: drag to rotate etc.
        new TrackballInteractor(teapotPanel, teapotPanel.getCamera());
        // Set background color
        teapotPanel.addActor(new SolidBackgroundActor(0.9f,1,0.9f));
        // Use 3D lighting
        teapotPanel.addActor(new LightingActor());
        // Use Z buffer for hidden surface removal
        teapotPanel.addActor(new DepthBufferActor());
        // Prepare to draw a teapot
		teapotPanel.addActor(new TeapotActor());
        getContentPane().add(teapotPanel);

        //Display the window.
        pack();
        setVisible(true);		
	};
}
