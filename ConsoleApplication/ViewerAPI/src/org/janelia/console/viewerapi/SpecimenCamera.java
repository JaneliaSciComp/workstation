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


package org.janelia.console.viewerapi;

import java.net.URL;
import java.util.Observer;

/**
 * interface SpecimenCamera
 * @author Christopher Bruns
 * Proof-of-concept initial Janelia Workstation API for communicating between
 *  Large Volume Viewer and Horta.
 * To minimize dependencies, I aim to use only bog-standard Java types, where possible.
 * This initial version communicates only data folder, and camera center location.
 * Future versions need to also communicate:
 *         * neuron/annotation structures
 *         * color/brightness settings
 *         * more camera parameters, such as rotation and zoom
 */
public interface SpecimenCamera extends CameraModel
{
    // Read-only methods in upper section - so slave clients can follow the camera position

    URL getDataPath(); // location of folder containing image data (both octree and raw-tile-yml database)

    // read-only-ish Observable API, for handling camera changes
    void addObserver(Observer observer); // (member of Observable class)

    void deleteObserver(Observer observer); // (member of Observable class)

    // mutable Observable API
    // notifyObservers() allows fine grained control, so a large number of changes could be
    // built up, before (possibly expensively) releasing the horses.
    // This mechanism will obviously be more important for things like neuron structure changes.
    void notifyObservers(); // trigger consequences of camera change, if something did change (member of Observable class)
}
