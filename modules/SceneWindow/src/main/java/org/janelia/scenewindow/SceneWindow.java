
package org.janelia.scenewindow;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.swing.Action;
import javax.swing.JComponent;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Viewport;
import org.janelia.scenewindow.SceneRenderer.CameraType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cmbruns
 */
public class SceneWindow implements GLJComponent, Scene {
    
    // private AbstractCamera camera;
    private final Viewport viewport = new Viewport();
    // private final Vantage vantage;
    private GLJComponent glCanvas;
    private final SceneRenderer renderer;
    private final BasicScene scene;
    // private CameraType cameraType = null;
    // private Scene scene;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public SceneWindow(Vantage vantage, CameraType cameraType) {
        scene = new BasicScene(vantage);
        GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities requestedCapabilities = new GLCapabilities(profile);
        
        // Problem: On Linux, asking for stereo and failing, causes double buffering
        // to fail too.
        requestedCapabilities.setDoubleBuffered(true);
        // requestedCapabilities.setStereo(true); // Causes double buffering to fail on Linux

        // Antialiasing
        // requestedCapabilities.setSampleBuffers(true);
        // requestedCapabilities.setNumSamples(8);
        
        glCanvas = GLJComponentFactory.createGLJComponent(requestedCapabilities);
        
        renderer = new SceneRenderer(vantage, viewport, cameraType);
        glCanvas.getGLAutoDrawable().addGLEventListener(renderer);
        //
        
        // Repaint window when camera viewpoint changes
        renderer.getCamera().getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // System.out.println("Camera changed");
                glCanvas.getInnerComponent().repaint();
            }
        });
    }
    
    @Override
    public JComponent getOuterComponent() {
        return glCanvas.getOuterComponent();
    }

    @Override
    public Component getInnerComponent() {
        return glCanvas.getInnerComponent();
    }

    @Override
    public GLAutoDrawable getGLAutoDrawable() {
        return glCanvas.getGLAutoDrawable();
    }
    
    @Override
    public void setControlsVisibility(boolean visible) {
        glCanvas.setControlsVisibility(visible);
    }
    
    @Override
    public void addPlayForwardListener(ActionListener listener) {
        glCanvas.addPlayForwardListener(listener);
    }

    @Override
    public void addPlayReverseListener(ActionListener listener) {
        glCanvas.addPlayReverseListener(listener);
    }

    @Override
    public void addPauseListener(ActionListener listener) {
        glCanvas.addPauseListener(listener);
    }    

    // immediate blocking repainting
    public boolean redrawImmediately() {
        GLAutoDrawable glad = getGLAutoDrawable();
        if (glad == null) return false;
        GLContext context = glad.getContext();
        if (context == null) return false;
        int result = context.makeCurrent();
        if (result == GLContext.CONTEXT_NOT_CURRENT)
            return false;
        try {
            glad.display();
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
        }
        finally {
            context.release();
        }
        return true;
    }
    
    public AbstractCamera getCamera() {
        return renderer.getCamera();
    }

    public SceneRenderer getRenderer() {
        return renderer;
    }   

    public void setBackgroundColor(Color backgroundColor) {
        renderer.setBackgroundColor(backgroundColor);
    }

    @Override
    public Collection<? extends Light> getLights() {
        return scene.getLights();
    }

    @Override
    public Collection<? extends Vantage> getCameras() {
        return scene.getCameras();
    }

    @Override
    public Scene add(Light light) {
        scene.add(light);
        return this;
    }

    @Override
    public Scene add(Vantage camera) {
        scene.add(camera);
        return this;
    }

    @Override
    public CompositeObject3d addChild(Object3d child) {
        scene.addChild(child);
        return this;
    }

    @Override
    public Vantage getVantage() {
        return scene.getVantage();
    }

    @Override
    public Object3d getParent() {
        return scene.getParent();
    }

    @Override
    public Object3d setParent(Object3d parent) {
        scene.setParent(parent);
        return this;
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
        return scene.getChildren();
    }

    @Override
    public Matrix4 getTransformInWorld() {
        return scene.getTransformInWorld();
    }

    @Override
    public Matrix4 getTransformInParent() {
        return scene.getTransformInParent();
    }

    @Override
    public boolean isVisible() {
        return scene.isVisible();
    }

    @Override
    public Object3d setVisible(boolean isVisible) {
        scene.setVisible(isVisible);
        return this;
    }

    @Override
    public String getName() {
        return scene.getName();
    }

    @Override
    public Object3d setName(String name) {
        scene.setName(name);
        return this;
    }
}
