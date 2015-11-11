/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.scenewindow;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Viewport;
import org.janelia.scenewindow.SceneRenderer.CameraType;
import org.janelia.scenewindow.stereo.HardwareRenderer;
import org.openide.util.Exceptions;

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

    // immediate blocking repainting
    public boolean redrawNow() {
        GLAutoDrawable glad = getGLAutoDrawable();
        if (glad == null) return false;
        GLContext context = glad.getContext();
        if (context == null) return false;
        context.makeCurrent();
        glad.display();
        context.release();
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
