package org.janelia.it.workstation.gui.opengl.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
// GLJPanel won't work with GL3!
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.janelia.it.workstation.gui.FullScreenMode;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.opengl.MeshGroupActor;
import org.janelia.it.workstation.gui.opengl.SolidBackgroundActor;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.opengl.JoglVersion;

@SuppressWarnings("serial")
public class GourdDemo extends JFrame
{

    static {
        // So GLCanvas will not occlude menus
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        // Use top menu bar on Mac
        if (System.getProperty("os.name").contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gourd Demo");
        }
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Warning: Failed to set native look and feel.");
        }
    }
    
    public static GLProfile glProfile 
            = GLProfile.getMinimum(true);

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
        
        JoglVersion foo = JoglVersion.getInstance();
        System.out.println(foo);
        GlueGenVersion foo2 = GlueGenVersion.getInstance();
        System.out.println(foo2);

        // Create canvas for openGL display of gourd
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setStencilBits(8); // causes crash on Mac Mountain Lion OS X 10.8.5 (with older jogl 2.0, 2.1 is OK)
        glCapabilities.setStereo(true);
        glCapabilities.setDoubleBuffered(true);
        GLCanvas glPanel = new GLCanvas(glCapabilities);
        // GLJPanel glPanel = new GLJPanel(glCapabilities); // DOES NOT WORK WITH GL3!?!
        //
        glPanel.setPreferredSize(new Dimension(1280, 800));
        getContentPane().add(glPanel);

        // Watch keyboard to detect fullscreen shortcut.
        // Hey! A one line full screen mode decorator!
        addKeyListener(new FullScreenMode(this));

        org.janelia.it.workstation.gui.opengl.PolygonalMesh gourdMesh;
        try {
            gourdMesh = org.janelia.it.workstation.gui.opengl.PolygonalMesh.createMeshFromObjFile(
                    this.getClass().getResourceAsStream("gourd.obj"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Create camera
        org.janelia.it.workstation.gui.camera.ObservableCamera3d camera = new BasicObservableCamera3d();
        camera.setFocus(new org.janelia.it.workstation.geom.Vec3(0, 0, 0));
        camera.setPixelsPerSceneUnit(200);

        org.janelia.it.workstation.gui.opengl.GLSceneComposer sceneComposer =
            new org.janelia.it.workstation.gui.opengl.GLSceneComposer(camera, glPanel);
        sceneComposer.addBackgroundActor(new SolidBackgroundActor(
                Color.gray));
        sceneComposer.addOpaqueActor(new org.janelia.it.workstation.gui.opengl.LightingActor());
        MeshGroupActor meshGroup = new MeshGroupActor();
        meshGroup.addActor(new org.janelia.it.workstation.gui.opengl.MeshActor(gourdMesh));
        sceneComposer.addOpaqueActor(meshGroup);
        // sceneComposer.addOpaqueActor(new TeapotActor());

        // Enable stereo 3D selection
        org.janelia.it.workstation.gui.opengl.stereo3d.StereoModeChooser stereoModeChooser = new org.janelia.it.workstation.gui.opengl.stereo3d.StereoModeChooser(glPanel);
        stereoModeChooser.stereoModeChangedSignal.connect(sceneComposer.setStereoModeSlot);
        // Menus
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);        
        viewMenu.add(stereoModeChooser.createJMenuItem());	    

        // Apply mouse interactions: drag to rotate etc.
        // Another one-line functionality decorator!
        new org.janelia.it.workstation.gui.TrackballInteractor(glPanel, camera);

        //Display the window.
        pack();
        setVisible(true);		
    }

}
