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

import org.janelia.geometry3d.Box3;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.OsFilePathRemapper;
import org.netbeans.api.progress.ProgressHandle;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christopher Bruns
 */
public class MouseLightYamlBrickSource 
implements StaticVolumeBrickSource
{
    private final Map<Double, BrickInfoSet> resMap = new HashMap<>();
    private final Box3 boundingBox = new Box3();
    
    public MouseLightYamlBrickSource(InputStream yamlStream, ProgressHandle progress) throws ParseException {
        progress.switchToDeterminate(100);
        progress.progress(5);
        Yaml yaml = new Yaml();
        progress.progress(10);
        Object foo = yaml.load(yamlStream);
        Map<String, Object> tilebase = (Map<String, Object>)foo;
        progress.progress(20);

        // Correct base path for OS network access, before populating tiles.
        String parentPath = (String) tilebase.get("path");
        parentPath = OsFilePathRemapper.remapLinuxPath(parentPath); // Convert to OS-specific file path

        // Error if folder STILL does not exist
        if (! new File(parentPath).exists()) {
            throw new RuntimeException("No such folder " + parentPath);
        }

        String tilebasePath = parentPath;
        
        progress.progress(25);
        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        int tileCount = 0;
        float totalTiles = tiles.size();
        for (Map<String, Object> tile : tiles) {
            String tilePath = (String) tile.get("path");
            
            BrickInfo tileInfo = new BrainTileInfo(tile, tilebasePath);

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
            
            tileCount += 1;
            progress.progress( (int)(25 + (90-25)*(tileCount/totalTiles)) );
        }
        progress.progress(90);
    }

    private Double getNearbyResolution(Double targetRes) {
        if (targetRes == null)
            return targetRes;
        if (resMap.containsKey(targetRes))
            return targetRes;
        for (Double oldRes : resMap.keySet() ) {
            if (oldRes == null)
                continue;
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
