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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.shared.mesh_loader.BufferPackager;
import org.janelia.it.jacs.shared.mesh_loader.NormalCompositor;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;
import org.janelia.it.jacs.shared.mesh_loader.wavefront_obj.OBJWriter;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationGeometry;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.InterestingAnnotation;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.workstation.tracing.VoxelPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle any mesh's vertex attributes.
 * 
 * @author fosterl
 */
public class NeuronTraceVtxAttribMgr implements VertexAttributeSourceI {
    private static final double MANUAL_SEGMENT_RADIUS = 2;
    private static final int MANUAL_SEGMENT_POLYGON_SIDES = 8;
    private static final double TRACED_SEGMENT_RADIUS = 3;
    private static final int TRACED_SEGMENT_POLYGON_SIDES = 10;
    private static final double ANNO_END_RADIUS = TRACED_SEGMENT_RADIUS * 8;
    private static final int ANNO_END_POLYGON_SIDES = 4;

    private static final float[] UNFINISHED_ANNO_COLOR = new float[]{
        1.0f, 1.0f, 1.0f
    };
    
    private static final float[] SPECIAL_ANNO_COLOR = new float[]{
        250.0f/256.0f, 218.0f/256.0f, 94.0f/256.0f
    };
    
    private static final float[] BRANCH_ANNO_COLOR = new float[]{
        0.1f, 1.0f, 1.0f
    };

    // The skeleton and neuron styles constitute the 'model' to be studied
    // in creating the 3D representation of annotations.
    private AnnotationSkeletonDataSourceI dataSource;

    // The sources and render buffers will be created, based on the contents
    // of the 'model'.
    private final List<TriangleSource> triangleSources = new ArrayList<>();
    private final Map<Long, RenderBuffersBean> renderIdToBuffers = new HashMap<>();
    
    private final Logger log = LoggerFactory.getLogger( NeuronTraceVtxAttribMgr.class );
    
    /**
     * Call this whenever something in the 'model' has been changed.
     * 
     * @return sources created.
     * @throws Exception 
     */
    @Override
    public synchronized List<TriangleSource> execute() throws Exception {
        // Can re-execute this.
        renderIdToBuffers.clear();
        triangleSources.clear();
    
        Skeleton skeleton = dataSource.getSkeleton();
        NeuronStyleModel neuronStyleModel = dataSource.getNeuronStyleModel();
        if ( dataSource == null  ||  skeleton == null  ||  neuronStyleModel == null ) {
            if (dataSource == null) {
                log.warn("No data source set");
            }
            if (skeleton == null) {
                log.warn("No skeleton in data source");
            }
            if ( neuronStyleModel == null ) {
                log.warn("No neuron style model.");
            }
            throw new Exception("Please set all model information before execution.");
        }
        
        createVertices();
        populateNormals(triangleSources, renderIdToBuffers);
        exportVertices(new File("/Users/fosterl/"), "NeuronTraceVtxAttribMgr_Test");
        
        return triangleSources;
    }

    @Override
    public synchronized Map<Long, RenderBuffersBean> getRenderIdToBuffers() {
        return renderIdToBuffers;
    }

    @Override
    public synchronized void close() {
        renderIdToBuffers.clear();
        triangleSources.clear();
    }

    @Override
    public void exportVertices(File outputLocation, String filenamePrefix) throws Exception {
        Skeleton skeleton = dataSource.getSkeleton();
        if (skeleton == null || skeleton.getAnchors().isEmpty()) {
            log.warn("Nothing to export");
            return;
        }
        Anchor anAnchor = skeleton.getAnchors().iterator().next();
        Long id = anAnchor.getNeuronID();
        exportVertices(outputLocation, filenamePrefix, triangleSources, id);
    }
    
    /**
     * This is a test-accessible function which does not rely on the skeleton
     * as a data source.
     * 
     * @param outputLocation where to place file.
     * @param filenamePrefix prefix for its name.
     * @param triangleSources iterate for OBJ content.
     * @param id also in the name
     * @throws Exception thrown by called methods.
     */
    public void exportVertices(File outputLocation, String filenamePrefix, List<TriangleSource> triangleSources, Long id) throws Exception {
        OBJWriter objWriter = new OBJWriter();
        for (TriangleSource triangleSource : triangleSources) {
            objWriter.writeVertices(
                    outputLocation,
                    filenamePrefix,
                    OBJWriter.FILE_SUFFIX,
                    id,
                    triangleSource
            );
        }
    }

    public void setDataSource(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * @return the skeleton
     */
    public Skeleton getSkeleton() {
        if (dataSource == null) {
            return null;
        }
        return dataSource.getSkeleton();
    }

    /**
     * @return the neuronStyleModel
     */
    public NeuronStyleModel getNeuronStyleModel() {
        return dataSource.getNeuronStyleModel();

    }

    /**
     * Test-accessible method which does not rely on skeleton data.  Will
     * calculate the normal vectors for the triangles in the triangle source.
     * 
     * @param triangleSources iterated for normals data.
     * @param renderIdToBuffers populated with normals data.
     */
    public void populateNormals(
            List<TriangleSource> triangleSources,
            Map<Long, RenderBuffersBean> renderIdToBuffers
    ) {
        Long sourceNumber = 0L;
        BufferPackager packager = new BufferPackager();
        NormalCompositor normalCompositor = new NormalCompositor();
        for (TriangleSource factory : triangleSources) {
            // Now have a full complement of triangles and vertices.  For this source, can traverse the
            // vertices, making a "composite normal" based on the normals of all entangling triangles.
            normalCompositor.combineCustomNormals(factory);
            RenderBuffersBean rbb = renderIdToBuffers.get(sourceNumber);
            if ( rbb == null ) {
                rbb = new RenderBuffersBean();
                renderIdToBuffers.put( sourceNumber, rbb );
            }
            rbb.setAttributesBuffer(packager.getVertexAttributes(factory));
            rbb.setIndexBuffer(packager.getIndices(factory));
            sourceNumber++;
        }
    }

    /**
     * Here is where the 'model' is transformed into vertices and render
     * buffers.
     * 
     * @throws Exception 
     */
    private synchronized void createVertices() throws Exception {
        // Make triangle sources.
        LineEnclosureFactory lineEnclosureFactory = new LineEnclosureFactory(TRACED_SEGMENT_POLYGON_SIDES, TRACED_SEGMENT_RADIUS);
        
        Set<SegmentIndex> voxelPathAnchorPairs = new HashSet<>();
        TileFormat tileFormat = dataSource.getTileFormat();

        int currentVertexNumber = 0;
        lineEnclosureFactory.setCurrentVertexNumber(
                currentVertexNumber
        );
        // Look at 'interesting annotations'.  What can be presented there?
        lineEnclosureFactory.setCharacteristics(ANNO_END_POLYGON_SIDES, ANNO_END_RADIUS);
        calculateInterestingAnnotationVertices(tileFormat, lineEnclosureFactory);

        // Get the auto-traced segments.
        lineEnclosureFactory.setCharacteristics(TRACED_SEGMENT_POLYGON_SIDES, TRACED_SEGMENT_RADIUS);
        calculateTracedSegmentVertices(voxelPathAnchorPairs, tileFormat, lineEnclosureFactory);
        
        // Now get the lines.
        lineEnclosureFactory.setCharacteristics(MANUAL_SEGMENT_POLYGON_SIDES, MANUAL_SEGMENT_RADIUS);
        calculateManualLineVertices(voxelPathAnchorPairs, lineEnclosureFactory);
        
        log.info("Number of vertices is {}.", lineEnclosureFactory.getCurrentVertexNumber());
        
		// Add each factory to the collection.
		triangleSources.add(lineEnclosureFactory);
    }

    protected void calculateInterestingAnnotationVertices(TileFormat tileFormat, LineEnclosureFactory interestingAnnotationEnclosureFactory) {
        AnnotationModel annoMdl = dataSource.getAnnotationModel();
        if (annoMdl != null) {
            FilteredAnnotationModel filteredModel = annoMdl.getFilteredAnnotationModel();
            for (int i = 0; i < filteredModel.getRowCount(); i++) {
                InterestingAnnotation anno = filteredModel.getAnnotationAtRow(i);
                long annotationId = anno.getAnnotationID();
                TmGeoAnnotation geoAnno = annoMdl.getGeoAnnotationFromID(annotationId);
                final AnnotationGeometry geometry = anno.getGeometry();
                if (!(geometry == AnnotationGeometry.BRANCH || geometry == AnnotationGeometry.END || geometry == AnnotationGeometry.LINK)) {
                    continue;
                }

                TileFormat.MicrometerXyz microns = tileFormat.micrometerXyzForVoxelXyz(
                        new TileFormat.VoxelXyz(
                                geoAnno.getX().intValue(),
                                geoAnno.getY().intValue(),
                                geoAnno.getZ().intValue()
                        ),
                        CoordinateAxis.Z
                );
                Vec3 v = tileFormat.centerJustifyMicrometerCoordsAsVec3(microns);
                double[] start = new double[] {
                    v.getX(),
                    v.getY(),
                    v.getZ() - (ANNO_END_RADIUS)
                };
                double[] end = new double[] {
                    v.getX(),
                    v.getY(),
                    v.getZ() + (ANNO_END_RADIUS)
                };

                if (geometry == AnnotationGeometry.BRANCH) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            start, end, BRANCH_ANNO_COLOR
                    );
                }
                else if (anno.getGeometry() == AnnotationGeometry.END) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            start, end, UNFINISHED_ANNO_COLOR
                    );
                }
                else if (anno.getGeometry() == AnnotationGeometry.LINK) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            start, end, SPECIAL_ANNO_COLOR
                    );
                }
            }
        }
    }

    protected void calculateManualLineVertices(Set<SegmentIndex> voxelPathAnchorPairs, LineEnclosureFactory manualSegmentEnclosureFactory) {
        Collection<AnchorLinesReturn> anchorLines = getAnchorLines(voxelPathAnchorPairs);
        for ( AnchorLinesReturn anchorLine: anchorLines ) {
            manualSegmentEnclosureFactory.addEnclosure(
                    anchorLine.getStart(),
                    anchorLine.getEnd(),
                    anchorLine.getStyle().getColorAsFloatArray()
            );
            
            //break; // TEMP: add only a single enclosure, so dump is easier to understand.
        }
    }

    protected void calculateTracedSegmentVertices(Set<SegmentIndex> voxelPathAnchorPairs, TileFormat tileFormat, LineEnclosureFactory tracedSegmentEnclosureFactory) {
        // Iterate over all the traced segments, and add enclosures for each.
        for ( AnchoredVoxelPath voxelPath: getSkeleton().getTracedSegments() ) {
            final SegmentIndex segmentIndex = voxelPath.getSegmentIndex();
            if (segmentIndex == null) {
                continue;
            }
            voxelPathAnchorPairs.add( segmentIndex );
            // need neuron ID; get it from the anchor at either end of the
            //  traced path; if there isn't an anchor, just move on--that
            //  path is also gone (happens when neurons deleted, merged)
            //  [from earlier code]
            Long anchorGuid = segmentIndex.getAnchor1Guid();
            Anchor anchor = getSkeleton().getAnchorByID(anchorGuid);
            if ( anchor == null ) {
                continue;
            }
            Long neuronId = anchor.getNeuronID();
            NeuronStyle style = getNeuronStyle(neuronId);
            final float[] colorAsFloatArray = style.getColorAsFloatArray();
            
            double[] previousCoords = null;
            VoxelPosition previousVoxelPos = null;
            for ( VoxelPosition voxelPos: voxelPath.getPath() ) {
                TileFormat.MicrometerXyz microns = tileFormat.micrometerXyzForVoxelXyz(
                        new TileFormat.VoxelXyz(
                                voxelPos.getX(),
                                voxelPos.getY(),
                                voxelPos.getZ()
                        ),
                        CoordinateAxis.Z
                );
                Vec3 v = tileFormat.centerJustifyMicrometerCoordsAsVec3(microns);
                double[] currentCoords = new double[] {
                    v.getX(), v.getY(), v.getZ()
                };
                
                if ( previousCoords != null ) {
                    int coordsAdded = tracedSegmentEnclosureFactory.addEnclosure(
                            previousCoords, currentCoords, colorAsFloatArray);
                    if (coordsAdded == 0) {
                        if (previousVoxelPos != null) {
                            log.info("Encountered identical endpoints: " + fmtVoxelPos(previousVoxelPos) + ":" + fmtVoxelPos(voxelPos) +
                                    ".  Encountered identical converted coords: " + fmtCoords(previousCoords) + ":" + fmtCoords(currentCoords) +
                                    ".  Found in segment index: " + voxelPath.getSegmentIndex() + ", and in neuron " + neuronId + ".");
                        }
                    }
                }
                previousCoords = currentCoords;
                previousVoxelPos = voxelPos;
            }
        }
    }
    
    private String fmtVoxelPos( VoxelPosition pos ) {
        return String.format("[%d,%d,%d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private String fmtCoords(double[] coords) {
        return String.format("[%f,%f,%f]", coords[0], coords[1], coords[2]);
    }

    private double[] toDoubleArr( Vec3 input ) {
        return new double[] { input.getX(), input.getY(), input.getZ() };
    }
    
    private Collection<AnchorLinesReturn> getAnchorLines(Set<SegmentIndex> tracedPathPairs) {
        Collection<AnchorLinesReturn> rtnVal = new ArrayList<>();
        int currentIndex = 0;
        Set<SegmentIndex> existing = new HashSet<>();
        Map<Anchor,Integer> anchorToIndex = new HashMap<>();
        for (Anchor anchor: getSkeleton().getAnchors()) {
            anchorToIndex.put(anchor, currentIndex++);
        }
        int tracedPairSkipCount = 0;
        for (Anchor anchor: getSkeleton().getAnchors()) {
            NeuronStyle style = getNeuronStyle(anchor.getNeuronID());
                    
            Integer i1 = anchorToIndex.get( anchor );
            for (Anchor neighbor: anchor.getNeighbors() ) {
                SegmentIndex pathTestInx = new SegmentIndex( anchor.getGuid(), neighbor.getGuid() );
                // Avoid any segments with auto trace.
                if (! tracedPathPairs.contains( pathTestInx )  &&  ! existing.contains( pathTestInx ) ) {
                    Integer i2 = anchorToIndex.get(neighbor);
                    // Only proceed in ascending order to avoid dups.
                    if (i1 < i2) {
                        AnchorLinesReturn traceRtn = new AnchorLinesReturn();
                        traceRtn.setStart( toDoubleArr(anchor.getLocation()) );
                        traceRtn.setEnd( toDoubleArr(neighbor.getLocation()) );
                        traceRtn.setStyle( style );
                        rtnVal.add( traceRtn );
                        log.debug("Adding anchor line: " + i1 + "->" + i2);
                    }
                    else {
                        log.debug("Skipped one line--i2 >= i1.  i1=" + i1 +", i2=" + i2);                        
                    }
                    existing.add( pathTestInx );
                }
                else {
                    if (tracedPathPairs.contains(pathTestInx)) {
                        log.debug("Skipped one line--in traced pairs: " + pathTestInx);
                        tracedPairSkipCount ++;
                    }
                    else {
                        log.debug("Skipped one line--existing: " + pathTestInx);
                    }
                }
            }
        }
        
        log.debug("Skipped " + tracedPairSkipCount + " for being in a traced pair.");
        return rtnVal;
    }

    private NeuronStyle getNeuronStyle(Long neuronId) {
        NeuronStyle style = dataSource.getNeuronStyleModel().get(neuronId);
        if (style == null) {
            style = NeuronStyle.getStyleForNeuron(neuronId);
        }
        return style;
    }

    private static class AnchorLinesReturn {
        private double[] start;
        private double[] end;
        private NeuronStyle style;

        /**
         * @return the start
         */
        public double[] getStart() {
            return start;
        }

        /**
         * @param start the start to set
         */
        public void setStart(double[] start) {
            this.start = start;
        }

        /**
         * @return the end
         */
        public double[] getEnd() {
            return end;
        }

        /**
         * @param end the end to set
         */
        public void setEnd(double[] end) {
            this.end = end;
        }

        /**
         * @return the style
         */
        public NeuronStyle getStyle() {
            return style;
        }

        /**
         * @param style the style to set
         */
        public void setStyle(NeuronStyle style) {
            this.style = style;
        }
        
    }
}
