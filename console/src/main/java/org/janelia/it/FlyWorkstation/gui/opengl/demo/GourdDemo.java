package org.janelia.it.FlyWorkstation.gui.opengl.demo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
// GLJPanel won't work with hardware stereo 3d
// import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.FullScreenMode;
import org.janelia.it.FlyWorkstation.gui.TrackballInteractor;
import org.janelia.it.FlyWorkstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.CompositeGLActor;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;
import org.janelia.it.FlyWorkstation.gui.opengl.LightingActor;
import org.janelia.it.FlyWorkstation.gui.opengl.MeshActor;
import org.janelia.it.FlyWorkstation.gui.opengl.PolygonalMesh;
import org.janelia.it.FlyWorkstation.gui.opengl.SolidBackgroundActor;
import org.janelia.it.FlyWorkstation.gui.opengl.stereo3d.AbstractStereoMode;
// import org.janelia.it.FlyWorkstation.gui.opengl.stereo3d.HardwareStereoMode;
import org.janelia.it.FlyWorkstation.gui.opengl.stereo3d.MonoStereoMode;
import org.janelia.it.FlyWorkstation.signal.Slot;

@SuppressWarnings("serial")
public class GourdDemo extends JFrame
{

	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	new GourdDemo();
            }
        });
	}

	Component glComponent;
	
	public GourdDemo() {
    	setTitle("Gourd Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create canvas for openGL display of gourd
        GLCapabilities glCapabilities = new GLCapabilities(GLProfile.getDefault());
        // glCapabilities.setStereo(true);
        GLCanvas glPanel = new GLCanvas(glCapabilities);
        //
        glComponent = glPanel;
        glPanel.setPreferredSize(new Dimension(1280, 800));
        getContentPane().add(glPanel);

        // Watch keyboard to detect fullscreen shortcut.
        // Hey! A one line full screen mode decorator!
        addKeyListener(new FullScreenMode(this));

        PolygonalMesh gourdMesh;
        try {
            gourdMesh = PolygonalMesh.createMeshFromObjFile(
                    this.getClass().getResourceAsStream("gourd.obj"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
		// Create camera
		ObservableCamera3d camera = new BasicObservableCamera3d();
		camera.setFocus(new Vec3(0, 0, 0));
		camera.setPixelsPerSceneUnit(200);

		boolean oldStereoMode = false;
		if (oldStereoMode) { // works
		    // Create non-stereo-3D actor component
		    CompositeGLActor monoActor = new CompositeGLActor();
		    // Use 3D lighting
		    monoActor.addActor(new LightingActor());
		    monoActor.addActor(new MeshActor(gourdMesh));
		    AbstractStereoMode stereoMode = new MonoStereoMode(
		            camera, monoActor);
		    // stereoMode.setSwapEyes(true);
		    glPanel.addGLEventListener(stereoMode);
		    // Is this needed? YES
		    camera.getViewChangedSignal().connect(
		            new Slot() {
		                @Override
		                public void execute() {
		                    glComponent.repaint();
		                }
		            });
		}
		else { // no gourd... TODO
		    // Wrap mono actor in stereo 3D mode
		    GLSceneComposer sceneComposer = 
		            new GLSceneComposer(camera, glPanel);
		    sceneComposer.addBackgroundActor(new SolidBackgroundActor(
		            Color.lightGray));
		    sceneComposer.addOpaqueActor(new LightingActor());
		    sceneComposer.addOpaqueActor(new MeshActor(gourdMesh));
		}
        
        // Apply mouse interactions: drag to rotate etc.
        // Another one-line functionality decorator!
        new TrackballInteractor(glPanel, camera);        
		
        //Display the window.
        pack();
        setVisible(true);		
	}
	
}
