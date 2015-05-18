/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.janelia.it.jacs.shared.mesh_loader.BufferPackager;
import org.janelia.it.jacs.shared.mesh_loader.NormalCompositor;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.workstation.tracing.VoxelPosition;

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
            normalCompositor.combineCustomNormals(factory);
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
		LineEnclosureFactory tracedSegmentEnclosureFactory = new LineEnclosureFactory(6, 8);
        LineEnclosureFactory manualSegmentEnclosureFactory = new LineEnclosureFactory(5, 4);
        
        Set<SegmentIndex> voxelPathAnchorPairs = new HashSet<>();
        
		// Iterate over all the annotations, and add enclosures for each
		// line segment.
        for ( AnchoredVoxelPath voxelPath: skeleton.getTracedSegments() ) {
            voxelPathAnchorPairs.add( voxelPath.getSegmentIndex() );
            double[] previousCoords = null; 
            for ( VoxelPosition voxelPos: voxelPath.getPath() ) {
                double[] currentCoords = new double[] {
                    voxelPos.getX(), voxelPos.getY(), voxelPos.getZ()
                };
                if ( previousCoords != null ) {                    
                    tracedSegmentEnclosureFactory.addEnclosure(
                            previousCoords, currentCoords
                    );
                }                
                previousCoords = currentCoords;
            }
        }

        // Now get the lines.
        Collection<Collection<Vec3>> anchorLines = getAnchorLines(voxelPathAnchorPairs);
        for ( Collection<Vec3> coordPair: anchorLines ) {
            final Iterator<Vec3> iterator = coordPair.iterator();
            manualSegmentEnclosureFactory.addEnclosure(
                    toDoubleArr(iterator.next()),
                    toDoubleArr(iterator.next())
            );
        }

		// Add each factory to the collection.
		triangleSources.add(tracedSegmentEnclosureFactory);
        triangleSources.add(manualSegmentEnclosureFactory);
		
		// Establish the indexes.
		BufferPackager packager = new BufferPackager();
        long i = 0;
        for ( TriangleSource source: triangleSources ) {
            RenderBuffersBean rbb = new RenderBuffersBean();
            rbb.setAttributesBuffer(packager.getVertexAttributes(source));
            rbb.setIndexBuffer(packager.getIndices(source));

            rbb = new RenderBuffersBean();
            renderIdToBuffers.put(i++, rbb);
        }
    }
    
    private double[] toDoubleArr( Vec3 input ) {
        return new double[] { input.getX(), input.getY(), input.getZ() };
    }
    
    //TODO return different value in collection.  Needs to have location and style.
    private Collection<Collection<Vec3>> getAnchorLines(Set<SegmentIndex> tracedPathPairs) {
        Collection<Collection<Vec3>> rtnVal = new ArrayList<>();
        int currentIndex = 0;
        Set<SegmentIndex> existing = new HashSet<>();
        Map<Anchor,Integer> anchorToIndex = new HashMap<>();
        for (Anchor anchor: skeleton.getAnchors()) {
            anchorToIndex.put(anchor, currentIndex++);
        }
        for (Anchor anchor: skeleton.getAnchors()) {
            NeuronStyle style = neuronStyleModel.get(anchor.getNeuronID());
            if ( style == null ) {
                style = NeuronStyle.getStyleForNeuron(anchor.getNeuronID());
            }
                    
            Integer i1 = anchorToIndex.get( anchor );
            for (Anchor neighbor: anchor.getNeighbors() ) {
                SegmentIndex pathTestInx = new SegmentIndex( anchor.getGuid(), neighbor.getGuid() );
                // Avoid any segments with auto trace.
                if (! tracedPathPairs.contains( pathTestInx )  &&  ! existing.contains( pathTestInx ) ) {
                    Integer i2 = anchorToIndex.get(neighbor);
                    // Only proceed in ascending order to avoid dups.
                    if (i1 < i2) {
                        Collection<Vec3> coords = new ArrayList<>();
                        coords.add( anchor.getLocation() );
                        coords.add( neighbor.getLocation() );
                        rtnVal.add( coords );
                    }
                    existing.add( pathTestInx );
                }
            }
        }
        
        return rtnVal;
    }
    
}
