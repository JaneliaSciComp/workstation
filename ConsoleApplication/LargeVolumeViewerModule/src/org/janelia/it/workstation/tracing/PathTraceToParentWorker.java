package org.janelia.it.workstation.tracing;

import java.util.Collection;
import java.util.HashSet;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.Subvolume;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.signal.Signal1;

import java.util.List;
import java.util.Vector;

/**
 * this worker traces a detailed path given a request; adapted from PathTraceWorker,
 * which didn't use the workstation's built-in workers
 *
 * djo, 1/14
 */
public class PathTraceToParentWorker extends BackgroundWorker {

    private static final double SECONDARY_SLOPE_TOLERANCE = 0.0;
    private PathTraceToParentRequest request;

    // timeout in seconds
    private double timeout = 10.0;

    // public Signal1<TracedPathSegment> pathTracedSignal = new Signal1<TracedPathSegment>();
    public Signal1<AnchoredVoxelPath> pathTracedSignal = new Signal1<>();

    public PathTraceToParentWorker(PathTraceToParentRequest request) {
        this.request = request;
    }

    public PathTraceToParentWorker(PathTraceToParentRequest request, double timeout) {
        this(request);
        this.timeout = timeout;
    }

    public String getName() {
        return "trace path to parent of anchor";
    }

    @Override
    protected void doStuff() throws Exception {
        // this part written by Christopher Bruns

        // note: can also use setProgress(long curr, long total)

        setStatus("Retrieving data");
        TileFormat tileFormat = request.getImageVolume().getLoadAdapter().getTileFormat();
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

        Subvolume subvolume = new Subvolume(v1pad, v2pad, request.getImageVolume(),
                request.getTextureCache());
        AStar astar = new AStar(subvolume);

        setStatus("Tracing");
        List<ZoomedVoxelIndex> path = astar.trace(zv1, zv2, timeout); // This is the slow part
        if (path == null) {
            // probably timed out; I don't see any other way it could fail
            // we don't do anything if we fail (would be nice to visually indicated it)
            setStatus("Timed out");
        } else {
            System.out.println("Original path length: " + path.size());
            Collection<ZoomedVoxelIndex> reducedPath = simplifyPath(path);
            System.out.println("Simplified path length: " + reducedPath.size());
            List<Integer> intensities = new Vector<>();
            for (ZoomedVoxelIndex p : reducedPath) {
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
    
    private Collection<ZoomedVoxelIndex> simplifyPath(Collection<ZoomedVoxelIndex> path) {
        Collection<ZoomedVoxelIndex> rtnVal = new HashSet<>();
        
        // Doing checks: along same lines?
        Vec3 prevNormal = null;
        Vec3 grandNormal = null;
        ZoomedVoxelIndex prevIndex = null;        
        for ( ZoomedVoxelIndex index: path ) {
            if ( prevIndex != null ) {
                Vec3 normal = calcNormal( prevIndex, index );
                if ( outOfTolerance( grandNormal, prevNormal, normal ) ) {
                    rtnVal.add(index);
                }
                grandNormal = prevNormal;
                prevNormal = normal;
            }
            prevIndex = index;
        }
        rtnVal.add(prevIndex); // Ensure that end point is in the return.
        
        return rtnVal;
    }
    
    private boolean outOfTolerance( Vec3 grandNormal, Vec3 prevNormal, Vec3 normal ) {
        boolean rtnVal = true;
        if (prevNormal == null) {
            rtnVal = true;
        }
        else if (prevNormal.equals(normal)) {
            rtnVal = false;
        }
        else if (grandNormal != null  &&  grandNormal.equals(normal)) {
            rtnVal = false;
        }
                
//        else if (grandNormal != null) {
//            double combinedVariance = Math.abs(grandNormal.getX() - normal.getX()) +
//                   Math.abs(grandNormal.getY() - normal.getY()) +
//                   Math.abs(grandNormal.getZ() - normal.getZ());
//            rtnVal = combinedVariance > SECONDARY_SLOPE_TOLERANCE;
//        }
        return rtnVal;
    }
    
    private Vec3 calcNormal(ZoomedVoxelIndex prev, ZoomedVoxelIndex curr) {
        double magnitude = 0;
        Vec3 prevVec = new Vec3( prev.getX(), prev.getY(), prev.getZ() );
        Vec3 currVec = new Vec3( curr.getX(), curr.getY(), curr.getZ() );
        Vec3 diff = prevVec.minus(currVec);
        magnitude = Math.sqrt(
                diff.getX() * diff.getX() + 
                diff.getY() * diff.getY() +
                diff.getZ() * diff.getZ() 
        );
        Vec3 diffNorm = new Vec3( 
                diff.getX() / magnitude,
                diff.getY() / magnitude,
                diff.getZ() / magnitude 
        );
        return diffNorm;
    }

}
