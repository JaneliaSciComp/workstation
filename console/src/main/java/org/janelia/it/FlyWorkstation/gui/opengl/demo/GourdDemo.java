package org.janelia.it.FlyWorkstation.gui.opengl.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
// GLJPanel won't work with GL3!
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.FullScreenMode;
import org.janelia.it.FlyWorkstation.gui.TrackballInteractor;
import org.janelia.it.FlyWorkstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;
import org.janelia.it.FlyWorkstation.gui.opengl.LightingActor;
import org.janelia.it.FlyWorkstation.gui.opengl.MeshActor;
import org.janelia.it.FlyWorkstation.gui.opengl.MeshGroupActor;
import org.janelia.it.FlyWorkstation.gui.opengl.PolygonalMesh;
import org.janelia.it.FlyWorkstation.gui.opengl.SolidBackgroundActor;
import org.janelia.it.FlyWorkstation.gui.opengl.stereo3d.StereoModeChooser;

@SuppressWarnings("serial")
public class GourdDemo extends JFrame
{

	static {
		// So GLCanvas will not occlude menus
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
	}
	
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	new GourdDemo();
            }
        });
	}

	public GourdDemo() {
    	setTitle("Gourd Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create canvas for openGL display of gourd
        GLCapabilities glCapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        // GLCapabilities glCapabilities = new GLCapabilities(GLProfile.getDefault());
        // glCapabilities.setStereo(true);
        GLCanvas glPanel = new GLCanvas(glCapabilities);
        // GLJPanel glPanel = new GLJPanel(glCapabilities); // DOES NOT WORK WITH GL3!?!
        //
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

	    GLSceneComposer sceneComposer = 
	            new GLSceneComposer(camera, glPanel);
	    sceneComposer.addBackgroundActor(new SolidBackgroundActor(
	            Color.gray));
	    sceneComposer.addOpaqueActor(new LightingActor());
	    MeshGroupActor meshGroup = new MeshGroupActor();
	    meshGroup.addActor(new MeshActor(gourdMesh));
	    sceneComposer.addOpaqueActor(meshGroup);
	    // sceneComposer.addOpaqueActor(new TeapotActor());
        
        // Enable stereo 3D selection
        StereoModeChooser stereoModeChooser = new StereoModeChooser(glPanel);
        stereoModeChooser.stereoModeChangedSignal.connect(sceneComposer.setStereoModeSlot);
        // Menus
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);        
        viewMenu.add(stereoModeChooser.createJMenuItem());	    
	    
        // Apply mouse interactions: drag to rotate etc.
        // Another one-line functionality decorator!
        new TrackballInteractor(glPanel, camera);        
		
        //Display the window.
        pack();
        setVisible(true);		
	}
	
}
