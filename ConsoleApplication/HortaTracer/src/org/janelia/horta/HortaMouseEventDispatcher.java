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

package org.janelia.horta;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.MouseInputListener;

/**
 * High-level mouse event dispatcher, so drag-vertex from TracingInteractor
 * can definitely override drag-world in OrbitPanZoomInteractor
 * @author brunsc
 */
public class HortaMouseEventDispatcher 
implements MouseInputListener
{
    private final List<MouseInputListener> listeners = new ArrayList<>();
    
    public HortaMouseEventDispatcher(
            MouseInputListener TracingInteractor,
            MouseInputListener WorldInteractor,
            MouseInputListener HortaInteractor)
    {
        // Use explicit ordering of event dispatch
        listeners.add(TracingInteractor);
        listeners.add(WorldInteractor);
        listeners.add(HortaInteractor);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseClicked(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mousePressed(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseReleased(event);
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseEntered(event);
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseExited(event);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseDragged(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseMoved(event);
        }
    }
}
