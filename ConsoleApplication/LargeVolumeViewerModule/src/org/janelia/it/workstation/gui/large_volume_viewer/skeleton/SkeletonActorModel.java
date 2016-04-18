package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.UpdateAnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.workstation.tracing.VoxelPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private NeuronStyleModel neuronStyles;

    public void clearStyles() {
        neuronStyles.clear();
    }

    public void setNeuronStyleModel(NeuronStyleModel nsModel) {
        this.neuronStyles = nsModel;
    }

    public NeuronStyleModel getNeuronStyles() { return neuronStyles; }

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

    public synchronized void updateSkeletonActorIfNecessary(SkeletonActor skeletonActor) {
    }

    // Vertex buffer objects need indices
    private Map<Anchor, Integer> neuronAnchorIndices = new HashMap<>();
    private Map<Long, Map<Integer, Anchor>> neuronIndexAnchors = new HashMap<>();
    private Map<Long, IntBuffer> neuronPointIndices = new HashMap<>();
    private Map<Long, IntBuffer> neuronLineIndices = new HashMap<>();

    private Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments = new HashMap<>();

    public Map<Long, IntBuffer> getNeuronLineIndices() { return neuronLineIndices; }

    private boolean verticesNeedCopy() { return true; }

    int cummulativeVertexOffset = 0;
    int cummulativeColorOffset = 0;
    int cummulativeLineOffset = 0;

    int cummulativePointOffset=0;

    public SkeletonActorModel() {
        updater = new SkeletonActorStateUpdater();
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

    public boolean updateVertices() {
        if (verticesNeedCopy()) {

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

                if (neuronStyles.get(neuronID) != null && !neuronStyles.get(neuronID).isVisible()) {
                    continue;
                }

                neuronOrderList.add(neuronID);

                int vertexBufferSize = neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT;
                int colorBufferSize = neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT;

                int lineBufferSize = 0;
                if (neuronLineIndices.containsKey(neuronID)) {
                    lineBufferSize = neuronLineIndices.get(neuronID).capacity() * INT_BYTE_COUNT;
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
                //log.info("start n="+n);
                ElementDataOffset elementDataOffset = vertexOffsets.get(n);
                FloatBuffer neuronVertexBuffer = neuronVertices.get(elementDataOffset.id);
                neuronVertexBuffer.rewind();
                vertexBuffer.put(neuronVertexBuffer);
                //log.info("end n="+n);
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
            return true;
        } else {
            return false;
        }
    }

    private boolean pointIndicesNeedCopy() { return true; }

    public boolean updatePoints() {
        if (pointIndicesNeedCopy()) {

            pointOffsets.clear();

            cummulativePointOffset=0;
            List<Long> neuronOrderList=new ArrayList<>();

            for (Long neuronID : neuronVertices.keySet()) {
                if (neuronStyles.get(neuronID) != null && !neuronStyles.get(neuronID).isVisible()) {
                    continue;
                }
                neuronOrderList.add(neuronID);
                int pointBufferSize=neuronPointIndices.get(neuronID).capacity()*INT_BYTE_COUNT;
                pointOffsets.add(new ElementDataOffset(neuronID, pointBufferSize, cummulativePointOffset));
                cummulativePointOffset+=pointBufferSize;
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
            return true;
        } else {
            return false;
        }
    }

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

    public int getIndexForAnchor(Anchor anchor) {
        if (anchor == null) {
            return -1;
        }
        if (neuronAnchorIndices.containsKey(anchor)) {
            return neuronAnchorIndices.get(anchor);
        }
        return -1;
    }

    public void changeNeuronStyle(TmNeuron neuron, NeuronStyle style) {
        if (neuron != null) {
            neuronStyles.put(neuron.getId(), style);
            updateAnchors();
        }
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
            ByteBuffer tempBytes = ByteBuffer.allocateDirect(neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronVertices.put(neuronID, tempBytes.asFloatBuffer());
            neuronVertices.get(neuronID).rewind();

            tempBytes = ByteBuffer.allocateDirect(neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronColors.put(neuronID, tempBytes.asFloatBuffer());
            neuronColors.get(neuronID).rewind();
        }

        neuronAnchorIndices.clear();
        neuronIndexAnchors.clear();
        Map<Long, Integer> neuronVertexIndex = new HashMap<>();
        int currentVertexIndex;
        NeuronStyle style;
        for (Anchor anchor : anchors) {
            Long neuronID = anchor.getNeuronID();
            Vec3 xyz = anchor.getLocation();
            neuronVertices.get(neuronID).put((float) xyz.getX());
            neuronVertices.get(neuronID).put((float) xyz.getY());
            neuronVertices.get(neuronID).put((float) xyz.getZ());
            if (neuronStyles.containsKey(neuronID)) {
                style = neuronStyles.get(neuronID);
            } else {
                style = NeuronStyle.getStyleForNeuron(neuronID);
            }
            neuronColors.get(neuronID).put(style.getRedAsFloat());
            neuronColors.get(neuronID).put(style.getGreenAsFloat());
            neuronColors.get(neuronID).put(style.getBlueAsFloat());

            if (neuronVertexIndex.containsKey(neuronID)) {
                currentVertexIndex = neuronVertexIndex.get(neuronID);
            } else {
                currentVertexIndex = 0;
                neuronVertexIndex.put(neuronID, currentVertexIndex);
            }
            neuronAnchorIndices.put(anchor, currentVertexIndex);

            if (!neuronIndexAnchors.containsKey(neuronID)) {
                neuronIndexAnchors.put(neuronID, new HashMap<Integer, Anchor>());
            }
            neuronIndexAnchors.get(neuronID).put(currentVertexIndex, anchor);

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
        updateTracedPaths();

        // lines between points, if no path (must be done after path updates so
        //  we know where the paths are!)
        updateLines();

        if (!anchors.contains(getNextParent())) {
            setNextParent(null);
        }

        updater.update();
    }

    private void updateLines() {
        // iterate through anchors and record lines where there are no traced
        //  paths; then copy the line indices you get into an array
        // note: I believe this works because we process the points and
        //  lines in exactly the same order (the order skeleton.getAnchors()
        //  returns them in)

        Map<Long, List<Integer>> tempLineIndices = new HashMap<>();
        for (Anchor anchor : getAnchorsSafe()) {
            int i1 = getIndexForAnchor(anchor);
            if (i1 < 0) {
                continue;
            }
            for (Anchor neighbor : anchor.getNeighbors()) {
                int i2 = getIndexForAnchor(neighbor);
                if (i2 < 0) {
                    continue;
                }
                if (i1 >= i2) {
                    continue; // only use ascending pairs, for uniqueness
                }
                SegmentIndex segmentIndex = new SegmentIndex(anchor.getGuid(), neighbor.getGuid());
                // if neuron has any paths, check and don't draw line
                //  where there's already a traced segment
                if (neuronTracedSegments.containsKey(anchor.getNeuronID())
                        && neuronTracedSegments.get(anchor.getNeuronID()).containsKey(segmentIndex)) {
                    continue;
                }
                if (!tempLineIndices.containsKey(anchor.getNeuronID())) {
                    tempLineIndices.put(anchor.getNeuronID(), new Vector<Integer>());
                }
                tempLineIndices.get(anchor.getNeuronID()).add(i1);
                tempLineIndices.get(anchor.getNeuronID()).add(i2);
            }
        }

        // loop over neurons and fill the arrays
        neuronLineIndices.clear();
        for (Long neuronID : tempLineIndices.keySet()) {
            ByteBuffer lineBytes = ByteBuffer.allocateDirect(tempLineIndices.get(neuronID).size() * INT_BYTE_COUNT);
            lineBytes.order(ByteOrder.nativeOrder());
            neuronLineIndices.put(neuronID, lineBytes.asIntBuffer());
            neuronLineIndices.get(neuronID).rewind();
            for (int i : tempLineIndices.get(neuronID)) // fill actual int buffer
            {
                neuronLineIndices.get(neuronID).put(i);
            }
            neuronLineIndices.get(neuronID).rewind();
        }

        verticesNeedCopy = true;
    }

    private void updateTracedPaths() {
        // Update Traced path actors

        // first, a short-circuit; if there are no anchors, the whole
        //  skeleton was cleared, and we can clear our traced segments as well;
        //  this is necessary because unlike in the old not-per-neuron way of
        //  doing things, we would normally need some info from anchors that
        //  just isn't there when the whole skeleton is cleared
        Collection<Anchor> anchors = getAnchorsSafe();
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

    /*
 * Change visual anchor position without actually changing the Skeleton model
 */
    public void lightweightPlaceAnchor(Anchor dragAnchor, Vec3 location) {
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
        updateLines();
        updater.update();
    }

    /**
     * is the input anchor's neuron visible?
     */
    public boolean anchorIsVisible(Anchor anchor) {
        if (anchor == null || anchor.getNeuronID() == null || !neuronStyles.containsKey(anchor.getNeuronID())) {
            return false;
        } else {
            return neuronStyles.get(anchor.getNeuronID()).isVisible();
        }
    }

    protected Anchor findAnchor(Long annotationID) {
        Anchor foundAnchor = null;
        for (Anchor testAnchor : getAnchorsSafe()) {
            if (testAnchor.getGuid().equals(annotationID)) {
                foundAnchor = testAnchor;
                break;
            }
        }
        return foundAnchor;
    }

    /** Avoid concurrent modification exceptions, during import. */
    private Collection<Anchor> getAnchorsSafe() {
        if (skeleton == null || skeleton.getAnchors() == null) {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<>(skeleton.getAnchors());
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
        Anchor foundAnchor = findAnchor(annotationID);

        // it's OK if we set a null (it's a deselect)
        return updateParent(foundAnchor);
    }

    public boolean setNextParent(Anchor parent) {
        return updateParent(parent);
    }

    public void addAnchorUpdateListener(UpdateAnchorListener l) {
        getUpdater().addListener(l);
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

}
