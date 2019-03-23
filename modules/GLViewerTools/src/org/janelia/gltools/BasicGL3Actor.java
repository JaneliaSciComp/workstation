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
package org.janelia.gltools;

import java.util.Collection;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BasicObject3D;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicGL3Actor implements GL3Actor {
    private final CompositeObject3d object3d;
    protected boolean isInitialized = false;
    
    public BasicGL3Actor(Object3d parent) {
        object3d = new BasicObject3D(parent);
    }

    @Override
    public Object3d addChild(Object3d child) {
        object3d.addChild(child);
        return this;
    }

    @Override
    public Object3d getParent() {
        return object3d.getParent();
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
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
    public Object3d setVisible(boolean isVisible) {
        object3d.setVisible(isVisible);
        return this;
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (! isVisible())
            return;
        if (! isInitialized) init(gl);
        if (getChildren() == null)
            return;
        if (getChildren().size() < 1)
            return;
        // Update modelview matrix
        // TODO - use cached version if nothing has changed
        Matrix4 modelViewMatrix = parentModelViewMatrix;
        if (modelViewMatrix == null)
            modelViewMatrix = camera.getViewMatrix();
        Matrix4 localMatrix = getTransformInParent();
        if (localMatrix != null)
            modelViewMatrix = new Matrix4(modelViewMatrix).multiply(localMatrix);
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor) {
                ((GL3Actor)child).display(gl, camera, modelViewMatrix);
            }
        }
    }

    @Override
    public void dispose(GL3 gl) {
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor)
                ((GL3Actor)child).dispose(gl);
        }
        isInitialized = false;
    }

    @Override
    public void init(GL3 gl) {
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor)
                ((GL3Actor)child).init(gl);
        }
        isInitialized = true;
    }

    @Override
    public String getName() {
        return object3d.getName();
    }

    @Override
    public Object3d setName(String name) {
        object3d.setName(name);
        return this;
    }

    @Override
    public Object3d setParent(Object3d parent) {
        object3d.setParent(parent);
        return this;
    }
    
}
