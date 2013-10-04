package org.janelia.it.FlyWorkstation.gui.opengl.demo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
// GLJPanel won't work with hardware stereo 3d
// import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.FullScreenMode;
import org.janelia.it.FlyWorkstation.gui.TrackballInteractor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.TeapotActor;
import org.janelia.it.FlyWorkstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.CompositeGLActor;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;
import org.janelia.it.FlyWorkstation.gui.opengl.LightingActor;
import org.janelia.it.FlyWorkstation.gui.opengl.SolidBackgroundActor;

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

	Component glComponent;
	
	public TeapotDemo() {
    	setTitle("Teapot Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Attempt to create a hardware stereo 3D capable OpenGL context
        GLCapabilities glCapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        // glCapabilities.setStereo(true);
        //
        // Create canvas for openGL display of teapot
        GLCanvas glPanel = new GLCanvas(glCapabilities);
        glComponent = glPanel;
        glPanel.setPreferredSize(new Dimension(1280, 800));
        getContentPane().add(glPanel);

        // Watch keyboard to detect fullscreen shortcut.
        // Hey! A one line full screen mode decorator!
        addKeyListener(new FullScreenMode(this));

        // Create non-stereo-3D actor component
        CompositeGLActor monoActor = new CompositeGLActor();
        // Use 3D lighting
        monoActor.addActor(new LightingActor());
        // Prepare to draw a teapot
		monoActor.addActor(new TeapotActor());
        
		// Create camera
		ObservableCamera3d camera = new BasicObservableCamera3d();
		camera.setFocus(new Vec3(0, 0, 0));
		camera.setPixelsPerSceneUnit(200);

		GLSceneComposer sceneComposer = 
		        new GLSceneComposer(camera, glPanel);
		sceneComposer.addBackgroundActor(new SolidBackgroundActor(
		        Color.lightGray));
		sceneComposer.addOpaqueActor(monoActor);

        // Apply mouse interactions: drag to rotate etc.
        // Another one-line functionality decorator!
        new TrackballInteractor(glPanel, camera);        
		
        //Display the window.
        pack();
        setVisible(true);		
	}
	
}
