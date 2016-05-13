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

import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.geometry3d.camera.ConstViewport;

/**
 * Rectangular viewing window region where scene rendering occurs
 * 
 * @author brunsc
 */
public class Viewport implements ViewSlab, ConstViewport
{
    private int originXPixels = 0;
    private int originYPixels = 0;
    private int widthPixels = 0;
    private int heightPixels = 0;
    private float zNearRelative = 0.5f;
    private float zFarRelative = 10.0f;
    private final ComposableObservable changeObservable = new ComposableObservable();

    public float getAspect() {
        if (heightPixels <= 0)
            return 1.0f;
        if (widthPixels <= 0)
            return 1.0f;
        return widthPixels/(float)heightPixels;
    }

    @Override
    public ComposableObservable getChangeObservable() {
        return changeObservable;
    }

    public int getOriginXPixels() {
        return originXPixels;
    }

    public int getOriginYPixels() {
        return originYPixels;
    }

    public void setOriginXPixels(int originXPixels) {
        if (originXPixels == this.originXPixels)
            return;
        this.originXPixels = originXPixels;
        changeObservable.setChanged();
    }

    public void setOriginYPixels(int originYPixels) {
        if (originYPixels == this.originYPixels)
            return;
        this.originYPixels = originYPixels;
        changeObservable.setChanged();
    }

    public int getWidthPixels() {
        return widthPixels;
    }

    public void setWidthPixels(int widthPixels) {
        if (widthPixels == this.widthPixels)
            return;
        this.widthPixels = widthPixels;
        changeObservable.setChanged();
    }

    public int getHeightPixels() {
        return heightPixels;
    }

    public void setHeightPixels(int heightPixels) {
        if (heightPixels == this.heightPixels)
            return;
        this.heightPixels = heightPixels;
        changeObservable.setChanged();
    }

    @Override
    public float getzNearRelative() {
        return zNearRelative;
    }

    @Override
    public void setzNearRelative(float zNearRelative) {
        if (zNearRelative == this.zNearRelative)
            return;
        this.zNearRelative = zNearRelative;
        changeObservable.setChanged();
    }

    @Override
    public float getzFarRelative() {
        return zFarRelative;
    }

    @Override
    public void setzFarRelative(float zFarRelative) {
        if (zFarRelative == this.zFarRelative)
            return;
        this.zFarRelative = zFarRelative;
        changeObservable.setChanged();
    }
    
}
