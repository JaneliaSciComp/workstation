package org.janelia.workstation.gui.large_volume_viewer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.gui.large_volume_viewer.controller.StatusUpdateListener;
import org.janelia.workstation.gui.viewer3d.interfaces.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ViewTileManager is a per-viewer implementation of tile management that used
 * to be in the (per-specimen) TileServer class.
 *
 * @author brunsc
 *
 */
public class ViewTileManager {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ViewTileManager.class);

    /**
     * @param loadStatusChangedListener the loadStatusChangedListener to set
     */
    void setLoadStatusChangedListener(StatusUpdateListener loadStatusChangedListener) {
        this.loadStatusChangedListener = loadStatusChangedListener;
    }

    /**
     * A TileSet is a group of rectangles that complete the LargeVolumeViewer
     * image display.
     *
     * Three TileSets are maintained: 1) Latest tiles : the tiles representing
     * the current view 2) LastGood tiles : the most recent tile set that could
     * be successfully displayed. 3) Emergency tiles : a tile set that is
     * updated with moderate frequency.
     *
     * We would always prefer to display the Latest tiles. But frequently the
     * image data for those tiles are not yet available. So we choose among the
     * three tile sets to give the best appearance of a responsive interface.
     *
     * The tricky part occurs when the user is rapidly changing the view, faster
     * than we can load the tile images. We load tile images in multiple
     * threads, but still it is not always possible to keep up. So one important
     * optimization is to first insert every desired tile image into the load
     * queue, but then when it is time to actually load an image, make another
     * check to ensure that the image is still desired. Otherwise the view can
     * fall farther and farther behind the current state.
     *
     * One approach is to display Latest tiles if they are ready, or the
     * LastGood tiles otherwise. The problem with this approach is that if the
     * user is rapidly changing the view, there is never time to fully update
     * the Latest tiles before they become stale. So the user just sees a static
     * ancient LastGood tile set. Precisely when the user most hopes to see
     * things moving fast. That is where 'emergency' tiles come in.
     *
     * Sets of emergency tiles are fully loaded as fast as possible, but no
     * faster. They are not dropped from the load queue, nor are they updated
     * until the previous set of emergency tiles has loaded and displayed.
     * During rapid user interaction, the use of emergency tiles allows the
     * scene to update in the fastest possible way, giving the comforting
     * impression of responsiveness.
     */
    public enum LoadStatus {
        NO_TEXTURES_LOADED,
        STALE_TEXTURES_LOADED,
        IMPERFECT_TEXTURES_LOADED,
        BEST_TEXTURES_LOADED,
    };

    private LoadStatus loadStatus = LoadStatus.NO_TEXTURES_LOADED;

    // Latest tiles list stores the current desired tile set, even if
    // not all of the tiles are ready.
    private TileSet latestTiles;
    // Emergency tiles list stores a recently displayed view, so that
    // SOMETHING gets displayed while the current view is being loaded.
    private TileSet emergencyTiles;
    // LastGoodTiles always hold a displayable tile set, even when emergency
    // tiles are loading.
    private TileSet lastGoodTiles;
    private final Set<TileIndex> neededTextures = new HashSet<>();
    private final Set<TileIndex> displayableTextures = new HashSet<>();

    private TileConsumer tileConsumer;
    private TextureCache textureCache;
    private SharedVolumeImage volumeImage;
    private StatusUpdateListener loadStatusChangedListener;

    ViewTileManager(TileConsumer tileConsumer) {
        this.tileConsumer = tileConsumer;
    }

    void textureLoaded(TileIndex index) {
        if (displayableTextures.contains(index)) {
            tileConsumer.repaint();
        }
    }

    public void clear() {
        emergencyTiles = null;
        if (latestTiles != null) {
            latestTiles.clear();
        }
        if (lastGoodTiles != null) {
            lastGoodTiles.clear();
        }
    }

    TileSet createLatestTiles() {
        return createLatestTiles(tileConsumer);
    }

    private TileSet createLatestTiles(TileConsumer tileConsumer) {
        return createLatestTiles(tileConsumer.getCamera(),
                tileConsumer.getViewport(),
                tileConsumer.getSliceAxis(),
                tileConsumer.getViewerInGround());
    }

    // June 20, 2013 Generalized for non-Z axes
    public TileSet createLatestTiles(Camera3d camera, Viewport viewport,
            CoordinateAxis sliceAxis, Rotation3d viewerInGround) {
        TileSet result = new TileSet();
        if (volumeImage.getLoadAdapter() == null) {
            return result;
        }
        if (!tileConsumer.isShowing()) // Hidden viewer shows no tiles.
        {
            return result;
        }

        // Need to loop over x and y
        // Need to compute z, and zoom
        // 1) zoom
        TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
        int zoom = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
        int zoomMax = tileFormat.getZoomLevelCount() - 1;

        int[] xyzFromWhd = {0, 1, 2};
        rearrangeFromRotationAxis(viewerInGround, xyzFromWhd);

        // 2) z or other slice axisIndex (d: depth)
        Vec3 focus = camera.getFocus();
        double fD = focus.get(xyzFromWhd[2]);
        // Correct for bottom Y origin of Raveler tile coordinate system
        // (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
        BoundingBox3d bb = volumeImage.getBoundingBox3d();
        if (xyzFromWhd[2] == 1) {
            double bottomY = bb.getMax().getY();
            fD = bottomY - fD - 0.5; // bounding box extends 0.5 voxels past final slice
        }
        int relativeTileDepth = tileFormat.calcRelativeTileDepth(xyzFromWhd, fD, bb);

        // 3) x and y tile index range
        ViewBoundingBox screenBounds = tileFormat.findViewBounds(
                (int) (viewport.getWidth() * 1.4),
                (int) (viewport.getHeight() * 1.4),
                focus,
                camera.getPixelsPerSceneUnit(),
                xyzFromWhd
        );
        TileBoundingBox tileBounds = tileFormat.viewBoundsToTileBounds(xyzFromWhd, screenBounds, zoom);

        TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
        // Must adjust the depth tile value relative to origin.
        for (int w = tileBounds.getwMin(); w <= tileBounds.getwMax(); ++w) {
            for (int h = tileBounds.gethMin(); h <= tileBounds.gethMax(); ++h) {
                int[] whd = {w, h, relativeTileDepth};
                TileIndex key = new TileIndex(
                        whd[xyzFromWhd[0]],
                        whd[xyzFromWhd[1]],
                        whd[xyzFromWhd[2]],
                        zoom,
                        zoomMax, indexStyle, sliceAxis);
                Tile2d tile = new Tile2d(key, tileFormat);
                tile.setYMax(bb.getMax().getY()); // To help flip y; Always actual Y! (right?)
                result.add(tile);
            }
        }
        return result;
    }

    TextureCache getTextureCache() {
        return textureCache;
    }

    LoadStatus getLoadStatus() {
        return loadStatus;
    }

    void setLoadStatus(LoadStatus loadStatus) {
        if (loadStatus == this.loadStatus) {
            return;
        }
        this.loadStatus = loadStatus;
        if (loadStatusChangedListener != null) {
            loadStatusChangedListener.update();
        }
    }

    void setTextureCache(TextureCache textureCache) {
        this.textureCache = textureCache;
    }

    public TileConsumer getTileConsumer() {
        return tileConsumer;
    }

    SharedVolumeImage getVolumeImage() {
        return volumeImage;
    }

    void setVolumeImage(SharedVolumeImage volumeImage) {
        this.volumeImage = volumeImage;
    }

    // Produce a list of renderable tiles to complete this view
    TileSet updateDisplayTiles() {
        // Update latest tile set
        latestTiles = createLatestTiles();
        latestTiles.assignTextures(textureCache);

        // Push latest textures to front of LRU cache
        int bestTexturesMovedToFrontOfQueue = latestTiles.stream()
                .map(tile -> tile.getBestTexture())
                .filter(texture -> texture != null)
                .map(texture -> textureCache.markHistorical(texture) ? 1 : 0)
                .reduce(0, (n1, n2) -> n1 + n2)
                ;

        // Need to assign textures to emergency tiles too...
        if (emergencyTiles != null) {
            emergencyTiles.assignTextures(textureCache);
        }

        // Maybe initialize emergency tiles
        if (emergencyTiles == null) {
            emergencyTiles = latestTiles;
        }
        if (emergencyTiles.size() < 1) {
            emergencyTiles = latestTiles;
        }

        // Which tile set will we display this time?
        TileSet result;
        if (latestTiles.canDisplay()) {
            if (latestTiles.getLoadStatus() == TileSet.LoadStatus.BEST_TEXTURES_LOADED) {
                setLoadStatus(LoadStatus.BEST_TEXTURES_LOADED);
            } else {
                setLoadStatus(LoadStatus.IMPERFECT_TEXTURES_LOADED);
            }
            // TODO - status
            emergencyTiles = latestTiles;
            lastGoodTiles = latestTiles;
            result = latestTiles;
        } else if (emergencyTiles.canDisplay()) {
            lastGoodTiles = emergencyTiles;
            result = emergencyTiles;
            // These emergency tiles will now be displayed.
            // So start a new batch of emergency tiles
            emergencyTiles = latestTiles;
            setLoadStatus(LoadStatus.STALE_TEXTURES_LOADED);
        } else {
            // Fall back to a known displayable
            result = lastGoodTiles;
            if (lastGoodTiles == null) {
                setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
            } else if (lastGoodTiles.canDisplay()) {
                setLoadStatus(LoadStatus.STALE_TEXTURES_LOADED);
            } else {
                setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
            }
        }

        // Keep working on loading both emergency and latest tiles only.
        Set<TileIndex> newNeededTextures = new LinkedHashSet<>();
        newNeededTextures.addAll(emergencyTiles.getFastNeededTextures());
        // Decide whether to load fastest textures or best textures
        Tile2d.LoadStatus stage = latestTiles.getMinStage();
        if (stage.ordinal() < Tile2d.LoadStatus.COARSE_TEXTURE_LOADED.ordinal()) {
            // First load the fast ones
            newNeededTextures.addAll(latestTiles.getFastNeededTextures());
        }
        // Then load the best ones
        newNeededTextures.addAll(latestTiles.getBestNeededTextures());
        Set<TileIndex> tilesToGet = Sets.difference(newNeededTextures, neededTextures);
        if (!tilesToGet.isEmpty()) {
            synchronized (neededTextures) {
                neededTextures.clear();
                neededTextures.addAll(tilesToGet);
            }
        } else if (getLoadStatus().ordinal() >= LoadStatus.IMPERFECT_TEXTURES_LOADED.ordinal() && bestTexturesMovedToFrontOfQueue == 0 ) {
            // since there are no new textures needed assume the best ones are loaded
            setLoadStatus(LoadStatus.BEST_TEXTURES_LOADED);
        }
        // Remember which textures might be useful
        // Even if it's LOADED, it might not be PAINTED yet.
        displayableTextures.clear();
        Stream.concat(latestTiles.stream(), emergencyTiles.stream())
                .forEach(tile -> {
                    // Best texture so far
                    if (tile.getBestTexture() != null) {
                        displayableTextures.add(tile.getBestTexture().getIndex());
                    }
                    // Best possible
                    displayableTextures.add(tile.getIndex());
                });
        return result;
    }

    TileSet getLatestTiles() {
        return latestTiles;
    }

    Collection<TileIndex> getNeededTextures() {
        // 3/23/2016 CMB JW-24845
        // Avoid (rare?) ConcurrentModificationException, by sending a Copy of the neededTextures
        Collection<TileIndex> result;
        synchronized (neededTextures) {
            result = ImmutableSet.copyOf(neededTextures);
        }
        return result;
    }

    private void rearrangeFromRotationAxis(Rotation3d viewerInGround, int[] xyzFromWhd) {
        // Rearrange from rotation matrix
        // Which axis (x,y,z) corresponds to width, height, and depth?
        for (int whd = 0; whd < 3; ++whd) {
            Vec3 vWhd = new Vec3(0, 0, 0);
            vWhd.set(whd, 1.0);
            Vec3 vXyz = viewerInGround.times(vWhd);
            double max = 0.0;
            for (int xyz = 0; xyz < 3; ++xyz) {
                double test = Math.abs(vXyz.get(xyz));
                if (test > max) {
                    xyzFromWhd[whd] = xyz;
                    max = test;
                }
            }
        }
    }
}
