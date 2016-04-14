package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by murphys on 4/14/2016.
 */
public class SkeletonActorModel {

    private static final Logger log = LoggerFactory.getLogger(SkeletonActorModel.class);

    private class ElementDataOffset {
        public ElementDataOffset(Long id, int size, long offset) { this.id=id; this.size=size; this.offset=offset; }
        public Long id;
        public int size;
        public long offset;
    }

    // semantic constants for allocating byte arrays
    private static final int FLOAT_BYTE_COUNT = 4;
    private static final int VERTEX_FLOAT_COUNT = 3;
    private static final int INT_BYTE_COUNT = 4;
    private static final int COLOR_FLOAT_COUNT = 3;

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


}
