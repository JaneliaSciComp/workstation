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

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import org.janelia.geometry3d.BasicObject3D;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Object3D;
import org.janelia.geometry3d.Vantage;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicScene implements Scene {
    private static int count = 0;
    
    private final int index;
    private Object3D object3d = new BasicObject3D(null);
    private List<Light> lights = new Vector<Light>();
    private List<Vantage> cameras = new Vector<Vantage>();
    
    public BasicScene() {
        index = count++;
    }

    public BasicScene(Vantage vantage) {
        add(vantage);
        if (vantage.getParent() == null)
            vantage.setParent(this);
        index = count++;
    }

    public int getIndex() {
        return index;
    }

    public Vantage getVantage() {
        return cameras.get(0);
    }

    @Override
    public Collection<? extends Light> getLights() {
        return lights;
    }

    @Override
    public Collection<? extends Vantage> getCameras() {
        return cameras;
    }

    @Override
    public Scene add(Light light) {
        lights.add(light);
        return this;
    }

    @Override
    public Scene add(Vantage camera) {
        cameras.add(camera);
        return this;
    }

    @Override
    public Object3D addChild(Object3D child) {
        object3d.addChild(child);
        return this;
    }

    @Override
    public Object3D getParent() {
        return object3d.getParent();
    }

    @Override
    public Collection<? extends Object3D> getChildren() {
        return object3d.getChildren();
    }

    @Override
    public Matrix4 getTransformInWorld() {
        return object3d.getTransformInWorld();
    }

    @Override
    public Matrix4 getTransformInParent() {
        return object3d.getTransformInParent();
    }

    @Override
    public boolean isVisible() {
        return object3d.isVisible();
    }

    @Override
    public Object3D setVisible(boolean isVisible) {
        object3d.setVisible(isVisible);
        return this;
    }

    @Override
    public String getName() {
        return object3d.getName();
    }

    @Override
    public Object3D setName(String name) {
        object3d.setName(name);
        return this;
    }

    @Override
    public Object3D setParent(Object3D parent) {
        object3d.setParent(parent);
        return this;
    }
}
