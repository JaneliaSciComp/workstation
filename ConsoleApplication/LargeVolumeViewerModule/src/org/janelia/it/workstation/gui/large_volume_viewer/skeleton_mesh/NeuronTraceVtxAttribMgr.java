/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.BufferPackager;
import org.janelia.it.jacs.shared.mesh_loader.NormalCompositor;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;

/**
 * Handle any mesh's vertex attributes.
 * 
 * @author fosterl
 */
public class NeuronTraceVtxAttribMgr implements VertexAttributeSourceI {
    // The skeleton and neuron styles constitute the 'model' to be studied
    // in creating the 3D representation of annotations.
    private Skeleton skeleton;
    private NeuronStyleModel neuronStyleModel;

    // The sources and render buffers will be created, based on the contents
    // of the 'model'.
    private List<TriangleSource> triangleSources = new ArrayList<>();
    private Map<Long, RenderBuffersBean> renderIdToBuffers = new HashMap<>();
    
    /**
     * Call this whenever something in the 'model' has been changed.
     * 
     * @return
     * @throws Exception 
     */
    @Override
    public List<TriangleSource> execute() throws Exception {
        if ( skeleton == null  ||  neuronStyleModel == null ) {
            throw new Exception("Please set all model information before execution.");
        }
        
        createVerticesAndBuffers();

        // Build triangle sources and render buffers, from input neuron info.
        for ( TriangleSource factory: triangleSources ) {
            // Now have a full complement of triangles and vertices.  For this renderable, can traverse the
            // vertices, making a "composite normal" based on the normals of all entangling triangles.
            NormalCompositor normalCompositor = new NormalCompositor();
            normalCompositor.createGouraudNormals(factory);
            BufferPackager packager = new BufferPackager();
            RenderBuffersBean rbb = new RenderBuffersBean();
            rbb.setAttributesBuffer(packager.getVertexAttributes(factory));
            rbb.setIndexBuffer(packager.getIndices(factory));
        }
        
        return triangleSources;
    }

    @Override
    public Map<Long, RenderBuffersBean> getRenderIdToBuffers() {
        return renderIdToBuffers;
    }

    @Override
    public void close() {
        renderIdToBuffers.clear();
        triangleSources.clear();
    }

    @Override
    public void exportVertices(File outputLocation, String filenamePrefix) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the skeleton
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * @param skeleton the skeleton to set
     */
    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * @return the neuronStyleModel
     */
    public NeuronStyleModel getNeuronStyleModel() {
        return neuronStyleModel;
    }

    /**
     * @param neuronStyleModel the neuronStyleModel to set
     */
    public void setNeuronStyleModel(NeuronStyleModel neuronStyleModel) {
        this.neuronStyleModel = neuronStyleModel;
    }

    /**
     * Here is where the 'model' is transformed into vertices and render
     * buffers.
     * 
     * @throws Exception 
     */
    private void createVerticesAndBuffers() throws Exception {
        // Make triangle sources.
    }
    
}
