package org.janelia.it.workstation.tracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

    @Override
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
        // NOTE: modified X-coordinate handling.
        // Other two coords first divide by microns, and then subtract origin.
        // Not so for the x: opposite order of operations: first subtracting
        // origin, and then dividing by micrometers.
        // (int)((camera.getFocus().getX() - tileFormat.getOrigin()[0]) / tileFormat.getVoxelMicrometers()[0]),
        // vox1 = new TileFormat.VoxelXyz( (int)((vox1.getX() - tileFormat.getOrigin()[0]) * tileFormat.getVoxelMicrometers()[0]), vox1.getY(), vox1.getZ());
        // vox2 = new TileFormat.VoxelXyz( (int)((vox2.getX() - tileFormat.getOrigin()[0]) * tileFormat.getVoxelMicrometers()[0]), vox2.getY(), vox2.getZ());
//        vox1 = new TileFormat.VoxelXyz( (int)(um1.getX() * tileFormat.getVoxelMicrometers()[0]), vox1.getY(), vox1.getZ());
//        vox2 = new TileFormat.VoxelXyz( (int)(um2.getX() * tileFormat.getVoxelMicrometers()[0]), vox2.getY(), vox2.getZ());
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
            //DEBUG System.out.println("Original path length: " + path.size());
            final List<ZoomedVoxelIndex> reducedPath = simplifyPath(path);
            if ( ! reducedPath.contains( path.get(0) ) ) {
                reducedPath.add( 0, path.get(0) ); 
            }
            if ( ! reducedPath.contains( path.get(path.size()-1) ) ) {
                reducedPath.add( path.get(path.size()-1) );
            }
            //DEBUG System.out.println("Simplified path length: " + reducedPath.size());
            List<Integer> intensities = new Vector<>();
            for (ZoomedVoxelIndex p : reducedPath) {
                int intensity = subvolume.getIntensityGlobal(p, 0);
                intensities.add(intensity);
            }

            //DEBUG dumpFullAndSimplified( path, reducedPath );
            setStatus("Finishing");

            // launder the request down to a more generic request
            PathTraceRequest simpleRequest = new PathTraceRequest(request.getXyz1(),
                    request.getXyz2(), request.getAnchorGuid1(), request.getAnchorGuid2());
            TracedPathSegment result = new TracedPathSegment(simpleRequest, reducedPath, intensities);
            // This is necessary, because starting locations need to be
            // single-stepped, and are based on the screen, with its 1x1x1
            // assumptions.  Best NOT to disturb AStar.
            result = refitResultToWorkspace(result);
            pathTracedSignal.emit(result);

            setStatus("Done");
        }
    }
    
    @SuppressWarnings("Unused")
    private void dumpFullAndSimplified( List<ZoomedVoxelIndex> path, List<ZoomedVoxelIndex> reducedPath ) {
        System.out.println("Full Path, prior to trimming:::");
        for ( ZoomedVoxelIndex inx: path ) {
            System.out.println( "\t" + inx.getX() + "," + inx.getY() + "," + inx.getZ() );
        }
        System.out.println("Post-trimming::");
        for ( ZoomedVoxelIndex inx: reducedPath ) {
            System.out.println( "\t" + inx.getX() + "," + inx.getY() + "," + inx.getZ() );
        }
    }
    
    private TracedPathSegment refitResultToWorkspace( TracedPathSegment segment ) {
        double xRes = request.getImageVolume().getXResolution();
        double yRes = request.getImageVolume().getYResolution();
        double zRes = request.getImageVolume().getZResolution();
        int[] origin = request.getImageVolume().getOrigin();
        List<ZoomedVoxelIndex> newPath = new ArrayList<>();
        for ( ZoomedVoxelIndex index: segment.getPath() ) {
            ZoomedVoxelIndex newIndex = new ZoomedVoxelIndex( 
                    index.getZoomLevel(),
                    origin[0] + (int)(index.getX() * xRes), 
                    origin[1] + (int)(index.getY() * yRes),
                    origin[2] + (int)(index.getZ() * zRes) 
            );
            newPath.add(newIndex);
        }
        TracedPathSegment newSegment = new TracedPathSegment(
                segment.getRequest(),
                newPath,
                segment.getIntensities()
        );
        return newSegment;
    }
    
    private List<ZoomedVoxelIndex> simplifyPath(Collection<ZoomedVoxelIndex> path) {
        List<ZoomedVoxelIndex> rtnVal = new ArrayList<>();
        
        // Doing checks: along same lines?
        Vec3 prevNormal = null;
        ZoomedVoxelIndex prevIndex = null;
        Iterator<ZoomedVoxelIndex> iter = path.iterator();
        for ( int i = 0; i < path.size(); i++ ) {
            ZoomedVoxelIndex index = iter.next();
            if ( prevIndex != null ) {
                Vec3 normal = calcNormal( prevIndex, index );
                if ( sameLine( prevNormal, normal ) ) {
                    rtnVal.remove(prevIndex);
                }
                prevNormal = normal;
            }
            rtnVal.add(index);            
            prevIndex = index;
        }
        rtnVal.add(prevIndex); // Ensure that end point is in the return.
        
        return rtnVal;
    }
    
    private boolean sameLine( Vec3 prevNormal, Vec3 normal ) {
        boolean rtnVal = false;
        if (prevNormal == null) {
            rtnVal = false;
        }
        else if (prevNormal.equals(normal)) {
            rtnVal = true;
        }
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
