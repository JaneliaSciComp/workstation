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
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoder;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoderProvider;
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
public class NeuronTraceVtxAttribMgr implements VertexAttributeSourceI, IdCoderProvider {
    public static final String ID_VTX_ATTRIB = "c_id";
    private static final double MANUAL_SEGMENT_RADIUS = 6;
    private static final int MANUAL_SEGMENT_POLYGON_SIDES = 8;
    private static final double TRACED_SEGMENT_RADIUS = 8;
    private static final int TRACED_SEGMENT_POLYGON_SIDES = 10;
    public static final double ANNO_RADIUS = TRACED_SEGMENT_RADIUS * 4;
    private static final int ANNO_POLYGON_SIDES = 12;
    
    private static final int CURRENT_SELECTION_POLYGON_SIDES = 24;
    public static final double CURRENT_SELECTION_RADIUS = TRACED_SEGMENT_RADIUS * 10;

    private static final float[] UNFINISHED_ANNO_COLOR = new float[]{
        1.0f, 0.6f, 0.6f
    };
    
    private static final float[] SPECIAL_ANNO_COLOR = new float[]{
        250.0f/256.0f, 218.0f/256.0f, 94.0f/256.0f
    };
    
    private static final float[] BRANCH_ANNO_COLOR = new float[]{
        0.6f, 0.6f, 1.0f
    };

    private static final float[] CURRENT_SELECTION_COLOR = new float[]{
        1.0f, 1.0f, 1.0f
    };

    private final Logger log = LoggerFactory.getLogger(NeuronTraceVtxAttribMgr.class);

    // The skeleton and neuron styles constitute the 'model' to be studied
    // in creating the 3D representation of annotations.
    private AnnotationSkeletonDataSourceI dataSource;
    
	private boolean hasDisplayable = false;	
    // The sources and render buffers will be created, based on the contents
    // of the 'model'.
    private final List<TriangleSource> triangleSources = new ArrayList<>();
    private final Map<Long, RenderBuffersBean> renderIdToBuffers = new HashMap<>();
    
    private double annoRadius = ANNO_RADIUS;
    private double currentSelectionRadius = CURRENT_SELECTION_RADIUS;
    private IdCoder idCoder;
    
    /**
     * @return the annoRadius
     */
    public double getAnnoRadius() {
        return annoRadius;
    }

    /**
     * @param annoRadius the annoRadius to set
     */
    public void setAnnoRadius(double annoRadius) {
        this.annoRadius = annoRadius;
    }

    /**
     * @return the currentSelectionRadius
     */
    public double getCurrentSelectionRadius() {
        return currentSelectionRadius;
    }

    /**
     * @param currentSelectionRadius the currentSelectionRadius to set
     */
    public void setCurrentSelectionRadius(double currentSelectionRadius) {
        this.currentSelectionRadius = currentSelectionRadius;
    }
    
    /**
     * Return this so that callers can encode/decode ids in same fashion
     * as was done here.
     * 
     * @return configured coder, or null.
     */
    @Override
    public IdCoder getIdCoder() {
        return idCoder;
    }

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
		if (hasDisplayable) {
			handleRenderBuffers(triangleSources, renderIdToBuffers);
		}
		else {
			triangleSources.clear();
			renderIdToBuffers.clear();
		}
        //exportVertices(new File("/Users/fosterl/"), "NeuronTraceVtxAttribMgr_Test");
        
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
        this.execute();  // Ensure contents.
        exportVertices(outputLocation, filenamePrefix, triangleSources, id);
        this.close(); // Ensure cleanup.
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
    public void handleRenderBuffers(
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
        VertexNumberGenerator vertexNumberGenerator = new VertexNumberGenerator();
        // Make triangle sources.
        LineEnclosureFactory lineEnclosureFactory = new LineEnclosureFactory(TRACED_SEGMENT_POLYGON_SIDES, TRACED_SEGMENT_RADIUS, vertexNumberGenerator);
        PointEnclosureFactory pointEnclosureFactory = new PointEnclosureFactory(ANNO_POLYGON_SIDES, ANNO_RADIUS, vertexNumberGenerator);
        
        Set<SegmentIndex> voxelPathAnchorPairs = new HashSet<>();
        TileFormat tileFormat = dataSource.getTileFormat();

        // Look at 'interesting annotations'.  What can be presented there?
        pointEnclosureFactory.setCharacteristics(ANNO_POLYGON_SIDES, getAnnoRadius());
        calculateInterestingAnnotationVertices(tileFormat, pointEnclosureFactory);
        
        // The current selection.
        pointEnclosureFactory.setCharacteristics(CURRENT_SELECTION_POLYGON_SIDES, getCurrentSelectionRadius());
        calculateCurrentSelectionVertices(pointEnclosureFactory);

        // Get the auto-traced segments.
        lineEnclosureFactory.setCharacteristics(TRACED_SEGMENT_POLYGON_SIDES, TRACED_SEGMENT_RADIUS);
        calculateTracedSegmentVertices(voxelPathAnchorPairs, tileFormat, lineEnclosureFactory);
        
        // Now get the lines.
        lineEnclosureFactory.setCharacteristics(MANUAL_SEGMENT_POLYGON_SIDES, MANUAL_SEGMENT_RADIUS);
        calculateManualLineVertices(voxelPathAnchorPairs, lineEnclosureFactory);                        

		// TESTING 
		//calculateAngleIllustrativeVertices(lineEnclosureFactory);
        log.info("Number of vertices is {}.", vertexNumberGenerator.getCurrentVertex());

		if (vertexNumberGenerator.hasVertices()) {
			hasDisplayable = true;
		}
		else {
			hasDisplayable = false;
		}
        
		// Add each factory to the collection.
        triangleSources.add(pointEnclosureFactory);
		triangleSources.add(lineEnclosureFactory);
    }

	@SuppressWarnings("unused")
	private void calculateAngleIllustrativeVertices( LineEnclosureFactory lef ) {
		int polygonRadius = 50;
		lef.setCharacteristics(5, polygonRadius);
    	double r = 500.0;

		// Numbers should be around 74000, 49000, and 19000
		double[] startingCoords = new double[] { 74000, 47000, 19000 };
		double[] endingCoords = new double[3];

		float[] extraColor = new float[]{1.0f, 0.0f, 1.0f};
		xyPlaneFan(endingCoords, startingCoords, r, lef, extraColor);
		yzPlaneFan(startingCoords, endingCoords, r, lef, extraColor);
		xzPlaneFan(startingCoords, endingCoords, r, lef, extraColor);
		toeOutFan(startingCoords, endingCoords, r, lef, new float[] { 0.9f, 1.0f, 0.9f });
		rollingFence(startingCoords, r, lef, polygonRadius);
	
	}
	
	/** This illustrates the flattening effect seen in some of the renderings. */
	private void rollingFence(double[] startingCoords, double r, LineEnclosureFactory lef, double increment) {
		double[] endingCoords = new double[3];
		double[] newStart = new double[3];
		newStart[1] = startingCoords[1] = 44000;
		newStart[2] = startingCoords[2];
		endingCoords[1] = startingCoords[1] + r;
		double zIncrement = 10.0;
		for (int i = 0; i < 30; i++) {
			newStart[0] = startingCoords[0] + 3 * i * increment;
			endingCoords[0] = newStart[0] - (i%2 == 0 ? 30.0 : 0.0);
			endingCoords[2] = startingCoords[2] + zIncrement * i;  // Smaller increment.
			lef.addEnclosure(newStart, endingCoords, BRANCH_ANNO_COLOR);
			endingCoords[2] = startingCoords[2] - zIncrement * i;   // Smaller increment.
			lef.addEnclosure(newStart, endingCoords, UNFINISHED_ANNO_COLOR);
		}
	}
	
	private void toeOutFan(double[] startingCoords, double[] endingCoords, double r, LineEnclosureFactory lef, float[] extraColor) {
		// Expect values to fan from left to top.
		endingCoords[0] = startingCoords[0] - r;
		endingCoords[1] = startingCoords[1] = 50000;
		endingCoords[2] = startingCoords[2] - 70.0; // Toe-out.
		for (double theta = 0.01; theta < Math.PI / 2.0; theta += 0.2 ) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, BRANCH_ANNO_COLOR);
		}
		for (double theta = 3 * Math.PI / 2.0; theta < 2 * Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, UNFINISHED_ANNO_COLOR);
		}
		for (double theta = Math.PI / 2.0 + 0.01; theta < Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, SPECIAL_ANNO_COLOR);
		}
		for (double theta = Math.PI; theta < 3*Math.PI / 2.0; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, extraColor);
		}
	}

	private void xzPlaneFan(double[] startingCoords, double[] endingCoords, double r, LineEnclosureFactory lef, float[] extraColor) {
		// Side-to-side
		startingCoords[1] = 48000;
		endingCoords[0] = startingCoords[0];
		endingCoords[1] = startingCoords[1];
		endingCoords[2] = startingCoords[2] - r;
		for (double theta = 0.01; theta < Math.PI / 2.0; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, BRANCH_ANNO_COLOR);
		}

		for (double theta = 3 * Math.PI / 2.0; theta < 2 * Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, UNFINISHED_ANNO_COLOR);
		}

		for (double theta = Math.PI / 2.0 + 0.01; theta < Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, SPECIAL_ANNO_COLOR);
		}

		for (double theta = Math.PI; theta < 3 * Math.PI / 2.0; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, extraColor);
		}
	}

	private void yzPlaneFan(double[] startingCoords, double[] endingCoords, double r, LineEnclosureFactory lef, float[] extraColor) {
		// Move this aside to make it easier to see everything.
		startingCoords[1] = 45000;
		endingCoords[0] = startingCoords[0];
		for (double theta = 0.01; theta < Math.PI / 2.0; theta += 0.2) {
			endingCoords[1] = startingCoords[1] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, BRANCH_ANNO_COLOR);
		}

		for (double theta = 3 * Math.PI / 2.0; theta < 2 * Math.PI; theta += 0.2) {
			endingCoords[1] = startingCoords[1] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, UNFINISHED_ANNO_COLOR);
		}

		for (double theta = Math.PI / 2.0 + 0.01; theta < Math.PI; theta += 0.2) {
			endingCoords[1] = startingCoords[1] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, SPECIAL_ANNO_COLOR);
		}

		for (double theta = Math.PI; theta < 3 * Math.PI / 2.0; theta += 0.2) {
			endingCoords[1] = startingCoords[1] + r * Math.cos(theta);
			endingCoords[2] = startingCoords[2] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, extraColor);
		}
	}

	private void xyPlaneFan(double[] endingCoords, double[] startingCoords, double r, LineEnclosureFactory lef, float[] extraColor) {
		// Expect values to fan from left to top.
		endingCoords[0] = startingCoords[0] - r;
		endingCoords[1] = startingCoords[1];
		endingCoords[2] = startingCoords[2];
		for (double theta = 0.01; theta < Math.PI / 2.0; theta += 0.2 ) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, BRANCH_ANNO_COLOR);
		}
		for (double theta = 3 * Math.PI / 2.0; theta < 2 * Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, UNFINISHED_ANNO_COLOR);
		}
		for (double theta = Math.PI / 2.0 + 0.01; theta < Math.PI; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, SPECIAL_ANNO_COLOR);
		}
		for (double theta = Math.PI; theta < 3*Math.PI / 2.0; theta += 0.2) {
			endingCoords[0] = startingCoords[0] + r * Math.cos(theta);
			endingCoords[1] = startingCoords[1] + r * Math.sin(theta);
			lef.addEnclosure(startingCoords, endingCoords, extraColor);
		}
	}
	
    protected void calculateInterestingAnnotationVertices(TileFormat tileFormat, PointEnclosureFactory interestingAnnotationEnclosureFactory) {
        AnnotationModel annoMdl = dataSource.getAnnotationModel();
        if (annoMdl != null) {
            float[] id = null;
            FilteredAnnotationModel filteredModel = annoMdl.getFilteredAnnotationModel();
            final int rowCount = filteredModel.getRowCount();
            idCoder = new IdCoder();
            for (int i = 0; i < rowCount; i++) {
                id = idCoder.encode(i);                
                log.debug("ID={}.  Row={}.", id, i);
                InterestingAnnotation anno = filteredModel.getAnnotationAtRow(i);
                long annotationId = anno.getAnnotationID();
                TmGeoAnnotation geoAnno = annoMdl.getGeoAnnotationFromID(annotationId);
                final AnnotationGeometry geometry = anno.getGeometry();
                if (!(geometry == AnnotationGeometry.BRANCH || geometry == AnnotationGeometry.END || anno.hasNote())) {
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
                double[] point = getPoint(v);

                if (anno.hasNote()) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            point, SPECIAL_ANNO_COLOR, id
                    );
                }
                else if (geometry == AnnotationGeometry.BRANCH) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            point, BRANCH_ANNO_COLOR, id
                    );
                }
                else if (anno.getGeometry() == AnnotationGeometry.END) {
                    interestingAnnotationEnclosureFactory.addEnclosure(
                            point, UNFINISHED_ANNO_COLOR, id
                    );
                }
            }
        }
    }

    protected void calculateCurrentSelectionVertices(PointEnclosureFactory currentSelectionEnclosureFactory) {
        Anchor nextParent = dataSource.getSkeleton().getNextParent();
        if (nextParent != null) {
            Vec3 v = nextParent.getLocation();
            double[] point = getPoint(v);
            currentSelectionEnclosureFactory.addEnclosure(
                    point, CURRENT_SELECTION_COLOR, new float[] {0,0,0}
            );
            log.debug("Next parent at {},{},{}.", v.getX(), v.getY(), v.getZ());
        }
        
    }

    protected double[] getPoint(Vec3 v) {
        double[] point = new double[]{
            v.getX(), v.getY(), v.getZ()
        };
        return point;
    }

    protected double[] getEndAroundPoint(Vec3 v, double radius) {
        double[] end = new double[] {
            v.getX() + (radius),
            v.getY(),
            v.getZ()
        };
        return end;
    }

    protected double[] getStartAroundPoint(Vec3 v, double radius) {
        double[] start = new double[] {
            v.getX() - (radius),
            v.getY(),
            v.getZ()
        };
        return start;
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
                double[] currentCoords = getPoint(v);
                
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
