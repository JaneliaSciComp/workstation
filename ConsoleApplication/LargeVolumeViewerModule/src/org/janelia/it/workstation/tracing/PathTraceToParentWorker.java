package org.janelia.it.workstation.tracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.Subvolume;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.octree.ZoomLevel;
import org.janelia.it.jacs.shared.octree.ZoomedVoxelIndex;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;

import java.util.List;
import java.util.Vector;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.PathTraceListener;

/**
 * this worker traces a detailed path given a request; adapted from PathTraceWorker,
 * which didn't use the workstation's built-in workers
 *
 * djo, 1/14
 */
public class PathTraceToParentWorker extends BackgroundWorker {

    private PathTraceToParentRequest request;
    private PathTraceListener pathTraceListener;

    // timeout in seconds
    private double timeout = 10.0;

    public PathTraceToParentWorker(PathTraceToParentRequest request) {
        this.request = request;
    }

    public PathTraceToParentWorker(PathTraceToParentRequest request, double timeout) {
        this(request);
        this.timeout = timeout;
    }

    public void setPathTraceListener(PathTraceListener l) {
        pathTraceListener = l;
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
        
        ZoomLevel zoomLevel = new ZoomLevel(0);
        Vec3 vec3_1 = request.getXyz1();
        ZoomedVoxelIndex zv1 = zoomedVoxelIndexForVoxelVec3(
                vec3_1, tileFormat, zoomLevel);

        Vec3 vec3_2 = request.getXyz2();
        ZoomedVoxelIndex zv2 = zoomedVoxelIndexForVoxelVec3(
                vec3_2, tileFormat, zoomLevel);

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
        astar.setVoxelSizes(tileFormat.getVoxelMicrometers());

        setStatus("Tracing");
        List<ZoomedVoxelIndex> path = astar.trace(zv1, zv2, timeout); // This is the slow part
        if (path == null) {
            // probably timed out; I don't see any other way it could fail
            // we don't do anything if we fail (would be nice to visually indicated it)
            setStatus("Timed out");
        } else {
            //DEBUG System.out.println("Original path length: " + path.size());
            final List<VoxelPosition> reducedPath = simplifyPath(path);
            if ( ! reducedPath.contains( zviToVoxel(path.get(0)) ) ) {
                reducedPath.add( 0, zviToVoxel(path.get(0)) ); 
            }
            if ( ! reducedPath.contains( zviToVoxel(path.get(path.size()-1)) ) ) {
                reducedPath.add( zviToVoxel(path.get(path.size()-1)) );
            }
            //DEBUG System.out.println("Simplified path length: " + reducedPath.size());
            List<Integer> intensities = new Vector<>();
            for (VoxelPosition p : reducedPath) {
                int intensity = subvolume.getIntensityGlobal(p, 0);
                intensities.add(intensity);
            }

            //DEBUG dumpFullAndSimplified( path, reducedPath );
            setStatus("Finishing");

            // launder the request down to a more generic request
            PathTraceRequest simpleRequest = new PathTraceRequest(request.getXyz1(),
                    request.getXyz2(), request.getAnchorGuid1(), request.getAnchorGuid2());
            TracedPathSegment result = new TracedPathSegment(simpleRequest, reducedPath, intensities);
            if (pathTraceListener != null) {
                pathTraceListener.pathTraced(result);
            }

            setStatus("Done");
        }
    }

    private ZoomedVoxelIndex zoomedVoxelIndexForVoxelVec3(Vec3 vec3, TileFormat tileFormat, ZoomLevel zoomLevel) {
        TileFormat.VoxelXyz vox = new TileFormat.VoxelXyz(vec3);
        ZoomedVoxelIndex zv = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox, zoomLevel, CoordinateAxis.Z);
        return zv;
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
    
    private List<VoxelPosition> simplifyPath(Collection<ZoomedVoxelIndex> path) {
        List<VoxelPosition> rtnVal = new ArrayList<>();
        
        // Doing checks: along same lines?
        Vec3 prevDirection = null;
        ZoomedVoxelIndex prevIndex = null;
        Iterator<ZoomedVoxelIndex> iter = path.iterator();
        for ( int i = 0; i < path.size(); i++ ) {
            ZoomedVoxelIndex index = iter.next();
            if ( prevIndex != null ) {
                Vec3 direction = calcDirection( prevIndex, index );
                if ( sameLine( prevDirection, direction ) ) {
                    rtnVal.remove(zviToVoxel(prevIndex));
                }
                prevDirection = direction;
            }
            rtnVal.add(zviToVoxel(index));
            prevIndex = index;
        }
        return rtnVal;
    }
    
    private VoxelPosition zviToVoxel( ZoomedVoxelIndex zvi ) {
        if (zvi.getZoomLevel().getZoomOutFactor() != 1) {
            throw new IllegalArgumentException("Cannot deal with non-closest-zoom.");
        }
        return new VoxelPosition(zvi.getX(), zvi.getY(), zvi.getZ());
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

    private Vec3 calcDirection(ZoomedVoxelIndex prev, ZoomedVoxelIndex curr) {
        double magnitude = 0;
        Vec3 prevVec = new Vec3( prev.getX(), prev.getY(), prev.getZ() );
        Vec3 currVec = new Vec3( curr.getX(), curr.getY(), curr.getZ() );
        // note: diff is in opposite direction of what you'd expect;
        //  not sure why the author chose this, but it's good enough for us
        Vec3 diff = prevVec.minus(currVec);
        magnitude = Math.sqrt(
                diff.getX() * diff.getX() + 
                diff.getY() * diff.getY() +
                diff.getZ() * diff.getZ() 
        );
        Vec3 diffNormalized = new Vec3(
                diff.getX() / magnitude,
                diff.getY() / magnitude,
                diff.getZ() / magnitude 
        );
        return diffNormalized;
    }

}
