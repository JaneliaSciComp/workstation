package org.janelia.horta.volume;

import org.janelia.geometry3d.Box3;
import org.janelia.horta.BrainTileInfo;
import org.janelia.console.viewerapi.OsFilePathRemapper;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
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
public class MouseLightYamlBrickSource implements StaticVolumeBrickSource {
    private final Map<Double, BrickInfoSet> resMap = new HashMap<>();
    private final Box3 boundingBox = new Box3();
    
    public MouseLightYamlBrickSource(InputStream yamlStream, boolean leverageCompressedFiles, ProgressHandle progress) throws ParseException {
        progress.switchToDeterminate(100);
        progress.progress(5);
        Yaml yaml = new Yaml();
        progress.progress(10);
        Object foo = yaml.load(yamlStream);
        Map<String, Object> tilebase = (Map<String, Object>)foo;
        progress.progress(20);

        // Correct base path for OS network access, before populating tiles; always do
        //  this in case user turns off http streaming for some reason
        String parentPath = (String) tilebase.get("path");
        parentPath = OsFilePathRemapper.remapLinuxPath(parentPath); // Convert to OS-specific file path
        System.out.println("MouseLightYamlBrickSource - modified parentPath="+parentPath);


        // Error if folder STILL does not exist
        if (!HttpDataSource.useHttp() && ! new File(parentPath).exists()) {
            throw new RuntimeException("No such folder " + parentPath);
        }

        String tilebasePath = parentPath;
        
        progress.progress(25);
        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        int tileCount = 0;
        float totalTiles = tiles.size();
        Double resolution = null;
        
        for (Map<String, Object> tile : tiles) {
            String tilePath = (String) tile.get("path");
            
            BrickInfo tileInfo = new BrainTileInfo(tile, tilebasePath, leverageCompressedFiles);

            // Update bounding box
            boundingBox.include(tileInfo.getBoundingBox());
            
            // Compute resolution
            // Jan 2017 CMB - Treat all tiles in YAML file as same resolution as the first tile.
            // (There is no dynamic loading by resolution at the moment for raw tiles in yaml file)
            if (resolution == null) {
                resolution = tileInfo.getResolutionMicrometers();
                resMap.put(resolution, new BrickInfoSet());
            }
            
            /*
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
            */
            
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

}
