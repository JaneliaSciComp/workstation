package org.janelia.it.FlyWorkstation.tracing;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Subvolume;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.octree.ZoomLevel;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.shared.workers.BackgroundWorker;
import org.janelia.it.FlyWorkstation.signal.Signal1;

import java.util.List;
import java.util.Vector;

/**
 * this worker traces a detailed path given a request; adapted from PathTraceWorker,
 * which didn't use the workstation's built-in workers
 *
 * djo, 1/14
 */
public class PathTraceToParentWorker extends BackgroundWorker {

    private PathTraceToParentRequest request;

    // public Signal1<TracedPathSegment> pathTracedSignal = new Signal1<TracedPathSegment>();
    public Signal1<AnchoredVoxelPath> pathTracedSignal = new Signal1<AnchoredVoxelPath>();

    public PathTraceToParentWorker(PathTraceToParentRequest request) {
        this.request = request;
    }

    public String getName() {
        return "trace path to parent of anchor";
    }

    @Override
    protected  void doStuff() throws Exception {
        // this part written by Christopher Bruns

        // note: can also use setProgress(long curr, long total)

        setStatus("Retrieving data");
        TileFormat tileFormat = request.getImageVolme().getLoadAdapter().getTileFormat();
        // A series of conversions to get to ZoomedVoxelIndex
        Vec3 vec3_1 = request.getXyz1();
        Vec3 vec3_2 = request.getXyz2();
        TileFormat.MicrometerXyz um1 = new TileFormat.MicrometerXyz(vec3_1.getX(), vec3_1.getY(), vec3_1.getZ());
        TileFormat.MicrometerXyz um2 = new TileFormat.MicrometerXyz(vec3_2.getX(), vec3_2.getY(), vec3_2.getZ());
        TileFormat.VoxelXyz vox1 = tileFormat.voxelXyzForMicrometerXyz(um1);
        TileFormat.VoxelXyz vox2 = tileFormat.voxelXyzForMicrometerXyz(um2);
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

        Subvolume subvolume = new Subvolume(v1pad, v2pad, request.getImageVolme(),
                request.getTextureCache());
        AStar astar = new AStar(subvolume);

        setStatus("Tracing");
        List<ZoomedVoxelIndex> path = astar.trace(zv1, zv2); // This is the slow part
        List<Integer> intensities = new Vector<Integer>();
        for (ZoomedVoxelIndex p : path) {
            int intensity = subvolume.getIntensityGlobal(p, 0);
            intensities.add(intensity);
        }

        setStatus("Finishing");

        // launder the request down to a more generic request
        PathTraceRequest simpleRequest = new PathTraceRequest(request.getXyz1(),
                request.getXyz2(), request.getAnchorGuid1(), request.getAnchorGuid2());
        TracedPathSegment result = new TracedPathSegment(simpleRequest, path, intensities);
        pathTracedSignal.emit(result);

        setStatus("Done");
    }

}
