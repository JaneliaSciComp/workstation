/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public interface SpecimenCamera
{
    // Read-only methods in upper section - so slave clients can follow the camera position

    URL getDataPath(); // location of folder containing image data (both octree and raw-tile-yml database)
    // camera focus location
    float getFocusXUm(); // camera focus X position, in micrometers
    float getFocusYUm();
    float getFocusZUm();
    // read-only-ish Observable API, for handling camera changes
    void addObserver(Observer observer); // (member of Observable class)
    void deleteObserver(Observer observer); // (member of Observable class)

    // Mutable methods below - so peer modules can change the camera position

    // Camera focus position
    void setFocusUm(float x, float y, float z); // in micrometer stage coordinates
    // mutable Observable API
    // notifyObservers() allows fine grained control, so a large number of changes could be
    // built up, before (possibly expensively) releasing the horses.
    // This mechanism will obviously be more important for things like neuron structure changes.
    void notifyObservers(); // trigger consequences of camera change, if something did change (member of Observable class)
}
