/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.tracing.PathTraceToParentRequest;

/**
 * Implement this to hear about requests to trace the path.
 * @author fosterl
 */
public interface PathTraceRequestListener {
    void pathTrace(PathTraceToParentRequest request);
}
