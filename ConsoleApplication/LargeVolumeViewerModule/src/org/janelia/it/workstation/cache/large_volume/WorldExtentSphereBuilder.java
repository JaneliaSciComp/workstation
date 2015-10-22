/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.OctreeMetadataSniffer;
import org.janelia.it.workstation.gui.large_volume_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.large_volume_viewer.TileBoundingBox;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.ViewBoundingBox;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModelBean;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;                
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This neighborhood builder, delineates the environ files by testing their
 * micron distance from the focus.  Nearby ones are included.
 * 
 * @author fosterl
 */
public class WorldExtentSphereBuilder extends GeometricNeighborhoodBuilder {

    private static Logger log = LoggerFactory.getLogger(WorldExtentSphereBuilder.class);
    private double radiusInMicrons;

    /**
     * Construct with all info require to describe a sphere.  That sphere will
     * have a radius of at least that provided.  However, its extent in all
     * directions will be at the resolution of the image stacks that make up
     * the repository.
     * 
     * @param topFolderURL base point for all files in repo.
     * @param radius lower-bound micrometers to extend.
     * @param cacheManager for lookups of pre-existing data.
     */
    public WorldExtentSphereBuilder(SharedVolumeImage sharedVolumeImage, URL topFolderURL, double radius, CacheFacadeI cacheManager) throws URISyntaxException {
        setTileFormatSource( new SVITileFormatSource(sharedVolumeImage));
        
        this.radiusInMicrons = radius;
        this.topFolder = new File(topFolderURL.toURI());
        this.cacheManager = cacheManager;
    }
    
    /**
     * Simplistic constructor, helpful for testing.
     * 
     * @param tileFormat
     * @param topFolder
     * @param radius 
     */
    public WorldExtentSphereBuilder(final TileFormat tileFormat, File topFolder, double radius) {
        setTileFormatSource( new TileFormatSource() {
            public TileFormat getTileFormat() {
                return tileFormat;
            }
        });
        this.radiusInMicrons = radius;
        this.topFolder = topFolder;
    }
    


	/**
     * Wish to produce the list of all files needed to fill in the
     * spherical neighborhood centered at the focus.
     * 
	 */
    @Override
    public GeometricNeighborhood buildNeighborhood(double[] focus, Double zoom, double pixelsPerSceneUnit) {
        double[] newFnZ = new double[4];
        newFnZ[0] = focus[0];
        newFnZ[1] = focus[1];
        newFnZ[2] = focus[2];
        newFnZ[3] = zoom;
        if (focusPlusZoom == null) {
            focusPlusZoom = newFnZ;
        } else {
            boolean sameAsBefore = true;
            for (int i = 0; i < 4; i++) {
                if (focusPlusZoom[i] != newFnZ[i]) {
                    sameAsBefore = false;
                }
            }
            if (sameAsBefore) {
                log.trace("Bailing...same as before.");
                return null;
            } else {
                focusPlusZoom = newFnZ;
            }
        }

        log.trace("Building neighborhood at zoom {}, focus {},{},{}", zoom, focus[0], focus[1], focus[2] );
        WorldExtentSphere neighborhood = new WorldExtentSphere();
        neighborhood.setFocus(focus);
        neighborhood.setZoom(zoom);
        
        TileFormat tileFormat = tileFormatSource.getTileFormat();
        if (tileHalfSize == null) {
            tileHalfSize = getTileHalfSize(tileFormat);
        }
        // In order to find neighborhood, must figure out what is the center
        // tile, and must find all additional tiles, out to a certain point.
        // Will wish to ensure that a proper distance-from-focus has been
        // calculated, so that ordering / priority is given to near tiles.        
        Vec3 center = new Vec3(focus[0], focus[1], focus[2]);
        dimensions = new int[]{(int)radiusInMicrons,(int)radiusInMicrons,(int)radiusInMicrons};
        log.debug("Dimensions in voxels are: {},{},{}.", dimensions[0], dimensions[1], dimensions[2]);
        // NOTE: when dumped, this looks like voxels, even though all the
        // classes/members in play are stated as microns.
        log.debug("Voxel volume in cache extends from\n\t{},{},{}\nto\n\t{},{},{}\nin voxels.",
                 center.getX() - radiusInMicrons, center.getY() - radiusInMicrons, center.getZ() - radiusInMicrons,
                 center.getX() + radiusInMicrons, center.getY() + radiusInMicrons, center.getZ() + radiusInMicrons
        );
                
        Map<String,double[]> fileToCenter = new HashMap<>();
        Map<String,int[]> fileToTileXyz = new HashMap<>();

        // Establish a collection with required order and guaranteed uniqueness.
        FocusProximityComparator comparator = new FocusProximityComparator();
        String tiffBase = OctreeMetadataSniffer.getTiffBase(CoordinateAxis.Z);
        Set<String> tileFilePaths = new HashSet<>();
        for (TileIndex tileIndex: getCenteredTileSet(tileFormat, center, pixelsPerSceneUnit, dimensions, zoom)) {
            File tilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, true);  
            if (tilePath == null) {
                log.warn("Null octree file path for {}.", tileIndex);
            }
            else {
                tilePath = new File(topFolder, tilePath.toString());
                double[] tileCenter = findTileCenterInMicrons(tileFormat, tileIndex);
                fileToCenter.put(tilePath.toString(), tileCenter);

                log.info("for tile="+tilePath.toString()+" TileIndex x="+tileIndex.getX()+" y="+tileIndex.getY()+" z="+tileIndex.getZ());

                fileToTileXyz.put(
                        tilePath.toString(),
                        new int[]{tileIndex.getX(), tileIndex.getY(), tileIndex.getZ()}
                );
                double sigmaSquare = 0.0;
                for (int i = 0; i < 3; i++) {
                    double absDist = Math.abs(tileCenter[i] - focus[i]);
                    sigmaSquare += absDist;
                }
                double distanceFromFocus = Math.sqrt(sigmaSquare);
                for (int channel = 0; channel < tileFormat.getChannelCount(); channel++) {
                    String fileName = OctreeMetadataSniffer.getFilenameForChannel(tiffBase, channel);
                    File tileFile = new File(tilePath, fileName);
                    // Work out what needs to be uncompressed, here.
                    if (namer == null) {
                        namer = resolver.getNamer(tileFile);
                    }
                    tileFile = namer.getCompressedName(tileFile);
                    String fullTilePath = tileFile.getAbsolutePath();
                    // With the comparator in use, this test is necessary.
                    if (!tileFilePaths.contains(fullTilePath)) {
                        comparator.addFile(tileFile, distanceFromFocus);
                        tileFilePaths.add(fullTilePath);
                        log.trace("Adding file {} to neighborhood {}.", fullTilePath, neighborhood.getId());
                    }
                }
            }
        }
        Set<File> tileFiles = new TreeSet<>(comparator);
        for ( String tileFilePath: tileFilePaths) {
            tileFiles.add(new File(tileFilePath));
        }
        neighborhood.setFiles(Collections.synchronizedSet(tileFiles));
        log.debug("Neighborhood contains {} files.", tileFiles.size());

        int[] minTiles = new int[]{
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        };
        int[] maxTiles = new int[]{
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,};

        Map<String,PositionalStatusModel> models = buildPositionalModels(fileToCenter, fileToTileXyz, minTiles, maxTiles);
        neighborhood.setPositionalModels(models);
        neighborhood.setTileExtents(minTiles, maxTiles);
        
        if (listener != null) {
            listener.created(neighborhood);
        }
        return neighborhood;
    }



}
