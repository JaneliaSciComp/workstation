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

package org.janelia.horta.volume;

import Jama.Matrix;
import java.io.IOException;
import java.util.List;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;

/**
 *
 * @author Christopher Bruns
 */
public interface BrickInfo {
    List<? extends ConstVector3> getCornerLocations(); // XYZ at extreme corners of raster volume
    List<? extends ConstVector3> getValidCornerLocations(); // XYZ at corners of non-zero subvolume, e.g. to ignore openGL padding to 4-byte boundary
    List<? extends ConstVector3> getTilingSubsetLocations(); // XYZ at corners of minimal tiling subvolume, e.g. to ignore adjacent tile overlap regions
    VoxelIndex getRasterDimensions(); // Length of principal edges, in voxels
    int getChannelCount(); // Number of color channels
    int getBytesPerIntensity(); // Number of bytes per intensity
    double getResolutionMicrometers(); // Finest resolution
    public Box3 getBoundingBox();
    Texture3d loadBrick(double maxEdgePadWidth) throws IOException;

    // Matrix that transforms local texture coordinates to world units (e.g. stage micrometers)
    Matrix getTexCoord_X_stageUm();
}
