package org.janelia.horta.neuronvbo;

import com.jogamp.common.nio.Buffers;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.model.*;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronEdge;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.model.TmModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one OpenGL vertex buffer object for rendering groups of neuron models.
 * Multiple NeuronVbos may be held in a NeuronVboPool
 * @author brunsc
 * 
 * TODO: Perform full clear when workspace changes
 * TODO: Test all edit operations, from both LVV and Horta
 */
public class NeuronVbo implements Iterable<TmNeuronMetadata>
{
    private final static int FLOATS_PER_VERTEX = 8;
    // Be sure to synchronize these constants with the actual shader vertex attribute (in) layout
    private final static int XYZR_ATTRIB = 1;
    private final static int RGBV_ATTRIB = 2;
    private final static float REVIEWED_GRAY_COLOR = 200;

    private final Set<TmNeuronMetadata> neurons = new HashSet<>();
    private int vboVertices = 0;
    private int vboEdgeIndices = 0;
    private int edgeCount = 0;
    private int vertexCount = 0;
    
    private boolean buffersNeedRebuild = false; // non-gl population of buffer data
    private boolean buffersNeedAllocation = false; // allocate and upload gl buffers
    private boolean buffersNeedUpdate = false; // replace contents of existing gl buffers
    
    private IntBuffer edgeBuffer;
    private FloatBuffer vertexBuffer;
    
    // Cached indices
    private final Map<TmNeuronMetadata, Integer> neuronOffsets = new HashMap<>(); // for surgically updating buffers
    private final Map<TmNeuronMetadata, Integer> neuronVertexCounts = new HashMap<>(); // for sanity checking
    private final Map<TmNeuronMetadata, Integer> neuronEdgeCounts = new HashMap<>(); // for sanity checking
    private final Map<TmNeuronMetadata, NeuronObserver> neuronObservers = new HashMap<>();
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    int getNeuronCount() {
        return neurons.size();
    }
    
    public void clear() {
        // first disconnect all signals
        for (TmNeuronMetadata neuron : this) {
            disconnectSignals(neuron);
        }
        if (edgeCount > 0)
            buffersNeedRebuild = true;
        if (vertexCount > 0)
            buffersNeedRebuild = true;
        neurons.clear();
        neuronOffsets.clear();
        neuronEdgeCounts.clear();
        neuronVertexCounts.clear();
        neuronObservers.clear();
        edgeCount = 0;
        vertexCount = 0;
    }
    
    private void connectSignals(final TmNeuronMetadata neuron) {
        if (neuronObservers.containsKey(neuron))
            return;
       // neuronObservers.put(neuron, new NeuronObserver(neuron));
    }
    
    private void disconnectSignals(TmNeuronMetadata neuron) {
        NeuronObserver observer = neuronObservers.get(neuron);
        if (observer == null)
            return;
        observer.disconnectSignals();
    }
    
    void init(GL3 gl)
    {
        if (vboEdgeIndices > 0)
            return; // already initialized
        IntBuffer vbos = IntBuffer.allocate(2);
        vbos.rewind();
        gl.glGenBuffers(2, vbos);
        vboVertices = vbos.get(0);
        vboEdgeIndices = vbos.get(1);
    }
    
    // Make sure the cone shader is loaded before calling this method
    synchronized void displayEdges(GL3 gl) 
    {
        init(gl);
        if (buffersNeedRebuild)
            rebuildBuffers();
        if (edgeCount < 1) 
            return;
        setUpVbo(gl);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);
        gl.glDrawElements(GL3.GL_LINES, 2 * edgeCount, GL3.GL_UNSIGNED_INT, 0);
    }
    
    // Make sure the sphere shader is loaded before calling this method
    synchronized void displayNodes(GL3 gl) 
    {
        init(gl);
        if (buffersNeedRebuild)
            rebuildBuffers();
        if (vertexCount < 1) 
            return;
        setUpVbo(gl);
        gl.glDrawArrays(GL3.GL_POINTS, 0, vertexCount);
    }
    
    private void setUpVbo(GL3 gl) {
        if (buffersNeedRebuild)
            rebuildBuffers();
        if (buffersNeedAllocation)
            allocateBuffers(gl);
        if (buffersNeedUpdate)
            updateBuffers(gl);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboVertices);
        gl.glEnableVertexAttribArray(XYZR_ATTRIB);
        gl.glVertexAttribPointer(
                XYZR_ATTRIB, // location in shader
                4, // component count
                GL3.GL_FLOAT, // component type
                false, // normalize ints?
                32, // stride in bytes, 8 * sizeof(float)
                0 // offset, in bytes
                );
        gl.glEnableVertexAttribArray(RGBV_ATTRIB);
        gl.glVertexAttribPointer(
                RGBV_ATTRIB, // location in shader
                4, // component count
                GL3.GL_FLOAT, // component type
                false, // normalize ints?
                32, // stride in bytes, 2 * 4 * sizeof(float)
                16 // offset, in bytes = 4 * sizeof(float)
                );
    }
    
    synchronized void dispose(GL3 gl) 
    {
        if (vertexCount > 0)
            buffersNeedAllocation = true;
        if (vboVertices == 0)
            return; // never allocated
        int [] vbos = {vboVertices, vboEdgeIndices};
        gl.glDeleteBuffers(2, vbos, 0);
        vboVertices = 0;
        vboEdgeIndices = 0;
    }
    
    // lightweight update of just the color field
    private boolean updateNeuronColor(TmNeuronMetadata neuron)
    {
        if (buffersNeedRebuild)
            return false;
        int sv = neuron.getAnnotationCount();
        int se = neuron.getEdges().size();
        float rgb[] = {0,0,0,1};
        neuron.getColor().getRGBComponents(rgb);
        boolean bChanged = false; // nothing has changed yet
        
        // check for whether the neuron is under review
        if (TmModelManager.getInstance().getCurrentView().isNeuronInReviewMode(neuron.getId())) {
            buffersNeedRebuild = true;
            return false;
        }
        
        // sanity check
        // Do we already have most of the information for this neuron tabulated?
        if ( neuronOffsets.containsKey(neuron)
                && (neuronVertexCounts.get(neuron) == sv)
                && (neuronEdgeCounts.get(neuron) == se)) 
        {
            // Has the color actually changed?
            final int COLOR_OFFSET = 4; // red color begins at 5th value
            int offset = neuronOffsets.get(neuron) * FLOATS_PER_VERTEX + COLOR_OFFSET;
            int max_offset = offset + (sv-1) * FLOATS_PER_VERTEX + 2;
            if (max_offset >= vertexBuffer.limit()) {
                // Hmm. The actual buffer is no longer big enough to hold this neuron.
                log.info("vertex buffer object is too small. rebuild queued (after updateNeuronColor())");
                buffersNeedRebuild = true;
                return true;
            }
            try {
                if ( (vertexBuffer.get(offset+0) == rgb[0])
                        && (vertexBuffer.get(offset+1) == rgb[1])
                        && (vertexBuffer.get(offset+2) == rgb[2]) )
                {
                    return bChanged; // color has not changed
                }
                log.trace("old neuron color was [{},{},{}]",
                        vertexBuffer.get(offset+0), 
                        vertexBuffer.get(offset+1), 
                        vertexBuffer.get(offset+2));
                log.trace("new neuron color = [{},{},{}]", rgb[0], rgb[1], rgb[2]);
                for (int v = 0; v < sv; ++v) {
                    int index = offset + v * FLOATS_PER_VERTEX;
                    for (int r = 0; r < 3; ++r) {
                        vertexBuffer.put(index + r, rgb[r]);
                        // assert(vertexBuffer.get(index + r) == rgb[r]);
                    }
                }
                buffersNeedUpdate = true;
                bChanged = true;
            } catch (IndexOutOfBoundsException exc) {
                log.info("stale vertex buffer object accessed with bogus index {}. Queueing rebuild.", offset);
                buffersNeedRebuild = true;
                return true;
            }
        }
        else {
            buffersNeedRebuild = true;
            bChanged = true;
        }
        return bChanged;
    }
    
    // lightweight update of just the visibility field
    // returns true if the buffer state actually changed
    private boolean updateNeuronVisibility(TmNeuronMetadata neuron)
    {
        if (buffersNeedRebuild)
            return false; // we are going to redo everything anyway, so skip the surgical update
        int sv = neuron.getAnnotationCount();
        int se = neuron.getEdges().size();
        boolean bIsVisible = neuron.isVisible();
        float visFloat = bIsVisible ? 1.0f : 0.0f;
        boolean bChanged = false;
        // log.info("Updating neuron visibility to '{}'", bIsVisible);

        // sanity check
        // Do we already have most of the information for this neuron tabulated?
        if ( neuronOffsets.containsKey(neuron)
                && (neuronVertexCounts.get(neuron) == sv)
                && (neuronEdgeCounts.get(neuron) == se) ) 
        {
            // Has the visibility actually changed?
            final int VISIBILITY_OFFSET = 7; // visibility is the 8th attribute value
            int offset = neuronOffsets.get(neuron) * FLOATS_PER_VERTEX + VISIBILITY_OFFSET;
            int max_offset = offset + (sv-1) * FLOATS_PER_VERTEX;
            if (max_offset >= vertexBuffer.limit()) {
                log.info("vertex buffer object is too small. rebuild queued (after updateNeuronVisibility())");
                // Hmm. The actual buffer is no longer big enough to hold this neuron.
                buffersNeedRebuild = true;
                return true;
            }
            try {
                if (vertexBuffer.get(offset) != visFloat) { // visibility actually changed
                    for (int v = 0; v < sv; ++v) {
                        int index = offset + v * FLOATS_PER_VERTEX;
                        vertexBuffer.put(index, visFloat);
                    }
                    buffersNeedUpdate = true;
                    bChanged = true;
                }
            } catch (IndexOutOfBoundsException exc) {
                log.info("stale vertex buffer object accessed with bogus index {}. Queueing rebuild.", offset);
                buffersNeedRebuild = true;
                return true;
            }
        }
        else {
            buffersNeedRebuild = true;
            bChanged = true; // something changed...
        }
        return bChanged;
    }
    
    private void rebuildBuffers()
    {
        log.info("Rebuilding neuron vbo data");
        // count the primitives
        List<Float> vertexAttributes = new ArrayList<>();
        List<Integer> edgeIndexes = new ArrayList<>();
        vertexCount = 0;
        edgeCount = 0;
        float rgb[] = {0,0,0};
        neuronOffsets.clear();
        neuronVertexCounts.clear();
        neuronEdgeCounts.clear();
        for (TmNeuronMetadata neuron : neurons) {
            // if (! neuron.isVisible()) continue;
            neuronOffsets.put(neuron, vertexCount);
            neuronVertexCounts.put(neuron, neuron.getAnnotationCount());
            neuronEdgeCounts.put(neuron, neuron.getEdges().size());
            float visibility = neuron.isVisible() ? 1 : 0;
            Color color = neuron.getColor();
            color.getColorComponents(rgb);
            Map<TmGeoAnnotation, Integer> vertexIndices = new HashMap<>();
            for (TmGeoAnnotation vertex : neuron.getGeoAnnotationMap().values()) {
                int index = vertexCount;
                vertexIndices.put(vertex, index);
                // X, Y, Z, radius, r, g, b, visibility
                float[] xyz = TmModelManager.getInstance().getLocationInMicrometers(vertex.getX(),
                        vertex.getY(), vertex.getZ());
                vertexAttributes.add(xyz[0]); // X
                vertexAttributes.add(xyz[1]); // Y
                vertexAttributes.add(xyz[2]); // Z
                float radius = vertex.getRadius().floatValue();
                if (TmModelManager.getInstance().getCurrentView().isNeuronRadiusToggle(neuron.getId())) {
                    radius = 0.3f;
                }
                vertexAttributes.add(radius); // radius
                /*if (neuron.getReviewMode() && neuron.isReviewedVertex(vertex)) {
                    vertexAttributes.add(REVIEWED_GRAY_COLOR); // red
                    vertexAttributes.add(REVIEWED_GRAY_COLOR); // green
                    vertexAttributes.add(REVIEWED_GRAY_COLOR); // blue
                } else {
                 */
                vertexAttributes.add(rgb[0]); // red
                vertexAttributes.add(rgb[1]); // green
                vertexAttributes.add(rgb[2]); // blue
                //}
                vertexAttributes.add(visibility); // visibility
                vertexCount += 1;
            }
           for (TmNeuronEdge edge : getEdges(neuron)) {
                TmGeoAnnotation v1 = edge.getParentVertex();
                TmGeoAnnotation v2 = edge.getChildVertex();
                Integer i1 = vertexIndices.get(v1);
                Integer i2 = vertexIndices.get(v2);
                if ( (i1 == null) || (i2 == null) ) {
                    log.error("Found neuron edge with unknown vertices {} and {} in neuron '{}'", v1, v2, neuron.getName());
                    continue;
                }
                edgeIndexes.add(i1);
                edgeIndexes.add(i2);
                edgeCount += 1;
            }
        }
        
        // allocate storage
        vertexBuffer = Buffers.newDirectFloatBuffer(vertexCount * FLOATS_PER_VERTEX);        
        vertexBuffer.rewind();
        for (float f : vertexAttributes) {
            vertexBuffer.put(f);
        }
        vertexBuffer.flip();
        
        edgeBuffer = Buffers.newDirectIntBuffer(edgeCount * 2);
        edgeBuffer.rewind();
        for (int i : edgeIndexes) {
            edgeBuffer.put(i);
        }
        edgeBuffer.flip();

        buffersNeedRebuild = false;
        buffersNeedAllocation = true;

        final boolean debugVboContents = false;
        if (debugVboContents) {
            log.info("Vertex buffer contents:");
            for (int i = 0; i < vertexBuffer.capacity(); ++i) {
                log.info(" vertex {} : {}", i, vertexBuffer.get(i));            
            }
            log.info("Edge buffer contents:");
            for (int i = 0; i < edgeBuffer.capacity(); ++i) {
                log.info(" edge {} : {}", i, edgeBuffer.get(i));            
            }
        }        
    }

    private Collection<TmNeuronEdge> getEdges(TmNeuronMetadata neuron) {
        Set<TmNeuronEdge> freshEdges = new HashSet<>(); // All edges in the current model
        for (TmGeoAnnotation child : neuron.getGeoAnnotationMap().values()) {
                Long parentId = child.getParentId();
                TmGeoAnnotation parent = neuron.getGeoAnnotationMap().get(parentId);
                if (parent == null)
                    continue;
                TmNeuronEdge edge = new TmNeuronEdge(parent, child);
                freshEdges.add(edge);
        }
        return freshEdges;
    }

    private void allocateBuffers(GL3 gl)
    {
        log.info("Uploading neuron vbo data");
        if (buffersNeedRebuild)
            rebuildBuffers();
        vertexBuffer.rewind();
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboVertices);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, 
                vertexBuffer.capacity() * Buffers.SIZEOF_FLOAT,
                vertexBuffer, 
                GL3.GL_STATIC_DRAW);
        edgeBuffer.rewind();
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);        
        gl.glBufferData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                edgeBuffer.capacity() * Buffers.SIZEOF_INT,
                edgeBuffer,
                GL3.GL_STATIC_DRAW);

        buffersNeedAllocation = false;
        buffersNeedUpdate = false;
    }
    
    // Reloads the entire buffer
    // TODO: Incrementally update one neuron at a time.
    private void updateBuffers(GL3 gl)
    {
        // log.info("Updating neuron vbo data");
        if (buffersNeedRebuild)
            rebuildBuffers();
        vertexBuffer.rewind();
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboVertices);
        gl.glBufferSubData(
                GL3.GL_ARRAY_BUFFER, 
                0,
                vertexBuffer.capacity() * Buffers.SIZEOF_FLOAT,
                vertexBuffer);
        edgeBuffer.rewind();
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);        
        gl.glBufferSubData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                0, 
                edgeBuffer.capacity() * Buffers.SIZEOF_INT,
                edgeBuffer);

        buffersNeedUpdate = false;
    }

    boolean add(final TmNeuronMetadata neuron)
    {
        if (neuron == null)
            return false;
        if (neurons.contains(neuron))
            return false;
        if (! neurons.add(neuron))
            return false;

        vertexCount += neuron.getAnnotationCount();
        buffersNeedRebuild = true;

        connectSignals(neuron);
        
        return true;
    }

    @Override
    public Iterator<TmNeuronMetadata> iterator() {
        return neurons.iterator();
    }

    boolean isEmpty() {
        return neurons.isEmpty();
    }

    int getVertexCount() {
        return vertexCount;
    }

    boolean contains(TmNeuronMetadata neuron) {
        return neurons.contains(neuron);
    }

    boolean remove(TmNeuronMetadata neuron) {
        if (! neurons.remove(neuron))
            return false;
        disconnectSignals(neuron);
        buffersNeedRebuild = true;
        return true;
    }

    void checkForChanges() 
    {
        // log.info("check for changes");
        if (buffersNeedRebuild)
            return; // no need to check counts, if we will be rebuilding anyway
        for (TmNeuronMetadata neuron : this) {
            if ( (neuron.getAnnotationCount() != neuronVertexCounts.get(neuron))
                    || (neuron.getEdges().size() != neuronEdgeCounts.get(neuron)))
            {
                buffersNeedRebuild = true;
                return;
            }
            // Check for visibility and color changes, in case of bulk update
            updateNeuronVisibility(neuron);
            updateNeuronColor(neuron);
        }
    }
    
    private class NeuronObserver
    {
        private final TmNeuronMetadata neuron;
        
        // Surgical update after color change
        private final Observer colorChangeObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                updateNeuronColor(neuron);
            }
        };
        
        // Surgical update after visibility change
        private final Observer visibilityObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                updateNeuronVisibility(neuron);
            }
        };
        
        // Full update after adding a vertex: the total number of vertices changed
        private final NeuronVertexCreationObserver vertexCreationObserver = new NeuronVertexCreationObserver() {
            @Override
            public void update(GenericObservable<VertexWithNeuron> object, VertexWithNeuron data) {
                buffersNeedRebuild = true;
            }
        };
        
        /* Maybe not necessary... */
        private final Observer geometryChangeObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                buffersNeedRebuild = true;
            }            
        };
        
        private final NeuronVertexUpdateObserver vertexUpdateObserver = new NeuronVertexUpdateObserver() {
            @Override
            public void update(GenericObservable<VertexWithNeuron> o, VertexWithNeuron arg)
            {
                // TODO: Surgical update -- while you are at it, you should probably investigate and
                // refactor usages of "geometryChangeObservable".
                buffersNeedRebuild = true;
            }
        };
        
        private final NeuronVertexDeletionObserver vertexDeletionObserver = new NeuronVertexDeletionObserver() {
            @Override
            public void update(GenericObservable<VertexCollectionWithNeuron> object, VertexCollectionWithNeuron data) {
                buffersNeedRebuild = true;
            }
        };
        
        
        public NeuronObserver(TmNeuronMetadata neuron) {
            this.neuron = neuron;
            connectSignals();
        }
        
        private void connectSignals() 
        {
           /* neuron.getColorChangeObservable().addObserver(colorChangeObserver);
            neuron.getVisibilityChangeObservable().addObserver(visibilityObserver);
            neuron.getVertexCreatedObservable().addObserver(vertexCreationObserver);
            neuron.getGeometryChangeObservable().addObserver(geometryChangeObserver);
            neuron.getVertexUpdatedObservable().addObserver(vertexUpdateObserver);
            neuron.getVertexesRemovedObservable().addObserver(vertexDeletionObserver);*/
    }

        private void disconnectSignals() 
        {
            /*neuron.getColorChangeObservable().deleteObserver(colorChangeObserver);
            neuron.getVisibilityChangeObservable().deleteObserver(visibilityObserver);
            neuron.getVertexCreatedObservable().deleteObserver(vertexCreationObserver);
            neuron.getGeometryChangeObservable().deleteObserver(geometryChangeObserver);
            neuron.getVertexUpdatedObservable().deleteObserver(vertexUpdateObserver);
            neuron.getVertexesRemovedObservable().deleteObserver(vertexDeletionObserver);*/
        }
    }

}
