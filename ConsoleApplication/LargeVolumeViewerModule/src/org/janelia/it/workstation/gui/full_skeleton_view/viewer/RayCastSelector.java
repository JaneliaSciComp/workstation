package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import Jama.Matrix;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.InterestingAnnotation;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;

/**
 *
 * @author fosterl
 */
public class RayCastSelector {
    private static final double SPHERE_R_SQUARE = 150.0 * 150.0; //NeuronTraceVtxAttribMgr.ANNO_END_RADIUS * NeuronTraceVtxAttribMgr.ANNO_END_RADIUS;
    private static final boolean DEBUG = false;
    
    private final AnnotationSkeletonDataSourceI dataSource;
    private final int width;
    private final int height;
    private final MeshViewContext context;
    
    public RayCastSelector( 
            AnnotationSkeletonDataSourceI dataSource,
            MeshViewContext context,
            int width, int height 
    ) {
        this.dataSource = dataSource;
        this.width = width;
        this.height = height;
        this.context = context;
    }
    
    public long select(int mouseX, int mouseY) {
        long rtnVal = -1;

        final AnnotationModel annoMdl = dataSource.getAnnotationModel();        
        final FilteredAnnotationModel filteredModel = annoMdl.getFilteredAnnotationModel();
        final TileFormat tileFormat = dataSource.getTileFormat();
        
        PickRayAndOrigin rayAndOrigin = getRayIntoWorld(mouseX, mouseY);
        final Vec3 rayOrigin = rayAndOrigin.pickOrigin;
        final Vec3 rayWorldVec3 = rayAndOrigin.pickRay;
        
        // Tracking progress.  What to beat.
        double nearestIntoT = Double.MAX_VALUE;
        double nearestGlancingT = Double.MAX_VALUE;
        int selectedIntoRow = -1;      // Two intersections with sphere.
        int selectedGlancingRow = -1;  // One intersection with sphere.
        
        //DEBUG System.out.println("-----------------------------------------------");
        //Vec3 smallestOMinusC = new Vec3(Double.MAX_VALUE / 10.0, 0, 0);
        //double smallestOMCMag = mag(smallestOMinusC);

        for (int i = 0; i < filteredModel.getRowCount(); i++) {
            TileFormat.MicrometerXyz sphereCenter = getCoords(filteredModel, i, annoMdl, tileFormat);
            Vec3 sphereCenterVec3 = new Vec3(sphereCenter.getX(), sphereCenter.getY(), 0.0);// sphereCenter.getZ());
            final Vec3 oMinusC = rayOrigin.minus(sphereCenterVec3);
                        
            // Setting up for quadratic equation.  a==1.
            double b = rayWorldVec3.dot(oMinusC);
            double c = oMinusC.dot(oMinusC) - SPHERE_R_SQUARE;

            // Pre-emptive bail: quick miss detection. Must be >= 0.
            double bSquareMinusC = b * b - c;
            if (bSquareMinusC >= 0) {
                //DEBUG System.out.println("O-C: " + oMinusC + "; sphereCenter: " + sphereCenterVec3 + "; cameraFocus: " + rayOrigin);
                //DEBUG System.out.println("b=" + b + "; c=" + c + "; b^2 - c=" + bSquareMinusC);
                double t = solveForT( b, bSquareMinusC );
                if (t >= 0  &&  bSquareMinusC == 0  &&  t < nearestGlancingT) {
                    selectedGlancingRow = i;
                }
                else if (t >= 0  &&  t < nearestIntoT) {
                    selectedIntoRow = i;
                }
            }
        }
        
        //System.out.println("Smallest (O-C): " + smallestOMinusC);
        System.out.println("RayOrigin: " + rayOrigin);
        
        // Have found what user selected.
        if (selectedIntoRow > -1) {
            rtnVal = filteredModel.getAnnotationAtRow(selectedIntoRow).getAnnotationID();
        }
        else if (selectedGlancingRow > -1) {
            rtnVal = filteredModel.getAnnotationAtRow(selectedGlancingRow).getAnnotationID();
        }
        
        return rtnVal;
    }
    

    private TileFormat.MicrometerXyz getCoords(FilteredAnnotationModel filteredModel, int i, final AnnotationModel annoMdl, TileFormat tileFormat) {
        TileFormat.MicrometerXyz microns;
        InterestingAnnotation anno = filteredModel.getAnnotationAtRow(i);
        long annotationId = anno.getAnnotationID();
        TmGeoAnnotation geoAnno = annoMdl.getGeoAnnotationFromID(annotationId);
        microns = tileFormat.micrometerXyzForVoxelXyz(
                new TileFormat.VoxelXyz(
                        geoAnno.getX().intValue(),
                        geoAnno.getY().intValue(),
                        geoAnno.getZ().intValue()
                ),
                CoordinateAxis.Z
        );
        
        return microns;
    }
    
    private double solveForT(double b, double bSquareMinusC) {
        double t = -1.0;
        if (bSquareMinusC == 0) {
            t = -b;
        }
        else {
            double sroot = Math.sqrt(bSquareMinusC);
            double soln1 = Math.abs(-b + sroot);
            double soln2 = Math.abs(-b - sroot);
            if (soln1 < soln2) {
                t = soln1;
            }
            else {
                t = soln2;
            }
        }
        return t;
    }
    
    /**
     * This "ray into the world", is a directional vector from the eye into the
     * 'world', corresponding to where we clicked.
     *
     * ^
     * O + Dt
     *
     * It can be used in eq O + D-hat * t Here D-hat is our directional, and O
     * is the camera vector, and t is a distance. We use this equation in
     * combination with a sphere equation to see if there is a value t that
     * solves for it.
     *
     * @return d-hat value.
     */
    private PickRayAndOrigin getRayIntoWorld(int mouseX, int mouseY) {
        Matrix rayWorld;
        // For technique,
        // @see http://antongerdelan.net/opengl/raycasting.html
        double x = (2.0 * mouseX) / width - 1.0;
        double y = 1.0 - (2.0f * mouseY) / height;
        double[] rayClip = new double[]{
            x, y, -1.0, 1.0
        };
        
        // Getting inverses of matrices.
        float[] transposedProjectionMatrix = new float[context.getPerspectiveMatrix().length];
        ViewMatrixSupport.transposeM(transposedProjectionMatrix, 0, context.getPerspectiveMatrix(), 0);
        float[] invertedProjectionMatrix = new float[context.getPerspectiveMatrix().length];
        ViewMatrixSupport.invertM(invertedProjectionMatrix, 0, transposedProjectionMatrix, 0);
        float[] transposedMM = new float[context.getModelViewMatrix().length];
        ViewMatrixSupport.transposeM(transposedMM, 0, context.getModelViewMatrix(), 0);
        float[] invertedModelViewMatrix = new float[context.getModelViewMatrix().length];
        ViewMatrixSupport.invertM(invertedModelViewMatrix, 0, transposedMM, 0);
        // Moving to JAMA representation.
        Matrix invProjJama = toJamaMatrix(invertedProjectionMatrix);
        dumpJama(invProjJama, "Inverted Projection");
        Matrix invMMJama = toJamaMatrix(invertedModelViewMatrix);
        dumpJama(invMMJama, "Inverted ModelView");
        Matrix rayClipJama = toJamaVector(rayClip);
        dumpJama(rayClipJama, "Ray Clip");
        // Multiplying the inverted projection matrix by the ray clip,
        // and adjusting as a ray (not a point).
        Matrix rayEye = invProjJama.times(rayClipJama);
        rayEye.set(2, 0, -1.0);
        rayEye.set(3, 0, 0.0);
        rayWorld = invMMJama.times(rayEye);
        rayWorld.set(3, 0, 0.0);  // Again, trunc away W.
        dumpJama(rayWorld, "Ray-World");
        double normalizer = rayWorld.normF();
        Matrix normalizedRayWorld = rayWorld.times(1.0 / normalizer);
        //dumpJama(normalizedRayWorld, "Normalized Ray-World");

        PickRayAndOrigin rtnVal = new PickRayAndOrigin();
        rtnVal.pickRay = new Vec3(
                normalizedRayWorld.get(0, 0),
                normalizedRayWorld.get(1, 0),
                normalizedRayWorld.get(2, 0)
        );

        rtnVal.pickOrigin = worldFromPixel(mouseX, mouseY);

        return rtnVal;
    }

    protected void dumpJama(Matrix rayWorld, String label) {
        if (! DEBUG) {
            return;
        }
        System.out.println(label + "=================");
        for (int row = 0; row < rayWorld.getRowDimension(); row++) {
            for (int col = 0; col < rayWorld.getColumnDimension(); col++) {
                System.out.print(rayWorld.get(row, col) + " ");
            }
            System.out.println();
        }
    }

    /**
     * Turns 1D matrix into 2D. Also handles any transpose details.
     */
    private Matrix toJamaMatrix(float[] onedMatrix) {
        //float[] transpose = new float[ onedMatrix.length ];
        //ViewMatrixSupport.transposeM(transpose, 0, onedMatrix, 0);
        double[][] jamaRaw = new double[][]{
            new double[4],
            new double[4],
            new double[4],
            new double[4]
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                jamaRaw[row][col] = onedMatrix[ row * 4 + col];
            }
        }

        Matrix matrix = new Matrix(jamaRaw);
        return matrix;
    }

    private Matrix toJamaVector(double[] onedMatrix) {
        double[][] jamaRaw = new double[][]{
            new double[1], new double[1], new double[1], new double[1]
        };
        Matrix matrix = new Matrix(jamaRaw);
        for (int row = 0; row < 4; row++) {
            jamaRaw[row][0] = onedMatrix[ row];
        }

        return matrix;
    }

    private Matrix toJamaVector(Vec3 vec) {
        double[][] jamaRaw = new double[][]{
            new double[1], new double[1], new double[1], new double[1]
        };
        Matrix matrix = new Matrix(jamaRaw);
        for (int row = 0; row < 3; row++) {
            jamaRaw[row][0] = vec.get(row);
        }

        return matrix;
    }
    
    private Vec3 worldFromPixel(int x, int y) {
        // Initialize to screen space position
        Vec3 result = new Vec3(x, y, 0);
        final Camera3d camera3d = context.getCamera3d();
        final Vec3 cameraFocus = camera3d.getFocus();
        // Normalize to screen viewport center
        //       First step is pro-forma.
        result = result.minus(new Vec3(0, 0, 0)); // origin for viewport.
        result = result.minus(new Vec3(width / 2.0, height / 2.0, 0)); // center
        // Convert from pixel units to world units
        result = result.times(1.0 / camera3d.getPixelsPerSceneUnit());
        // Apply viewer orientation, e.g. X/Y/Z orthogonal viewers
        Rotation3d viewerInGround = camera3d.getRotation();
        result = viewerInGround.times(result);
		// TODO - apply rotation, but only for rotatable viewers, UNLIKE large volume viewer
        // Apply camera focus
        result = result.plus(cameraFocus);
        result.setZ(0.0);
        // System.out.println(pixel + ", " + getCamera().getFocus() + ", " + result);
        return result;
    }

    private static class PickRayAndOrigin {
        public Vec3 pickRay;
        public Vec3 pickOrigin;
    }

}
