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

import java.awt.Color;

/**
 * Quadrilateral mesh used to cover the whole screen.
 * Suitable for either per-vertex coloring, or texture mapping.
 * @author Christopher Bruns
 */
public class ScreenQuadMesh extends MeshGeometry 
{
    
    public ScreenQuadMesh() {
        initialize(Color.BLACK);
        notifyObservers();
    }

    public ScreenQuadMesh(Color color) {
        initialize(color);
        notifyObservers();
    }

    public ScreenQuadMesh(Color topColor, Color bottomColor) {
        initialize(topColor);
        // recolor bottom vertices
        for (int ix : new int[] {0, 1}) {
            Vertex vtx = vertices.get(ix);
            Vector4 col = (Vector4)vtx.getVectorAttribute("color");
            col.set(0, bottomColor.getRed()/255.0f);
            col.set(1, bottomColor.getGreen()/255.0f);
            col.set(2, bottomColor.getBlue()/255.0f);
            col.set(3, bottomColor.getAlpha()/255.0f);
        }
        notifyObservers();
    }

    private void initialize(Color color) {
        float red = color.getRed()/255.0f;
        float green = color.getGreen()/255.0f;
        float blue = color.getBlue()/255.0f;
        float alpha = color.getAlpha()/255.0f;
        
        // Use normalized device coordinates, so no matrix multiplies will be required in shader.
        Vertex v;
        v = addVertex(-1, -1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(0, 0));

        v = addVertex( 1, -1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(1, 0));
        
        v = addVertex( 1,  1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(1, 1));
        
        v = addVertex(-1,  1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(0, 1));
        
        addFace(new int[] {0, 1, 2, 3});
    }
    
    public void setColor(Color color) 
    {
        float red = color.getRed()/255.0f;
        float green = color.getGreen()/255.0f;
        float blue = color.getBlue()/255.0f;
        float alpha = color.getAlpha()/255.0f;
        for (Vertex v : vertices) {
            Vector4 c = (Vector4)v.getVectorAttribute("color");
            c.set(0, red);
            c.set(1, green);
            c.set(2, blue);
            c.set(3, alpha);
        }
        setChanged();
    }
}
