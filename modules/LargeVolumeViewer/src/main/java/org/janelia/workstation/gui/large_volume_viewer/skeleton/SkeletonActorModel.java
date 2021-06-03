package org.janelia.workstation.gui.large_volume_viewer.skeleton;

import java.awt.Point;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.SpatialIndexManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.gui.large_volume_viewer.action.BasicMouseMode;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.workstation.gui.large_volume_viewer.tracing.SegmentIndex;
import org.janelia.workstation.controller.tileimagery.VoxelPosition;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Created by murphys on 4/14/2016.
 */
public class SkeletonActorModel {

    private static final Logger log = LoggerFactory.getLogger(SkeletonActorModel.class);

    // semantic constants for allocating byte arrays
    public static final int FLOAT_BYTE_COUNT = 4;
    public static final int VERTEX_FLOAT_COUNT = 3;
    public static final int INT_BYTE_COUNT = 4;
    public static final int COLOR_FLOAT_COUNT = 3;

    private int mostRecentAnchorVersion=-1;

    private boolean pointIndicesNeedCopy=true;
    private boolean verticesNeedCopy=true;

    private Camera3d camera;
    private Viewport viewport;
    private BasicMouseMode pointComputer;

    public Camera3d getCamera() {
        return camera;
    }

    public void setCamera(Camera3d camera) {
        this.camera = camera;
    }

    private boolean isFocusOnNextParent = false;

    private TileFormat tileFormat;

    private TileFormat getTileFormat() {
        return tileFormat;
    }

    private boolean anchorsVisible = true;

    private Skeleton skeleton;
    private SkeletonActorStateUpdater updater;

    ByteBuffer vertexByteBuffer;
    FloatBuffer vertexBuffer;

    ByteBuffer colorByteBuffer;
    FloatBuffer colorBuffer;

    ByteBuffer lineByteBuffer;
    IntBuffer lineBuffer;

    ByteBuffer pointByteBuffer;
    IntBuffer pointBuffer;

    // arrays for draw
    private Multiset<Long> neuronVertexCount = HashMultiset.create();
    private Map<Long, FloatBuffer> neuronVertices = new HashMap<>();
    private Map<Long, FloatBuffer> neuronColors = new HashMap<>();

    List<ElementDataOffset> vertexOffsets=new ArrayList<>();
    List<ElementDataOffset> colorOffsets=new ArrayList<>();
    List<ElementDataOffset> lineOffsets=new ArrayList<>();
    List<ElementDataOffset> pointOffsets=new ArrayList<>();

    Map<Long, ElementDataOffset> vertexOffsetMap=new HashMap<>();
    Map<Long, ElementDataOffset> colorOffsetMap=new HashMap<>();

    // Vertex buffer objects need indices
    private Map<Anchor, Integer> neuronAnchorIndices = new HashMap<>();
    private Map<Long, Map<Integer, Anchor>> neuronIndexAnchors = new HashMap<>();
    private Map<Long, IntBuffer> neuronPointIndices = new HashMap<>();
    private Map<Long, IntBuffer> neuronLineIndices = new HashMap<>();

    private Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments = new HashMap<>();

    public Map<Long, IntBuffer> getNeuronLineIndices() { return neuronLineIndices; }

    int cummulativeVertexOffset = 0;
    int cummulativeColorOffset = 0;
    int cummulativeLineOffset = 0;
    int cummulativePointOffset=0;

    private float zoomedZThicknessInPixels = 0f;

    public int getCummulativeVertexOffset() { return cummulativeVertexOffset; }
    public ByteBuffer getVertexBuffer() { return vertexByteBuffer; }
    public int getCummulativeColorOffset() { return cummulativeColorOffset; }
    public FloatBuffer getColorBuffer() { return colorBuffer; }
    public int getCummulativeLineOffset() { return cummulativeLineOffset; }
    public IntBuffer getLineBuffer() { return lineBuffer; }
    public List<ElementDataOffset> getLineOffsets() { return lineOffsets; }
    public List<ElementDataOffset> getVertexOffsets() { return vertexOffsets; }
    public List<ElementDataOffset> getColorOffsets() { return colorOffsets; }
    public int getCummulativePointOffset() { return cummulativePointOffset; }
    public IntBuffer getPointBuffer() { return pointBuffer; }
    public Map<Long, IntBuffer> getNeuronPointIndices() { return neuronPointIndices; }
    public List<ElementDataOffset> getPointOffsets() { return pointOffsets; }
    public Map<Long, ElementDataOffset> getVertexOffsetMap() { return vertexOffsetMap; }
    Map<Long, ElementDataOffset> getColorOffsetMap() { return colorOffsetMap; }
    public Multiset<Long> getNeuronVertexCount() { return neuronVertexCount; }
    public Map<Long, Map<SegmentIndex, TracedPathActor>> getNeuronTracedSegments() { return neuronTracedSegments; }

    public SkeletonActorModel() {
        this.updater = new SkeletonActorStateUpdater();
    }

    public Skeleton getSkeleton() { return skeleton; }

    public void setSkeleton(Skeleton skeleton) {
        if (skeleton == this.skeleton) {
            return;
        }
        this.skeleton = skeleton;
        updateAnchors();
    }

    public SkeletonActorStateUpdater getUpdater() {
        return updater;
    }

    public boolean isAnchorsVisible() {
        return anchorsVisible;
    }

    public void setAnchorsVisible(boolean anchorsVisible) {
        if (anchorsVisible == this.anchorsVisible) {
            return; // no change
        }
        this.anchorsVisible = anchorsVisible;
        updater.update();
    }

    public synchronized boolean updateVertices() {
        if (verticesNeedCopy) {
            log.trace("updateVertices - running");
            
            cummulativeVertexOffset=0;
            cummulativeColorOffset=0;
            cummulativeLineOffset=0;

            vertexOffsets.clear();
            colorOffsets.clear();
            lineOffsets.clear();

            vertexOffsetMap.clear();
            colorOffsetMap.clear();

            List<Long> neuronOrderList = new ArrayList<>();

            //log.info("displayLines2 Check1");

            for (Long neuronID : neuronVertices.keySet()) {
                if (TmModelManager.getInstance().getCurrentView().isHidden(neuronID)) {
                    continue;
                }

                neuronOrderList.add(neuronID);

                int vertexCount=neuronVertexCount.count(neuronID);

                int vertexBufferSize = vertexCount * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT;
                int colorBufferSize = vertexCount * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT;

                int lineBufferSize = 0;
                IntBuffer lineBuffer=neuronLineIndices.get(neuronID);
                if (lineBuffer!=null) {
                    lineBufferSize = lineBuffer.capacity() * INT_BYTE_COUNT;
                }

                ElementDataOffset vertexOffset = new ElementDataOffset(neuronID, vertexBufferSize, cummulativeVertexOffset);
                vertexOffsets.add(vertexOffset);
                vertexOffsetMap.put(neuronID, vertexOffset);

                ElementDataOffset colorOffset = new ElementDataOffset(neuronID, colorBufferSize, cummulativeColorOffset);
                colorOffsets.add(colorOffset);
                colorOffsetMap.put(neuronID, colorOffset);

                lineOffsets.add(new ElementDataOffset(neuronID, lineBufferSize, cummulativeLineOffset));

                cummulativeVertexOffset += vertexBufferSize;
                cummulativeColorOffset += colorBufferSize;
                cummulativeLineOffset += lineBufferSize;
            }

            if (vertexByteBuffer==null || vertexByteBuffer.limit()!=cummulativeVertexOffset) {
                vertexByteBuffer = ByteBuffer.allocateDirect(cummulativeVertexOffset);
                vertexBuffer = vertexByteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            int n = 0;
            for (Long neuronID : neuronOrderList) {
                ElementDataOffset elementDataOffset = vertexOffsets.get(n);
                FloatBuffer neuronVertexBuffer = neuronVertices.get(elementDataOffset.id);
                neuronVertexBuffer.rewind();
                vertexBuffer.put(neuronVertexBuffer);
                n++;
            }
            vertexBuffer.rewind();

            if (colorByteBuffer==null || colorByteBuffer.limit()!=cummulativeColorOffset) {
                colorByteBuffer = ByteBuffer.allocateDirect(cummulativeColorOffset);
                colorBuffer = colorByteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            n = 0;
            for (Long neuronID : neuronOrderList) {
                ElementDataOffset elementDataOffset = colorOffsets.get(n);
                FloatBuffer neuronColorBuffer = neuronColors.get(elementDataOffset.id);
                neuronColorBuffer.rewind();
                colorBuffer.put(neuronColorBuffer);
                n++;
            }
            colorBuffer.rewind();

            if (lineByteBuffer==null || lineByteBuffer.limit()!=cummulativeLineOffset) {
                lineByteBuffer = ByteBuffer.allocateDirect(cummulativeLineOffset);
                lineBuffer = lineByteBuffer.order(ByteOrder.nativeOrder()).asIntBuffer();
            }
            n = 0;
            for (Long neuronID : neuronOrderList) {
                ElementDataOffset elementDataOffset = lineOffsets.get(n);
                if (elementDataOffset.size != 0) {
                    IntBuffer neuronLineBuffer = neuronLineIndices.get(elementDataOffset.id);
                    lineBuffer.put(neuronLineBuffer);
                }
                n++;
            }
            lineBuffer.rewind();
            verticesNeedCopy=false;
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean updatePoints() {
        if (pointIndicesNeedCopy) {
            log.trace("updatePoints - running");

            pointOffsets.clear();

            cummulativePointOffset=0;
            List<Long> neuronOrderList=new ArrayList<>();

            for (Long neuronID : neuronVertices.keySet()) {
                if (TmModelManager.getInstance().getCurrentView().isHidden(neuronID)) {
                    continue;
                }
                neuronOrderList.add(neuronID);
                IntBuffer intBuffer = neuronPointIndices.get(neuronID);
                if (intBuffer==null) {
                    log.warn("Missing neuron {} from neuronPointIndices",neuronID);
                }
                else {
                    int pointBufferSize=intBuffer.capacity()*INT_BYTE_COUNT;
                    pointOffsets.add(new ElementDataOffset(neuronID, pointBufferSize, cummulativePointOffset));
                    cummulativePointOffset+=pointBufferSize;
                }
            }

            int n=0;
            if (pointByteBuffer==null || pointByteBuffer.limit()!=cummulativePointOffset) {
                pointByteBuffer = ByteBuffer.allocateDirect(cummulativePointOffset);
                pointBuffer = pointByteBuffer.order(ByteOrder.nativeOrder()).asIntBuffer();
            }
            for (Long neuronID : neuronOrderList) {
                ElementDataOffset elementDataOffset = pointOffsets.get(n);
                IntBuffer neuronPointBuffer=neuronPointIndices.get(elementDataOffset.id);
                neuronPointBuffer.rewind();
                pointBuffer.put(neuronPointBuffer);
                n++;
            }
            pointBuffer.rewind();
            pointIndicesNeedCopy=false;
            return true;
        } else {
            return false;
        }
    }

    public int getIndexForAnchor(Anchor anchor) {
        if (anchor == null) {
            return -1;
        }
        if (neuronAnchorIndices.containsKey(anchor)) {
            return neuronAnchorIndices.get(anchor);
        }
        return -1;
    }

    public synchronized void forceUpdateAnchors() {
        log.trace("forceUpdateAnchors; currently at version " + mostRecentAnchorVersion);
        mostRecentAnchorVersion--; // trick to trigger update
        updateAnchors();
    }
    
    /**
     * update the arrays we'll send to OpenGL; this includes the anchors/points
     * (thus the name of the method), the lines between them, and the
     * automatically traced paths if present
     *
     * the update in general consists of looping over all points, and copying
     * their positions into appropriate byte array, getting the data ready for
     * the next call to display()
     */
    public synchronized void updateAnchors() {
        if (skeleton == null) {
            return;
        }
        log.trace("updateAnchors - running");
        StopWatch w = new StopWatch();
        w.start();

        log.trace("skeleton has anchor set version = " + skeleton.getAnchorSetVersion());
        log.trace("mostRecentAnchorVerion = " + mostRecentAnchorVersion);
        if (mostRecentAnchorVersion == skeleton.getAnchorSetVersion()) {
            log.trace("updateAnchors() skipping redundant update");
            return;
        }
        else {
            mostRecentAnchorVersion = skeleton.getAnchorSetVersion();
            log.trace("updateAnchors() - updating to version = " + mostRecentAnchorVersion);
        }

        // we do the point update in this method, then call out
        //  to other methods for the lines and paths; no reason we
        //  couldn't also refactor this into its own method, too
        // clear out the maps first
        neuronVertexCount.clear();
        neuronVertices.clear();
        neuronColors.clear();
        // first, how many vertices per neuron; then, fill the buffers (one per neuron)
        Collection<Anchor> anchors = getAnchorsSafe();
        for (Anchor anchor : anchors) {
            neuronVertexCount.add(anchor.getNeuronID());
        }
        for (Long neuronID : neuronVertexCount.elementSet()) {
            int vertexCount=neuronVertexCount.count(neuronID);
            ByteBuffer tempBytes = ByteBuffer.allocateDirect(vertexCount * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronVertices.put(neuronID, tempBytes.asFloatBuffer());
            neuronVertices.get(neuronID).rewind();

            tempBytes = ByteBuffer.allocateDirect(vertexCount * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronColors.put(neuronID, tempBytes.asFloatBuffer());
            neuronColors.get(neuronID).rewind();
        }

        neuronAnchorIndices.clear();
        neuronIndexAnchors.clear();
        Map<Long, Integer> neuronVertexIndex = new HashMap<>();
        Integer currentVertexIndex;
        for (Anchor anchor : anchors) {
            Long neuronID = anchor.getNeuronID();

            Vec3 xyz = anchor.getLocation();
            FloatBuffer vertexBuffer=neuronVertices.get(neuronID);
            vertexBuffer.put((float) xyz.getX());
            vertexBuffer.put((float) xyz.getY());
            vertexBuffer.put((float) xyz.getZ());

            float[] styleColorArr= TmViewState.getColorForNeuronAsFloatArray(neuronID);
            FloatBuffer colorBuffer=neuronColors.get(neuronID);
            colorBuffer.put(styleColorArr);

            currentVertexIndex=neuronVertexIndex.get(neuronID);
            if (currentVertexIndex==null) {
                currentVertexIndex=0;
                neuronVertexIndex.put(neuronID, currentVertexIndex);
            }
            neuronAnchorIndices.put(anchor, currentVertexIndex);

            Map<Integer, Anchor> indexAnchorMap=neuronIndexAnchors.get(neuronID);
            if (indexAnchorMap==null) {
                indexAnchorMap=new HashMap<>();
                neuronIndexAnchors.put(neuronID, indexAnchorMap);
            }
            indexAnchorMap.put(currentVertexIndex, anchor);

            neuronVertexIndex.put(neuronID, currentVertexIndex + 1);
        }

        neuronPointIndices.clear();
        for (Long neuronID : neuronVertexIndex.keySet()) {
            // recall that the last value neuronVertexIndex takes is the
            //  number of points:
            ByteBuffer tempBytes = ByteBuffer.allocateDirect(neuronVertexIndex.get(neuronID) * INT_BYTE_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronPointIndices.put(neuronID, tempBytes.asIntBuffer());
            neuronPointIndices.get(neuronID).rewind();
        }
        for (Anchor anchor : anchors) {
            int i1 = neuronAnchorIndices.get(anchor);
            neuronPointIndices.get(anchor.getNeuronID()).put(i1);
        }

        pointIndicesNeedCopy=true;

        // automatically traced paths
        updateTracedPaths(anchors);

        // lines between points, if no path (must be done after path updates so
        //  we know where the paths are!)
        updateLines(anchors);

        boolean foundNextParent = false;
        Anchor nextParent = getNextParent();
        if (nextParent!=null) {
            for (Anchor anchor: anchors) {
                if (anchor.getGuid().equals(nextParent.getGuid())) {
                    foundNextParent = true;
                }
            }
        }
        if (!foundNextParent)
            setNextParent(null);
        
        w.start();
        log.trace("updateAnchors took {} ms",w.getElapsedTime());
        
        updater.update();
    }

    private void updateLines(Collection<Anchor> anchors) {
        // iterate through anchors and record lines where there are no traced
        //  paths; then copy the line indices you get into an array
        // note: I believe this works because we process the points and
        //  lines in exactly the same order (the order skeleton.getAnchors()
        //  returns them in)

        log.trace("updateLines - running");
        if (anchors==null) {
            anchors=getAnchorsSafe();
        }

        Map<Long, List<Integer>> tempLineIndices = new HashMap<>();
        for (Anchor anchor : anchors) {
            int i1 = getIndexForAnchor(anchor);
            if (i1 < 0) {
                continue;
            }
            for (Anchor neighbor : anchor.getNeighbors()) {
                Integer i2=neuronAnchorIndices.get(neighbor);
                if (i2==null) {
                    i2=-1;
                }
                if (i2 < 0) {
                    continue;
                }
                if (i1 >= i2) {
                    continue; // only use ascending pairs, for uniqueness
                }
                Long neuronID=anchor.getNeuronID();
                if (neuronTracedSegments.containsKey(neuronID)) {
                    SegmentIndex segmentIndex = new SegmentIndex(anchor.getGuid(), neighbor.getGuid());
                    // if neuron has any paths, check and don't draw line
                    //  where there's already a traced segment
                    if (neuronTracedSegments.get(neuronID).containsKey(segmentIndex)) {
                        continue;
                    }
                }
                if (!tempLineIndices.containsKey(neuronID)) {
                    tempLineIndices.put(neuronID, new Vector<Integer>());
                }
                tempLineIndices.get(neuronID).add(i1);
                tempLineIndices.get(neuronID).add(i2);
            }
        }

        // loop over neurons and fill the arrays
        neuronLineIndices.clear();
        for (Long neuronID : tempLineIndices.keySet()) {
            ByteBuffer lineBytes = ByteBuffer.allocateDirect(tempLineIndices.get(neuronID).size() * INT_BYTE_COUNT);
            lineBytes.order(ByteOrder.nativeOrder());
            IntBuffer lineIndexBuffer=lineBytes.asIntBuffer();
            neuronLineIndices.put(neuronID, lineIndexBuffer);
            lineIndexBuffer.rewind();
            for (int i : tempLineIndices.get(neuronID)) // fill actual int buffer
            {
                lineIndexBuffer.put(i);
            }
            lineIndexBuffer.rewind();
        }
        verticesNeedCopy=true;
    }

    private void updateTracedPaths(Collection<Anchor> anchors) {
        // Update Traced path actors

        log.trace("updateTracedPaths - running");
        // first, a short-circuit; if there are no anchors, the whole
        //  skeleton was cleared, and we can clear our traced segments as well;
        //  this is necessary because unlike in the old not-per-neuron way of
        //  doing things, we would normally need some info from anchors that
        //  just isn't there when the whole skeleton is cleared
        if (anchors==null) {
            anchors = getAnchorsSafe();
        }
        if ( anchors.isEmpty() ) {
            neuronTracedSegments.clear();
            return;
        }

        Set<SegmentIndex> foundSegments = new HashSet<>();
        Collection<AnchoredVoxelPath> skeletonSegments = skeleton.getTracedSegments();
        // log.info("Skeleton has " + skeletonSegments.size() + " traced segments");
        for (AnchoredVoxelPath segment : skeletonSegments) {
            SegmentIndex ix = segment.getSegmentIndex();

            // need neuron ID; get it from the anchor at either end of the
            //  traced path; if there isn't an anchor, just move on--that
            //  path is also gone (happens when neurons deleted, merged)
            Anchor pathAnchor = skeleton.getAnchorByID(ix.getAnchor1Guid());
            if (pathAnchor == null) {
                continue;
            }
            Long neuronID = pathAnchor.getNeuronID();

            foundSegments.add(ix);
            if (neuronTracedSegments.containsKey(neuronID)) {
                if (neuronTracedSegments.get(neuronID).containsKey(ix)) {
                    // Is the old traced segment still valid?
                    AnchoredVoxelPath oldSegment = neuronTracedSegments.get(neuronID).get(ix).getSegment();
                    List<VoxelPosition> p0 = oldSegment.getPath();
                    List<VoxelPosition> p1 = segment.getPath();
                    boolean looksTheSame = true;
                    if (p0.size() != p1.size()) // same size?
                    {
                        looksTheSame = false;
                    } else if (p0.get(0) != p1.get(0)) // same first voxel?
                    {
                        looksTheSame = false;
                    } else if (p0.get(p0.size() - 1) != p1.get(p1.size() - 1)) // same final voxel?
                    {
                        looksTheSame = false;
                    }
                    if (looksTheSame) {
                        continue; // already have this segment, no need to recompute!
                    } else {
                        neuronTracedSegments.get(neuronID).remove(ix);
                    }
                }
            } else {
                // haven't seen this neuron yet
                neuronTracedSegments.put(neuronID, new ConcurrentHashMap<SegmentIndex, TracedPathActor>());
            }
            TracedPathActor actor = new TracedPathActor(segment, getTileFormat());

            neuronTracedSegments.get(neuronID).put(actor.getSegmentIndex(), actor);

            // not sure why this is in the loop instead of out of it!
            //  all it does is trigger a repaint; I suppose it's better to
            //  paint after every path added, so they pop in as they are
            //  ready; paint can't be that expensive, can it?
            updater.update();
        }

        // carefully iterate over segments and prune the obsolete ones
        for (Long neuronID : neuronTracedSegments.keySet()) {
            Set<SegmentIndex> neuronSegmentIndices = new HashSet<>(neuronTracedSegments.get(neuronID).keySet());
            for (SegmentIndex ix : neuronSegmentIndices) {
                if (!foundSegments.contains(ix)) {
                    log.info("Removing orphan segment");
                    neuronTracedSegments.get(neuronID).remove(ix);
                }
            }
        }
    }

    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;

        // propagate to all traced path actors, too:
        for (Long neuronID : neuronTracedSegments.keySet()) {
            for (TracedPathActor path : neuronTracedSegments.get(neuronID).values()) {
                path.setTileFormat(tileFormat);
            }
        }
    }

    /**
     * Change visual anchor position without actually changing the Skeleton model
     */
    public synchronized void lightweightPlaceAnchor(Anchor dragAnchor, Vec3 location) {
        if (dragAnchor == null) {
            return;
        }
        int index = getIndexForAnchor(dragAnchor);
        if (index < 0) {
            return;
        }
        int offset = index * VERTEX_FLOAT_COUNT;
        for (int i = 0; i < 3; ++i) {
            neuronVertices.get(dragAnchor.getNeuronID()).put(offset + i, (float) (double) location.get(i));
        }
        updateLines(null);
        updater.update();
    }

    /**
     * is the input anchor's neuron visible?
     */
    public boolean anchorIsVisible(Anchor anchor) {
        if (anchor == null || anchor.getNeuronID() == null) {
            return false;
        } else {
            return !TmModelManager.getInstance().getCurrentView().isHidden(anchor.getNeuronID());
        }
    }
    
    public boolean anchorIsNonInteractable(Anchor anchor) {
        if (anchor == null || anchor.getNeuronID() == null) {
            return false;
        } else {
            return TmModelManager.getInstance().getCurrentView().isNonInteractable(anchor.getNeuronID());
        }
    }
    
    private Collection<Anchor> getAllAnchors() {
        Set<Anchor> anchors = skeleton.getAnchors();
        synchronized (anchors) {
            return new ArrayList<>(anchors);
        }
    }
    
    private Collection<Anchor> getAnchorsSafe() {
        if (skeleton == null || skeleton.getAnchors() == null || skeleton.getAnchors().size() == 0) {
            return Collections.emptyList();
        }

        boolean anchorsInViewport = ApplicationPanel.isAnchorsInViewport();
        if (viewport==null || pointComputer==null || !anchorsInViewport) {
            // Fallback on old inefficient behavior which always renders all anchors
            return getAllAnchors();
        }
        
        List<Anchor> anchors = new ArrayList<>();
        // Establish the extents of the current viewport in world coordinates

        Point p1 = new Point(viewport.getOriginX(),
                viewport.getOriginY());
        Point p2 = new Point(viewport.getOriginX()+viewport.getWidth(),
                viewport.getOriginY()+viewport.getHeight());

        Vec3 wp1v = pointComputer.worldFromPixel(p1);
        Vec3 wp2v = pointComputer.worldFromPixel(p2);
        double thickness = zoomedZThicknessInPixels/getCamera().getPixelsPerSceneUnit();

        // Add in Z thickness and translate into array format

        double[] wp1 = {
                wp1v.getX(),
                wp1v.getY(),
                wp1v.getZ()-thickness/2
        };
        double[] wp2 = {
                wp2v.getX(),
                wp2v.getY(),
                wp2v.getZ()+thickness/2
        };

        // Find all relevant anchors which are inside the viewport
        Map<Long,TmGeoAnnotation> annotations = new LinkedHashMap<>();
        SpatialIndexManager spatialManager = TmModelManager.getInstance().getSpatialIndexManager();
        List<TmGeoAnnotation> vertexList = spatialManager.getAnchorsInMicronArea(wp1, wp2);
        if (vertexList != null) {
            for (TmGeoAnnotation vertex : vertexList) {
                annotations.put(vertex.getId(), vertex);
            }
        }

        // Add next parent, even if it's not in the current viewport
        Anchor nextParent = getNextParent();
        if (nextParent != null) {
            // Serious gymnastics required to get a TmGeoAnnotation by id
            TmNeuronMetadata nextNeuron = NeuronManager.getInstance().getNeuronFromNeuronID(nextParent.getNeuronID());
            if (nextNeuron!=null) {
                TmGeoAnnotation nextVertex = nextNeuron.getGeoAnnotationMap().get(nextParent.getGuid());
                if (nextVertex != null) {
                    annotations.put(nextVertex.getId(), nextVertex);
                }
            }
        }

        // when zoom is high, the viewport could be so small (especially in z) that we don't
        //  catch points we care about; so add some nearby points on the theory that if you're
        //  zoomed so far in that this is a problem, it's likely that the nearby points
        //  are the ones you care about; choosing 3 such points arbitrarily
        Point pcenter = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        Vec3 wpcenterv = pointComputer.worldFromPixel(pcenter);
        double[] wpcenter = {
                wpcenterv.getX(),
                wpcenterv.getY(),
                wpcenterv.getZ()-thickness/2
        };

        vertexList = spatialManager.getAnchorClosestToMicronLocation(wpcenter, 3);
        if (vertexList != null) {
            for (TmGeoAnnotation vertex : vertexList) {
                annotations.put(vertex.getId(), vertex);
            }
        }

        // Add all parent and child anchors, so that lines are draw even if the linked anchor is outside the viewport
        for (Long annotationId : getRelevantAnchorIds(annotations.values())) {
            Anchor anchor = skeleton.getAnchorByID(annotationId);
            if (anchor != null) {
                anchors.add(anchor);
            }
            else {
                // This is probably ok: it just means that the spatial index returned some anchors which are no longer in the
                // skeleton. The spatial index should get updated separately, and then this will stop.
                log.trace("Cannot find anchor for annotation: "+annotationId);
            }
        }

        if (!anchors.isEmpty() && anchors.size()<annotations.size()) {
            log.warn("Adding less anchors ({}) than are in the index ({}). This probably means the index is stale.", anchors.size(), annotations.size());
        }

        log.debug("Found {} anchors in viewport",anchors.size());
        return anchors;
    }
    
    private Set<Long> getRelevantAnchorIds(Collection<TmGeoAnnotation> annotations) {

        Set<Long> anchorIds = new HashSet<>();
        for (TmGeoAnnotation annotation : annotations) {

            TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(annotation.getNeuronId());

            // Add annotation in viewport
            anchorIds.add(annotation.getId());

            // Add parent
            Long parentId = annotation.getParentId();
            if (parentId!=null && !parentId.equals(neuron.getId())) {
                anchorIds.add(parentId);
            }
            
            // Add children
            for(Long childId : annotation.getChildIds()) {
                anchorIds.add(childId);
            }
        }
        
        return anchorIds;
    }
    
    public synchronized void setHoverAnchor(Anchor anchor) {
        if (anchor == skeleton.getHoverAnchor()) {
            return;
        }
        skeleton.setHoverAnchor(anchor);
        updater.update();
    }

    public Anchor getNextParent() {
        return skeleton.getNextParent();
    }

    public boolean setNextParentByID(Long annotationID) {
        // find the anchor corresponding to this annotation ID and pass along
        if (getSkeleton() == null) {
            return false;
        }
        Anchor foundAnchor = annotationID == null ? null : skeleton.getAnchorByID(annotationID);

        // it's OK if we set a null (it's a deselect)
        return updateParent(foundAnchor);
    }

    public boolean setNextParent(Anchor parent) {
        return updateParent(parent);
    }

    public void addAnchorUpdateListener(UpdateAnchorListener l) {
        getUpdater().addListener(l);
    }

    public void setFocusOnNextParent(boolean flag) {
        isFocusOnNextParent = flag;
    }

    private boolean updateParent(Anchor parent) {
        if (parent != skeleton.getNextParent()) {
            skeleton.setNextParent(parent);
            // first signal is for drawing the marker, second is for notifying
            //  components that want to, eg, select the enclosing neuron
            updater.update();
            updater.update(skeleton.getNextParent());
        }
        if (isFocusOnNextParent && parent != null) {
            getCamera().setFocus(parent.getLocation());
        }
        return true;
    }

    public void setZoomedZThicknessInPixels(float zoomedZThicknessInPixels) {
        this.zoomedZThicknessInPixels  = zoomedZThicknessInPixels;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    public void setPointComputer(BasicMouseMode pointComputer) {
        this.pointComputer = pointComputer;
    }
    
    
 }
