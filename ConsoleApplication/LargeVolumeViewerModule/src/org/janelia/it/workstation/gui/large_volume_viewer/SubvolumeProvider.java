package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

/**
 * within the large volume viewer, this interface enables our bad habits; the data
 * is owned by QuadViewUi in a not very abstract way; this interface lets us
 * pass the data to algorithms that need it more safely
 */
public class SubvolumeProvider {

    private SharedVolumeImage volumeImage;
    private TileServer tileServer;

    SubvolumeProvider(SharedVolumeImage volumeImage, TileServer tileServer) {
        this.volumeImage = volumeImage;
        this.tileServer = tileServer;
    }


    /**
     * this method returns a read-only subvolume of data (maximum zoom,
     * intended for calculations); we have one that takes Vec3 input
     * (doubles) and launders the coordinates through the internal units,
     * and one that takes coordinates with that already done
     */
    public Subvolume getSubvolume(Vec3 corner1, Vec3 corner2) {
        // all this coordinate stuff is taken from something Christopher
        //  wrote in support of the path tracing code he wrote

        // A series of conversions to get to ZoomedVoxelIndex
        TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
        TileFormat.MicrometerXyz um1 = new TileFormat.MicrometerXyz(corner1.getX(), corner1.getY(), corner1.getZ());
        TileFormat.MicrometerXyz um2 = new TileFormat.MicrometerXyz(corner2.getX(), corner2.getY(), corner2.getZ());
        TileFormat.VoxelXyz vox1 = tileFormat.voxelXyzForMicrometerXyz(um1);
        TileFormat.VoxelXyz vox2 = tileFormat.voxelXyzForMicrometerXyz(um2);
        ZoomLevel zoomLevel = new ZoomLevel(0);
        ZoomedVoxelIndex zv1 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox1, zoomLevel, CoordinateAxis.Z);
        ZoomedVoxelIndex zv2 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox2, zoomLevel, CoordinateAxis.Z);

        return getSubvolume(zv1, zv2);
    }
    
    /**
     * this method returns a read-only subvolume of data (at zoom).
     * @See getVolume(Vec3 corner1, Vec3 corner2)
     * @deprecated See no-corners overload
     */
    public Subvolume getSubvolume(Vec3 corner1, Vec3 corner2, int zoomIndex, IndeterminateNoteProgressMonitor monitor) {
        // all this coordinate stuff is taken from something Christopher
        //  wrote in support of the path tracing code he wrote

        // A series of conversions to get to ZoomedVoxelIndex
        TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
        TileFormat.MicrometerXyz um1 = new TileFormat.MicrometerXyz(corner1.getX(), corner1.getY(), corner1.getZ());
        TileFormat.MicrometerXyz um2 = new TileFormat.MicrometerXyz(corner2.getX(), corner2.getY(), corner2.getZ());
        TileFormat.VoxelXyz vox1 = tileFormat.voxelXyzForMicrometerXyz(um1);
        TileFormat.VoxelXyz vox2 = tileFormat.voxelXyzForMicrometerXyz(um2);

        ZoomLevel zoomLevel = new ZoomLevel(zoomIndex);
        ZoomedVoxelIndex zv1 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox1, zoomLevel, CoordinateAxis.Z);
        ZoomedVoxelIndex zv2 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox2, zoomLevel, CoordinateAxis.Z);

        ZoomedVoxelIndex zvFull1 = null;
        ZoomedVoxelIndex zvFull2 = null;
        
        // For this override, will back-apply the volume size calculation, to
        // re-expand the coordinates to original sizes.
        if ( zoomIndex == 0 ) {
            zvFull1 = zv1;
            zvFull2 = zv2;
        }
        else {
            //  First, calculate the center point.
            Vec3 zoomedCenterPoint = new Vec3(Math.abs(zv1.getX() + zv2.getX()) / 2.0, Math.abs(zv1.getY() + zv2.getY()) / 2.0, Math.abs(zv1.getZ() + zv2.getZ()) / 2.0);
            //  Next, make new starts/ends.
            double[] originalSizing = new double[]{
                Math.abs(corner1.getX() - corner2.getX()) + 1.0,
                Math.abs(corner1.getY() - corner2.getY()) + 1.0,
                Math.abs(corner1.getZ() - corner2.getZ()) + 1.0
            };

            zvFull1 = new ZoomedVoxelIndex(
                    zoomLevel,
                    (int) findLowerBound(zoomedCenterPoint.getX(), (int) originalSizing[0]),
                    (int) findLowerBound(zoomedCenterPoint.getY(), (int) originalSizing[1]),
                    (int) findLowerBound(zoomedCenterPoint.getZ(), (int) originalSizing[2])
            );

            zvFull2 = new ZoomedVoxelIndex(
                    zoomLevel,
                    (int) findUpperBound(zvFull1.getX(), (int) originalSizing[0]),
                    (int) findUpperBound(zvFull1.getY(), (int) originalSizing[1]),
                    (int) findUpperBound(zvFull1.getZ(), (int) originalSizing[2])
            );

        }
        
        return new Subvolume(zvFull1, zvFull2, volumeImage, tileServer.getTextureCache(), monitor);
    }

    /**
     * This method returns a read-only subvolume of data (at zoom), centered
     * at the center coords, and extending around the extent dimension,
     * symetrically in all directions.
     * 
     * @param center volume's center is here.
     * @param extent x, y, z overal length.
     * @param zoomIndex zoomed in this much.
     * @param monitor feeds back progress.
     * @return R/O sub volume centered at center, extending extent-sub-i div by 2 all directions.
     */
    public Subvolume getSubvolume(Vec3 center, int[] extent, int zoomIndex, IndeterminateNoteProgressMonitor monitor) {
        // all this coordinate stuff is taken from something Christopher
        //  wrote in support of the path tracing code he wrote

        // A series of conversions to get to ZoomedVoxelIndex
        TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
        TileFormat.MicrometerXyz um = new TileFormat.MicrometerXyz(center.getX(), center.getY(), center.getZ());
        TileFormat.VoxelXyz vox = tileFormat.voxelXyzForMicrometerXyz(um);

        ZoomLevel zoomLevel = new ZoomLevel(zoomIndex);
        ZoomedVoxelIndex zv = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vox, zoomLevel, CoordinateAxis.Z);

        return new Subvolume(zv, volumeImage, extent, tileServer.getTextureCache(), monitor);
    }

    public Subvolume getSubvolume(ZoomedVoxelIndex zv1, ZoomedVoxelIndex zv2) {
        return new Subvolume(zv1, zv2, volumeImage, tileServer.getTextureCache(), null);
    }
    
    public static double findLowerBound( double centerCoord, int desiredDimension ) {
        return Math.max( 0.0, centerCoord - (double)(desiredDimension / 2));        
    }
    
    public static double findUpperBound( double lowerCoord, int desiredDimension ) {
        return lowerCoord + desiredDimension - 1;
    }
}
