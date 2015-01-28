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

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.horta.BrainTileInfo;
import org.janelia.geometry3d.Box3;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Christopher Bruns
 */
public class MouseLightYamlBrickSource 
implements StaticVolumeBrickSource
{
    private final String tilebasePath;
    private final Map<Double, BrickInfoSet> resMap = new HashMap<>();
    private final Box3 boundingBox = new Box3();
    
    public MouseLightYamlBrickSource(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> tilebase = (Map<String, Object>)yaml.load(yamlStream);

        // Correct base path for OS network access, before populating tiles.
        String parentPath = (String) tilebase.get("path");
        // Only munge the path if the stated path does not exist
        if (! new File(parentPath).exists()) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // parentPath = parentPath.replace("/tier2/mousebrainmicro/mousebrainmicro/", "X:/");
                parentPath = parentPath.replace("/nobackup/mousebrainmicro/", "\\\\fxt\\nobackup\\mousebrainmicro\\");
                parentPath = parentPath.replace("/groups/mousebrainmicro/mousebrainmicro/", "\\\\dm11\\mousebrainmicro\\");
                parentPath = parentPath.replace("/tier2/mousebrainmicro/mousebrainmicro/", "\\\\tier2\\mousebrainmicro\\mousebrainmicro\\");
            }
        }
        // Error if folder STILL does not exist
        if (! new File(parentPath).exists()) {
            throw new RuntimeException("No such folder " + parentPath);
        }
        
        tilebasePath = parentPath;
        
        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        for (Map<String, Object> tile : tiles) {
            String tilePath = (String) tile.get("path");
            
            BrickInfo tileInfo = new BrainTileInfo(tile, this.tilebasePath);

            // Update bounding box
            boundingBox.include(tileInfo.getBoundingBox());
            
            // Compute resolution
            Double resolution = tileInfo.getResolutionMicrometers();
            // KLUDGE: Treat resolutions within 30% as equivalent
            resolution = getNearbyResolution(resolution);
            
            for (Double oldRes : resMap.keySet() ) {
                double ratio = resolution / oldRes;
                final double threshold = 1.30;
                if ( (ratio > 1.0/threshold) && (ratio < threshold) )
                    resolution = oldRes;
            }
           
            if (! resMap.containsKey(resolution))
                resMap.put(resolution, new BrickInfoSet());
            BrickInfoSet brickSet = resMap.get(resolution);
            brickSet.add(tileInfo);
            // System.out.println(tilePath);
        }        
    }

    private Double getNearbyResolution(Double targetRes) {
        if (resMap.containsKey(targetRes))
            return targetRes;
        for (Double oldRes : resMap.keySet() ) {
            double ratio = targetRes / oldRes;
            final double threshold = 1.30;
            if ( (ratio > 1.0/threshold) && (ratio < threshold) )
                return oldRes;
        }
        return targetRes;
    }
    
    @Override
    public Collection<Double> getAvailableResolutions() {
        return resMap.keySet();
    }

    @Override
    public BrickInfoSet getAllBrickInfoForResolution(Double resolution) {
        Double r = getNearbyResolution(resolution);
        return resMap.get(r);
    }

    @Override
    public Box3 getBoundingBox() {
        return boundingBox;
    }
    
}
