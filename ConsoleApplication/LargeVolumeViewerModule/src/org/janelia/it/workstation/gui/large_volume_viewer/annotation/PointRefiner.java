package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.Subvolume;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class refines the position of a point based on some algorithm
 * using the underlying data
 *
 * for example, given a user's click at x, y, z, look at z +/- a
 * few planes and return the point with the highest intensity
 *
 * eventually this class may switch between several algorithms,
 * which may themselves be implemented in different classes,
 * which will need their own UIs to input parameters,
 * but to start, it's going to be monolithic and hard-coded
 *
 * djo, 7/14
 *
 */
public class PointRefiner {

    private static final Logger log = LoggerFactory.getLogger(PointRefiner.class);

    SubvolumeProvider dataProvider;

    PointRefiner(SubvolumeProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    Vec3 refine(Vec3 point) {
        return maxIntensityZ(point, 5);
    }

    /**
     * look zRange planes above and below the point (which we
     * round to nearest integer); look for z with greatest
     * intensity, where intensity is squared sum of values in
     * each channel
     */
    Vec3 maxIntensityZ(Vec3 point, int zRange) {
        // frankly, this routine is a mess; the coordinate
        //  transforms (int to float, voxel to voxel via
        //  zoom level and united coords) worry me

        // this is probably not the right thing to do?
        if (zRange < 1) {
            return null;
        }

        // note that working in ZoomedVoxelIndex means the
        //  subvolume can take care of the coordinate offset, since
        //  it knows its origin in the larger (full) volume

        // yes, this is a bit dangerous, but we really shouldn't be
        //  exceeding int for our coordinates!
        ZoomLevel zoomLevel = new ZoomLevel(0);
        ZoomedVoxelIndex roundedPoint = new ZoomedVoxelIndex(
                zoomLevel,
                (int) Math.round(point.getX()),
                (int) Math.round(point.getY()),
                (int) Math.round(point.getZ())
        );

        // I'm adding an x-y buffer for no particular reason; note the zmax+1
        //  upper bound; the volume doesn't hold data at its upper limit 
        //  (and returns zeroes there)
        int xc = roundedPoint.getX();
        int yc = roundedPoint.getY();
        int zmin = roundedPoint.getZ() - zRange;
        int zmax = roundedPoint.getZ() + zRange;
        ZoomedVoxelIndex corner1 = new ZoomedVoxelIndex(
                zoomLevel,
                xc - 10, yc - 10, zmin);
        ZoomedVoxelIndex corner2 = new ZoomedVoxelIndex(
                zoomLevel,
                xc + 10, yc + 10, zmax + 1);
        Subvolume volume = dataProvider.getSubvolume(corner1, corner2);

        long maxSqrIntensity = -1L;
        long currentIntensity;
        long sumIntensitySquared;
        ZoomedVoxelIndex currentZVI;
        Integer zMaxInt = null;
        double[] sisArr=new double[zmax-zmin+1];
        for (int z=zmin; z<=zmax; z++) {
            sumIntensitySquared = 0;
            currentZVI = new ZoomedVoxelIndex(zoomLevel, xc, yc, z);
            for (int c=0; c<volume.getChannelCount(); c++) {
                currentIntensity = volume.getIntensityGlobal(currentZVI, c);
                // System.out.println("z: " + z + "; channel " + c + " intensity: " + volume.getIntensityGlobal(currentZVI, c));
                sumIntensitySquared += currentIntensity * currentIntensity;
            }
            sisArr[z-zmin]=(double)sumIntensitySquared;
            // System.out.println("z: " + z + "; sum sqr intensity: " + sumIntensitySquared);
            if (sumIntensitySquared > maxSqrIntensity) {
                zMaxInt = z;
                maxSqrIntensity = sumIntensitySquared;
            }
        }

        // this procedure is problematic for noise; if you don't have signal,
        //  no reason to choose even the locally brightest pixel over the others
        // so, look at the statistics; if the coefficient of variation (std dev / mean)
        //  isn't over the threshold, consider the image too noisy to make a 
        //  choice, and go with the user's initial click regardless

        double sisVar=0.0;
        double sisAvg=0.0;
        for (double s : sisArr) {
            sisAvg+=s;
        }
        sisAvg = sisAvg / (double)sisArr.length;
        for (double s : sisArr) {
            sisVar += (s - sisAvg)*(s - sisAvg);
        }
        sisVar = sisVar / (double)sisArr.length;
        double sisDev = Math.sqrt(sisVar);

        // log.info("coeff of var = " + sisDev / sisAvg);

        // note the only coordinate we touch is z; x and y are
        //  taken from the user's click
        // this threshold chosen empirically; in a sampling of our data,
        //  noise tended to stay < 0.05, and signal tended to be > 0.2
        if (sisDev / sisAvg > 0.1) {
            return new Vec3(point.getX(), point.getY(), zMaxInt);
        } else {
            return point;
        }
    }
}
