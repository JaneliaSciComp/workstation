package org.janelia.horta.neuronvbo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For improved rendering performance with large numbers of neurons,
 * NeuronVboPool distributes all the neurons among a finite set of vertex buffer
 * objects. Instead of using a separate vbo for each neuron, like we were doing
 * before.
 *
 * @author brunsc
 */
public class NeuronVboPool implements Iterable<TmNeuronMetadata> {

    // Use pool size to balance:
    //  a) static rendering performance (more vbos means more draw calls, means slower rendering)
    //  b) edit update speed (more vbos means fewer neurons per vbo, means faster edit-to-display time)
    // Weird: POOL_SIZE=30 animates much faster than POOL_SIZE=5 with about 120 neurons / 300,000 vertices
    // (smaller blocks for the win...)
    private final static int POOL_SIZE = 30;

    // Maintain vbos in a structure sorted by how much stuff is in each one.
    private final NavigableMap<Integer, Deque<NeuronVbo>> vbos = new TreeMap<>();
    // private final List<NeuronVbo> vbos;
    // private int nextVbo = 0;

    // private Set<NeuronModel> dirtyNeurons; // Track incremental updates
    private Map<Long, NeuronVbo> neuronMap = new HashMap<>();
    // TODO: increase after initial debugging
    // Shaders...
    // Be sure to synchronize these constants with the actual shader source uniform layout
    private final ShaderProgram conesShader = new ConesShader();
    private final ShaderProgram spheresShader = new SpheresShader();
    private final static int VIEW_UNIFORM = 1;
    private final static int PROJECTION_UNIFORM = 2;
    private final static int LIGHTPROBE_UNIFORM = 3;
    private final static int SCREENSIZE_UNIFORM = 4;
    private final static int RADIUS_OFFSET_UNIFORM = 5;
    private final static int RADIUS_SCALE_UNIFORM = 6;
    private final Texture2d lightProbeTexture;

    private float minPixelRadius = 0.8f; // TODO: expose as adjustable parameter
    private float radiusOffset = 0.0f; // amount to add to every radius, in micrometers
    private float radiusScale = 1.0f; // amount to multiply every radius, in micrometers

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public NeuronVboPool() {
        for (int i = 0; i < POOL_SIZE; ++i) {
            insertVbo(new NeuronVbo());
        }

        lightProbeTexture = new Texture2d();
        try {
            lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                    "/org/janelia/gltools/material/lightprobe/"
                    + "Office1W165Both.ppm"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    synchronized private void insertVbo(NeuronVbo vbo) {
        Integer vboSize = vboSize(vbo);
        if (!vbos.containsKey(vboSize)) {
            vbos.put(vboSize, new ConcurrentLinkedDeque<NeuronVbo>());
        }
        Collection<NeuronVbo> subList = vbos.get(vboSize);
        subList.add(vbo);
    }

    // Method vboSize is used to determine whether one vbo has more stuff in
    // it than another
    private static Integer vboSize(NeuronVbo vbo) {
        return vbo.getVertexCount();
    }

    synchronized private NeuronVbo popEmptiestVbo() {
        Map.Entry<Integer, Deque<NeuronVbo>> entry = vbos.firstEntry();
        Integer vboSize = entry.getKey();
        Deque<NeuronVbo> vboDeque = entry.getValue();
        NeuronVbo vbo = vboDeque.removeFirst();
        if (vboDeque.isEmpty()) {
            vbos.remove(vboSize); // That was the last of its kind
        }
        return vbo;
    }

    public float getRadiusOffset() {
        return radiusOffset;
    }

    public void setRadiusOffset(float radiusOffset) {
        this.radiusOffset = radiusOffset;
    }

    public float getRadiusScale() {
        return radiusScale;
    }

    public void setRadiusScale(float radiusScale) {
        this.radiusScale = radiusScale;
    }

    private void setUniforms(
            GL3 gl,
            float[] modelViewMatrix,
            float[] projectionMatrix,
            float[] screenSize) {
        gl.glUniformMatrix4fv(VIEW_UNIFORM, 1, false, modelViewMatrix, 0);
        gl.glUniformMatrix4fv(PROJECTION_UNIFORM, 1, false, projectionMatrix, 0);
        gl.glUniform2fv(SCREENSIZE_UNIFORM, 1, screenSize, 0);
        gl.glUniform1i(LIGHTPROBE_UNIFORM, 0);
        gl.glUniform1f(RADIUS_OFFSET_UNIFORM, radiusOffset);
        gl.glUniform1f(RADIUS_SCALE_UNIFORM, radiusScale);
    }

    void display(GL3 gl, AbstractCamera camera) {
        float[] modelViewMatrix = camera.getViewMatrix().asArray();
        float[] projectionMatrix = camera.getProjectionMatrix().asArray();
        float[] screenSize = new float[]{
            camera.getViewport().getWidthPixels(),
            camera.getViewport().getHeightPixels()
        };
        lightProbeTexture.bind(gl, 0);

        float micrometersPerPixel
                = camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        radiusOffset = minPixelRadius * micrometersPerPixel;

        // First pass: draw all the connections (edges) between adjacent neuron anchor nodes.
        // These edges are drawn as truncated cones, tapering width between
        // the radii of the adjacent nodes.
        conesShader.load(gl);
        setUniforms(gl, modelViewMatrix, projectionMatrix, screenSize);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.displayEdges(gl);
        }

        // TODO: Second pass: repeat display loop for spheres/nodes
        spheresShader.load(gl);
        setUniforms(gl, modelViewMatrix, projectionMatrix, screenSize);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.displayNodes(gl);
        }
    }

    void dispose(GL3 gl) {
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.dispose(gl);
        }
        lightProbeTexture.dispose(gl);
        conesShader.dispose(gl);
        spheresShader.dispose(gl);
    }

    void init(GL3 gl) {
        conesShader.init(gl);
        spheresShader.init(gl);
        lightProbeTexture.init(gl);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.init(gl);
        }
    }

    synchronized void add(TmNeuronMetadata neuron) {
        // To keep the vbos balanced, always insert into the emptiest vbo
        NeuronVbo emptiestVbo = popEmptiestVbo();
        final boolean doLogStats = false;
        if (doLogStats) {
            log.info("Emptiest vbo ({}) contains {} neurons and {} vertices",
                    emptiestVbo.toString(),
                    emptiestVbo.getNeuronCount(),
                    emptiestVbo.getVertexCount());
        }
        emptiestVbo.add(neuron);
        neuronMap.put(neuron.getId(), emptiestVbo);
        if (doLogStats) {
            log.info("Emptiest vbo ({}) now contains {} neurons and {} vertices after insersion",
                    emptiestVbo.toString(),
                    emptiestVbo.getNeuronCount(),
                    emptiestVbo.getVertexCount());
        }
        insertVbo(emptiestVbo); // Reinsert into its new sorted location
    }

    synchronized boolean remove(TmNeuronMetadata neuron) {
        for (NeuronVbo vbo : new VboIterable()) {
            if (vbo.remove(neuron)) {
                return true;
            }
        }
        return false;
    }

    boolean isEmpty() {
        for (NeuronVbo vbo : new VboIterable()) {
            if (!vbo.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    synchronized public void clear() {
        for (Iterator<NeuronVbo> it = new VboIterator(); it.hasNext();) {
            NeuronVbo vbo = it.next();
            vbo.clear();
        }
    }

    boolean contains(TmNeuronMetadata neuron) {
        for (Iterator<NeuronVbo> it = new VboIterator(); it.hasNext();) {
            NeuronVbo vbo = it.next();
            if (vbo.contains(neuron)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<TmNeuronMetadata> iterator() {
        return new NeuronIterator();
    }

    public void markAsDirty(Long neuronId) {
        NeuronVbo dirtyVbo = neuronMap.get(neuronId);
        if (dirtyVbo!=null) {
            dirtyVbo.markAsDirty();
        }
    }

    void checkForChanges() {
        for (Iterator<NeuronVbo> it = new VboIterator(); it.hasNext();) {
            NeuronVbo vbo = it.next();
            vbo.checkForChanges();
        }
    }

    void checkForChanges(TmNeuronMetadata neuron) {
        for (Iterator<NeuronVbo> it = new VboIterator(); it.hasNext();) {
            NeuronVbo vbo = it.next();
            if (vbo.contains(neuron)) {
                vbo.checkForChanges();
            }
        }
    }

    private static class ConesShader extends BasicShaderProgram {

        public ConesShader() {
            try {
                // Cones and spheres share a vertex shader
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "ConesColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "ConesColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private static class SpheresShader extends BasicShaderProgram {

        public SpheresShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "SpheresColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                + "SpheresColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private class VboIterable implements Iterable<NeuronVbo> {

        @Override
        public Iterator<NeuronVbo> iterator() {
            return new VboIterator();
        }
    }

    private class VboIterator implements Iterator<NeuronVbo> {

        private final Collection<NeuronVbo> EMPTY_LIST = Collections.<NeuronVbo>emptyList();

        private final Iterator<Integer> sizeIterator;
        private Iterator<NeuronVbo> vboIterator = EMPTY_LIST.iterator();

        public VboIterator() {
            sizeIterator = vbos.keySet().iterator();
            if (sizeIterator.hasNext()) {
                Integer currentSize = sizeIterator.next();
                vboIterator = vbos.get(currentSize).iterator();
            }
        }

        private void advanceToNextVbo() {
            // Advance to next actual neuron
            while (sizeIterator.hasNext() && (!vboIterator.hasNext())) {
                Integer currentSize = sizeIterator.next();
                vboIterator = vbos.get(currentSize).iterator();
            }
        }

        @Override
        public boolean hasNext() {
            advanceToNextVbo();
            return vboIterator.hasNext();
        }

        @Override
        public NeuronVbo next() {
            advanceToNextVbo();
            return vboIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class NeuronIterator implements Iterator<TmNeuronMetadata> {

        private final Collection<TmNeuronMetadata> EMPTY_LIST = Collections.<TmNeuronMetadata>emptyList();

        private final Iterator<NeuronVbo> vboIterator;
        private Iterator<TmNeuronMetadata> neuronIterator = EMPTY_LIST.iterator(); // iterator for one vbo

        public NeuronIterator() {
            vboIterator = new VboIterator();
            if (vboIterator.hasNext()) {
                NeuronVbo currentVbo = vboIterator.next();
                neuronIterator = currentVbo.iterator();
            }
        }

        private void advanceToNextNeuron() {
            // Advance to next actual neuron
            while (vboIterator.hasNext() && (!neuronIterator.hasNext())) {
                NeuronVbo currentVbo = vboIterator.next();
                neuronIterator = currentVbo.iterator();
            }
        }

        @Override
        public boolean hasNext() {
            advanceToNextNeuron();
            return neuronIterator.hasNext();
        }

        @Override
        public TmNeuronMetadata next() {
            advanceToNextNeuron();
            return neuronIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
