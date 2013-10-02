package org.janelia.it.FlyWorkstation.tracing;

import java.util.List;
import java.util.Vector;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Subvolume;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TextureCache;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.MicrometerXyz;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.VoxelXyz;
import org.janelia.it.FlyWorkstation.octree.ZoomLevel;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.signal.Signal1;

public class PathTraceWorker implements Runnable 
{
    private TextureCache textureCache;
    private SharedVolumeImage volume;
    private PathTraceRequest request;
    
    public Signal1<TracedPathSegment> pathTracedSignal = new Signal1<TracedPathSegment>();
    
    public PathTraceWorker(
            TextureCache textureCache,
            SharedVolumeImage volume,
            PathTraceRequest request) 
    {
        this.textureCache = textureCache;
        this.volume = volume;
        this.request = request;
    }
    
    @Override
    public void run() {
        TileFormat tileFormat = volume.getLoadAdapter().getTileFormat();
        // A series of conversions to get to ZoomedVoxelIndex
        Vec3 vec3_1 = request.getXyz1();
        Vec3 vec3_2 = request.getXyz2();
        MicrometerXyz um1 = new MicrometerXyz(vec3_1.getX(), vec3_1.getY(), vec3_1.getZ());
        MicrometerXyz um2 = new MicrometerXyz(vec3_2.getX(), vec3_2.getY(), vec3_2.getZ());
        VoxelXyz vox1 = tileFormat.voxelXyzForMicrometerXyz(um1);
        VoxelXyz vox2 = tileFormat.voxelXyzForMicrometerXyz(um2);
        ZoomLevel zoomLevel = new ZoomLevel(0);
        ZoomedVoxelIndex zv1 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox1, zoomLevel, CoordinateAxis.Z);
        ZoomedVoxelIndex zv2 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox2, zoomLevel, CoordinateAxis.Z);
        // Create some padding around the neurite ends.
        final int padPixels = 10;
        ZoomedVoxelIndex v1pad = new ZoomedVoxelIndex(
                zv1.getZoomLevel(),
                Math.min(zv1.getX(), zv2.getX()) - padPixels, 
                Math.min(zv1.getY(), zv2.getY()) - padPixels, 
                Math.min(zv1.getZ(), zv2.getZ()) - padPixels);
        ZoomedVoxelIndex v2pad = new ZoomedVoxelIndex(
                zv2.getZoomLevel(),
                Math.max(zv1.getX(), zv2.getX()) + padPixels, 
                Math.max(zv1.getY(), zv2.getY()) + padPixels, 
                Math.max(zv1.getZ(), zv2.getZ()) + padPixels);
        //
        System.out.println("Loading subvolume...");
        Subvolume subvolume = new Subvolume(v1pad, v2pad, volume, textureCache);
        System.out.println("Finished loading subvolume.");
        // 
        System.out.println("Initializing A*...");
        AStar astar = new AStar(subvolume);
        System.out.println("Finished initializing A*.");
        System.out.println("Tracing path...");
        List<ZoomedVoxelIndex> path = astar.trace(zv1, zv2); // This is the slow part
        System.out.println("Finished tracing path.");
        System.out.println("Number of points in path = "+path.size());
        List<Integer> intensities = new Vector<Integer>();
        for (ZoomedVoxelIndex p : path) {
            int intensity = subvolume.getIntensityGlobal(p, 0);
            // System.out.println(p + ": " +intensity);
            intensities.add(intensity);
        }
        TracedPathSegment result = new TracedPathSegment(request, path, intensities);
        pathTracedSignal.emit(result);
    }

}
