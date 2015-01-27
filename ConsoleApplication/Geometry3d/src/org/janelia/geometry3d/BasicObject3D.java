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
package org.janelia.geometry3d;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicObject3D implements Object3D {
    private Object3D parent;
    private final List<Object3D> children;
    private final Matrix4 transformInWorld;
    private final Matrix4 transformInParent;
    private boolean isVisible = true;
    private String name = "Object3D";
    
    public BasicObject3D(Object3D parent) {
        this.parent = parent;
        children = new LinkedList<Object3D>();
        transformInWorld = new Matrix4();
        transformInParent = new Matrix4();
    }
    
    @Override
    public Object3D addChild(Object3D child) {
        children.add(child);
        return this;
    }

    @Override
    public Object3D getParent() {
        return parent;
    }

    @Override
    public Collection<? extends Object3D> getChildren() {
        return children;
    }

    @Override
    public Matrix4 getTransformInWorld() {
        return transformInWorld;
    }

    @Override
    public Matrix4 getTransformInParent() {
        return transformInParent;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public Object3D setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        return this;
    }

    @Override
    public Object3D setName(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object3D setParent(Object3D parent) {
        this.parent = parent;
        return this;
    }

}
