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

import org.janelia.geometry3d.AbstractCamera;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.SwingUtilities;

/**
 * Vantage manages a View matrix
 * @author brunsc
 */
public class OrbitPanZoomInteractor
extends SceneInteractor
implements MouseListener, MouseMotionListener, MouseWheelListener
{
    private Point previousPoint = null;
    
    public OrbitPanZoomInteractor(AbstractCamera camera, Component component) 
    {
        super(camera, component);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
    }

    @Override
    public String getToolTipText() {
        return ""
                + "Middle-drag to rotate, dude";
    } 

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            // double click to recenter
            if ( recenterOnMouse(event) )
                 notifyObservers();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        previousPoint = event.getPoint();
        // System.out.println("mouse pressed");
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        previousPoint = null;
        // System.out.println("mouse released");
    }

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}

    @Override
    public void mouseDragged(MouseEvent event) {
        boolean bChanged = false;
        if (previousPoint != null) {
            int dx = event.getPoint().x - previousPoint.x;
            int dy = event.getPoint().y - previousPoint.y;
            if ( (dx != 0) || (dy != 0) ) {
                // Left drag to pan
                if (SwingUtilities.isLeftMouseButton(event)) {
                    bChanged = panPixels(-dx, dy, 0);
                }
                // Middle drag to rotate
                else if (SwingUtilities.isMiddleMouseButton(event)) {
                    if (camera.getVantage().isConstrainedToUpDirection())
                        bChanged = orbitPixels(dx, -dy, 6.0f);
                    else 
                        bChanged = rotatePixels(dx, -dy, 6.0f);
                }
            }
        }
        if (bChanged) 
            notifyObservers();
        previousPoint = event.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        // System.out.println("Mouse wheel moved");
        if (zoomMouseWheel(event, 0.15f))
            notifyObservers();
    }

}
