/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.DirectionalReferenceAxesActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.AxesActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh.NeuronTraceVtxAttribMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveViewer;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveRenderer;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveRenderer.OcclusiveVolumeModel;
import org.janelia.it.workstation.gui.viewer3d.ResetPositionerI;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.AttributeManagerBufferUploader;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.MeshDrawActorConfigurator;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoderProvider;
import org.janelia.it.workstation.gui.viewer3d.picking.RenderedIdPicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel holds all relevant components for showing the skeleton of
 * an annotation made in the Large Volume Viewer.
 * 
 * @author fosterl
 */
public class AnnotationSkeletonPanel extends JPanel {
    private final AnnotationSkeletonDataSourceI dataSource;
    private OcclusiveViewer viewer;
    private MeshViewContext context;
    private OcclusiveRenderer.OcclusiveVolumeModel occlusiveVolumeModel;

    private UniqueColorSelector ucSelector;
	private RenderedIdPicker picker;
    private SkeletonActor linesDrawActor;
    
    private Collection<GLActor> coreActors = new ArrayList<>();
    private Collection<GLActor> fixedFunctionActors = new ArrayList<>();
    private boolean meshMode = true;
    private boolean forceReset = true;
    
    public AnnotationSkeletonPanel(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
        this.setLayout(new BorderLayout());
    }
    
    /** T=draw with mesh; F=draw with lines. */
    public void setMeshMode(boolean isMesh) {
        meshMode = isMesh;
    }
    
    public boolean isMeshMode() {
        return meshMode;
    }
    
    /**
     * Force a complete positional reset on the viewer.  This should definitely
     * be done at creation of the panel.
     * @param flag 
     */
    public void setForceReset(boolean flag) {
        this.forceReset = flag;
    }
    
    public void establish3D() {
        if (occlusiveVolumeModel == null) {
            occlusiveVolumeModel = new OcclusiveVolumeModel();
            context = occlusiveVolumeModel;
        }

        if (viewer == null  &&  dataSource.getSkeleton() != null  &&  dataSource.getSkeleton().getTileFormat() != null) {
            // Establish the lines-ish version of the skele viewer.
            // This one also acts as a collector of data.
            linesDrawActor = new SkeletonActor();
            linesDrawActor.setParentAnchorImageName( SkeletonActor.ParentAnchorImage.LARGE );
            linesDrawActor.getModel().setNeuronStyleModel( dataSource.getNeuronStyleModel() );
            linesDrawActor.setShowOnlyParentAnchors( true );
            linesDrawActor.getModel().setAnchorsVisible(true);
            linesDrawActor.getModel().setFocusOnNextParent(true);
            TileFormat tileFormat = dataSource.getSkeleton().getTileFormat();
            final BoundingBox3d boundingBox = tileFormat.calcBoundingBox();
            Vec3 yExtender = new Vec3(0, 0.75 * boundingBox.getHeight(), 0);
            linesDrawActor.getBoundingBox3d().setMax( boundingBox.getMax().plus( yExtender ) );
            linesDrawActor.getBoundingBox3d().setMin( boundingBox.getMin().minus( yExtender ) );

            // Establish the renderer.
            OcclusiveRenderer renderer = new OcclusiveRenderer( occlusiveVolumeModel );
            final SkeletalBoundsResetPositioner skeletalBoundsResetPositioner = new SkeletalBoundsResetPositioner(dataSource.getSkeleton());
            renderer.setResetPositioner( skeletalBoundsResetPositioner );

            // Establish the viewer.
            viewer = new OcclusiveViewer(renderer);            
            skeletalBoundsResetPositioner.setViewer(viewer);
            skeletalBoundsResetPositioner.setRenderer(renderer);
            skeletalBoundsResetPositioner.setActor(linesDrawActor);
            viewer.setVolumeModel(occlusiveVolumeModel);
            double[] voxelMicronD = tileFormat.getVoxelMicrometers();
            float[] voxelMicronF = new float[] {
                (float)voxelMicronD[0], 
                (float)voxelMicronD[1],
                (float)voxelMicronD[2]
            };
            context.setVoxelMicrometers(voxelMicronF);
            final Camera3d rendererCamera = context.getCamera3d();

            linesDrawActor.getModel().setSkeleton(dataSource.getSkeleton());
            linesDrawActor.getModel().setCamera(rendererCamera);
            linesDrawActor.getModel().setTileFormat(tileFormat);
            linesDrawActor.setRenderInterpositionMethod(
                    SkeletonActor.RenderInterpositionMethod.Occlusion
            );
            context.setBackgroundColor(new float[] {
                0.0f, 0.0f, 0.0f
            });
            // Set maximal thickness.  Z-fade is not practical for 3D rotations.
            linesDrawActor.setZThicknessInPixels( Long.MAX_VALUE );
            linesDrawActor.getModel().updateAnchors();

            DirectionalReferenceAxesActor refAxisActor = new DirectionalReferenceAxesActor(
                    new float[] { 100.0f, 100.0f, 100.0f },
                    boundingBox,
                    context,
                    DirectionalReferenceAxesActor.Placement.BOTTOM_LEFT                    
            );
            
            viewer.setResetFirstRedraw( forceReset );
            if (occlusiveVolumeModel.getCameraDepth() == null) {
                occlusiveVolumeModel.setCameraDepth(new Vec3(0,0,0));
            }
            final BoundingBox3d originalBoundingBox = tileFormat.calcBoundingBox();

            MDReturn meshDrawResults = buildMeshDrawActor( context, originalBoundingBox );
            final MeshDrawActor meshDrawActor = meshDrawResults.getActor();
            GLActor axesActor = buildOpenGLCoreAxesActor( originalBoundingBox, 1.0, context );
            GLActor ffAxesActor = buildOpenGLFixedFunctionActor( originalBoundingBox, 1.0, context);
            
            // NOTE: refAxisActor is forcing all 'conventional' actors which
            // display after it, into the same confined corner of the screen.
            // The 'meshDrawActor' may be permitted to follow it, but the
            // others may not.
            coreActors.clear();
            coreActors.add(axesActor);
            coreActors.add(meshDrawActor);
            coreActors.add(refAxisActor);
            
            fixedFunctionActors.clear();
            fixedFunctionActors.add(ffAxesActor);
            fixedFunctionActors.add(linesDrawActor);
            fixedFunctionActors.add(refAxisActor);
            
            viewer.addMenuAction(new BackgroundPickAction(viewer));
            viewer.addMenuAction(
                new ModeSwapAction(
                    this, "Mesh Draw", "Lines Draw"
                )
            );
            
            viewer.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    long selectedAnnotation = select(me.getX(), me.getY());
                    if (selectedAnnotation > 0) {
                        positionForSelection(selectedAnnotation);
                    }
                }
            });
            // Reserve the menu actions from mesh draw, until last.
            for (Action menuAction: meshDrawResults.getMenuActions()) {
                viewer.addMenuAction(menuAction);
            }
            
            // This should be done after establishing the skeleton.
            SkeletonController controller = SkeletonController.getInstance();
            controller.registerForEvents(viewer);
            controller.registerForEvents(linesDrawActor);

            // Add the initial actor list.
            if (meshMode) {
                for (GLActor actor : coreActors) {
                    viewer.addActorContinuousView(actor);
                }
            }
            else {
                for (GLActor actor: fixedFunctionActors) {
                    viewer.addActorContinuousView(actor);
                }
            }
            
            this.add(viewer, BorderLayout.CENTER);
            // This should be done only initially.
            if (forceReset) {
                forceReset = false;
                viewer.resetView();
            }

            validate();
            repaint();
            controller.registerForEvents(this);

        }
    }

    public void positionForSelection(long selectedAnnotation) {
        final SkeletonController skeletonController = SkeletonController.getInstance();
        //skeletonController.annotationSelected(selectedAnnotation);
        Vec3 focus = skeletonController.getAnnotationPosition(selectedAnnotation);
        if (focus != null) {
            skeletonController.setLVVFocus(focus);
            context.getCamera3d().setFocus(focus);
            viewer.invalidate();
            viewer.validate();
            viewer.repaint();
        }        
    }
    
    public void close() {
        if (viewer != null ) {
            viewer.clear();
            SkeletonController controller = SkeletonController.getInstance();
            controller.unregister(viewer);
            controller.unregister(linesDrawActor);
            this.remove(viewer);
            viewer = null;
        }
    }
    
    @Override
    public void paint(Graphics g) {
        establish3D();
        super.paint(g);
    }

    /**
     * Creates the actor to draw the axes on the screen. This version is
     * using all-core/generic vertex attributes.
     *
     * @param boundingBox tells extrema for the axes.
     * @param axisLengthDivisor applies downsampling abbreviation of axes.
     * @param volumeModel tells the axes actor whether its background will be white.
     * @return the actor.
     */
    public GLActor buildOpenGLFixedFunctionActor(BoundingBox3d boundingBox, double axisLengthDivisor, VolumeModel volumeModel) {
        org.janelia.it.workstation.gui.viewer3d.axes.AxesActor axes = new org.janelia.it.workstation.gui.viewer3d.axes.AxesActor();
        axes.setVolumeModel(volumeModel);
        axes.setBoundingBox(boundingBox);
        axes.setAxisLengths( boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth() );
        axes.setRenderMethod(org.janelia.it.workstation.gui.viewer3d.axes.AxesActor.RenderMethod.MESH);
        axes.setAxisLengthDivisor( axisLengthDivisor );
        axes.setFullAxes( true );
        return axes;
    }
    
    /**
     * Creates the actor to draw the axes on the screen. This version is using
     * older fixed-function-compatible calls.
     *
     * @param boundingBox tells extrema for the axes.
     * @param axisLengthDivisor applies downsampling abbreviation of axes.
     * @param volumeModel tells the axes actor whether its background will be
     * white.
     * @return the actor.
     */
    public GLActor buildOpenGLCoreAxesActor(BoundingBox3d boundingBox, double axisLengthDivisor, MeshViewContext volumeModel) {
        AxesActor axes = new AxesActor();
        axes.setMeshViewerContext(volumeModel);
        axes.setBoundingBox(boundingBox);
        axes.setAxisLengths(boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth());
        axes.setRenderMethod(AxesActor.RenderMethod.MESH);
        axes.setAxisLengthDivisor(axisLengthDivisor);
        axes.setFullAxes(true);
        return axes;
    }

    /**
     * Creates the actor to draw the "wrapped geometry" or "suit of armor"
     * rendition of the traces.
     * 
     * @param context various info used during draw.
     * @param boundingBox contains whole in-use space.
     * @return fully-configured actor, ready for drawing.
     */
    private MDReturn buildMeshDrawActor(MeshViewContext context, BoundingBox3d boundingBox) {
        MeshDrawActorConfigurator configurator = new MeshDrawActorConfigurator();
        configurator.setAxisLengths( new double[] {
            boundingBox.getMaxX() - boundingBox.getMinX(),
            boundingBox.getMaxY() - boundingBox.getMinY(),
            boundingBox.getMaxZ() - boundingBox.getMinZ() 
        } );
        
        //configurator.setAxisLengths(new double[]{100.0, 100.0, 100.0});
        configurator.setContext(context);
        configurator.setMatrixScope(MeshDrawActor.MatrixScope.LOCAL);                  
        
        final NeuronTraceVtxAttribMgr attributeManager = new NeuronTraceVtxAttribMgr(); 
        ucSelector = new UniqueColorSelector(dataSource, attributeManager, this);
        attributeManager.setDataSource(dataSource);
        configurator.setVertexAttributeManager(attributeManager);
        configurator.setColoringStrategy(MeshDrawActor.ColoringStrategy.ATTRIBUTE);
        configurator.setUseIdAttribute(true);
        configurator.setBoundingBox(boundingBox);
        // This is the testing opportunity.  This may be swapped with a different
        // buffer uploader, if doubt should arise re: the accuracy of the geometry.
        configurator.setBufferUploader(
                new AttributeManagerBufferUploader(configurator)
        );
		picker = new RenderedIdPicker((IdCoderProvider)attributeManager);
		configurator.setPicker( picker );
	    picker.setPixelListener(ucSelector);
        
        MeshDrawActor meshDraw = new MeshDrawActor(configurator);
        SkeletonController.getInstance().registerForEvents(
                meshDraw, dataSource.getAnnotationModel().getFilteredAnnotationModel()
        );
        
        MDReturn rtnVal = new MDReturn();
        rtnVal.setActor(meshDraw);
        
        rtnVal.getMenuActions().add( new SerializeWaveFrontAction(attributeManager, AnnotationSkeletonPanel.this) );
        rtnVal.getMenuActions().add( new SphereSizeAction(attributeManager, meshDraw, AnnotationSkeletonPanel.this) );
        
        return rtnVal;
    }
    
    /**
     * Attempt to select an 'interesting object' at the location given.
     * Should be either the currently (otherwise) selected anchor, or
     * an interesting annotation.
     * 
     * @param mouseX mouse pos x.
     * @param mouseY mouse pos y.
     */
    private long select(int mouseX, int mouseY) {
        long rtnVal = -1L;
        if (context != null) {            
			picker.setPickCoords(mouseX, mouseY);
            this.validate();
            this.repaint();
        }
        return rtnVal;
    }

    /** 
     * Bean to hold what is created to build mesh-draw actor, so the contents
     * do not have to become fields.
     */
    private static class MDReturn {
        private MeshDrawActor actor;
        private Collection<Action> menuActions = new ArrayList<>();

        /**
         * @return the actor
         */
        public MeshDrawActor getActor() {
            return actor;
        }

        /**
         * @param actor the actor to set
         */
        public void setActor(MeshDrawActor actor) {
            this.actor = actor;
        }

        /**
         * @return the menuActions
         */
        public Collection<Action> getMenuActions() {
            return menuActions;
        }

    }
    
    public static class SerializeWaveFrontAction extends AbstractAction {

        private NeuronTraceVtxAttribMgr attributeManager;
        private JComponent parentComponent;
        
        public SerializeWaveFrontAction( NeuronTraceVtxAttribMgr attributeManager, JComponent parentComponent ) {
            this.attributeManager = attributeManager;
            this.parentComponent = parentComponent;
            putValue(Action.NAME, "Save Skeleton Mesh to Wavefront/OBJ");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        return f.isFile();
                    }

                    @Override
                    public String getDescription() {
                        return "Please choose an output directory and filename root.  Output file will have '.obj' suffix, and will include the id of an anchor from the current workspace.";
                    }
                    
                });

                int fcOption = fileChooser.showSaveDialog(parentComponent);
                if (fcOption == JFileChooser.APPROVE_OPTION) {
                    attributeManager.exportVertices(
                            fileChooser.getSelectedFile().getParentFile(),
                            fileChooser.getSelectedFile().getName()
                    );
                }
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
        
    }
    
    public static class SphereSizeAction extends AbstractAction {

        private final NeuronTraceVtxAttribMgr attributeManager;
        private final MeshDrawActor meshDraw;
        private final AnnotationSkeletonPanel panel;

        public SphereSizeAction(
                NeuronTraceVtxAttribMgr attributeManager,
                MeshDrawActor meshDraw, 
                AnnotationSkeletonPanel panel
        ) {
            this.attributeManager = attributeManager;
            this.meshDraw = meshDraw;
            this.panel = panel;
            putValue(Action.NAME, "Toggle Landmark Sphere Sizes");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (attributeManager.getAnnoRadius() == NeuronTraceVtxAttribMgr.ANNO_RADIUS) {
                attributeManager.setAnnoRadius(NeuronTraceVtxAttribMgr.ANNO_RADIUS / 2.0);
                attributeManager.setCurrentSelectionRadius(attributeManager.getAnnoRadius() * 2.0);
            }
            else {
                attributeManager.setAnnoRadius(NeuronTraceVtxAttribMgr.ANNO_RADIUS);
                attributeManager.setCurrentSelectionRadius(NeuronTraceVtxAttribMgr.CURRENT_SELECTION_RADIUS);
            }
            // Must force re-build/re-send
            meshDraw.refresh();
            panel.validate();
            panel.repaint();
        }
        
    }
    
    public static class BackgroundPickAction extends AbstractAction {

        private OcclusiveViewer viewer;
        public BackgroundPickAction( OcclusiveViewer viewer ) {
            this.viewer = viewer;
            putValue(Action.NAME, "Background Color");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            float[] background = viewer.getVolumeModel().getBackgroundColorFArr();
            Color oldBackground = new Color( background[0], background[1], background[2] );
            Color newBackground = JColorChooser.showDialog(
                    viewer, 
                    "Annotation Skeleton Background Color", 
                    oldBackground
            );
            if ( newBackground != null ) {
                newBackground.getColorComponents(background);
                viewer.getVolumeModel().setBackgroundColor(background);
            }
        }
        
    }
    
    public static class ModeSwapAction extends AbstractAction {

        private final static String SWAP_FORMAT = "Replace %s with %s.";
        private final String firstLabel;
        private final String secondLabel;

        private boolean inCore = true;
        private AnnotationSkeletonPanel swapPanel;

        public ModeSwapAction(
                AnnotationSkeletonPanel swapPanel,
                String firstActorLabel,
                String secondActorLabel
        ) {
            this.swapPanel = swapPanel;
            this.firstLabel = String.format(SWAP_FORMAT, firstActorLabel, secondActorLabel);
            this.secondLabel = String.format(SWAP_FORMAT, secondActorLabel, firstActorLabel);
            inCore = swapPanel.isMeshMode();

            putValue(Action.NAME, inCore ? firstLabel : secondLabel);

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (inCore) {
                inCore = false;
                putValue(Action.NAME, secondLabel);
            } else {
                inCore = true;
                putValue(Action.NAME, firstLabel);
            }
            swapPanel.close();
            swapPanel.setMeshMode(inCore);
            swapPanel.establish3D();
            swapPanel.validate();
            swapPanel.repaint();
        }

    }

    public static class SkeletalBoundsResetPositioner implements ResetPositionerI {
        private Skeleton skeleton;
        private OcclusiveViewer viewer;
        private OcclusiveRenderer renderer;
        private SkeletonActor actor;
        private final Logger logger = LoggerFactory.getLogger(AnnotationSkeletonPanel.class);
        
        public SkeletalBoundsResetPositioner( Skeleton skeleton ) {
            this.skeleton = skeleton;
        }
        
        public void setViewer(OcclusiveViewer viewer) {
            this.viewer = viewer;
        }
        
        public void setRenderer(OcclusiveRenderer renderer) {
            this.renderer = renderer;
        }
        
        public void setActor(SkeletonActor actor) {
            this.actor = actor;
        }
        
        @Override
        public void resetView() {
            // Compute a bounding box out of the skeleton.
            BoundingBox3d boundingBox = getInclusiveBox();            
            viewer.getVolumeModel().getCamera3d().setFocus(boundingBox.getCenter());
            viewer.getVolumeModel().getCamera3d().resetRotation();
            renderer.resetCameraDepth(actor.getBoundingBox3d());
        }
        
        private BoundingBox3d getInclusiveBox() {
            Vec3 minimum = null;
            Vec3 maximum = null;
            Long maxXId = null;
            Long maxYId = null;
            Long maxZId = null;
            Long minXId = null;
            Long minYId = null;
            Long minZId = null;
            for (Anchor anchor : skeleton.getAnchors()) {
                if (minimum == null) {
                    minimum = anchor.getLocation().clone();
                } else {
                    minimum.setX(Math.min(minimum.getX(), anchor.getLocation().getX()));
                    minimum.setY(Math.min(minimum.getY(), anchor.getLocation().getY()));
                    minimum.setZ(Math.min(minimum.getZ(), anchor.getLocation().getZ()));
                    if (logger.isDebugEnabled()) {
                        if (minimum.getX() == anchor.getLocation().getX()) {
                            minXId = anchor.getGuid();
                        }
                        if (minimum.getY() == anchor.getLocation().getY()) {
                            minYId = anchor.getGuid();
                        }
                        if (minimum.getZ() == anchor.getLocation().getZ()) {
                            minZId = anchor.getGuid();
                        }
                    }
                }

                if (maximum == null) {
                    maximum = anchor.getLocation().clone();
                } else {
                    maximum.setX(Math.max(maximum.getX(), anchor.getLocation().getX()));
                    maximum.setY(Math.max(maximum.getY(), anchor.getLocation().getY()));
                    maximum.setZ(Math.max(maximum.getZ(), anchor.getLocation().getZ()));
                    if (logger.isDebugEnabled()) {
                        if (maximum.getX() == anchor.getLocation().getX()) {
                            maxXId = anchor.getGuid();
                        }
                        if (maximum.getY() == anchor.getLocation().getY()) {
                            maxYId = anchor.getGuid();
                        }
                        if (maximum.getZ() == anchor.getLocation().getZ()) {
                            maxZId = anchor.getGuid();
                        }
                    }
                }
            }
            BoundingBox3d box = new BoundingBox3d();
            if (minimum != null  &&  maximum != null) {
                box.include(minimum);
                box.include(maximum);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Min X/Y/Z IDs: {}/{}/{}.", minXId, minYId, minZId);
                logger.debug("Max X/Y/Z IDs: {}/{}/{}.", maxXId, maxYId, maxZId);
                logger.debug("Min X,Y,Z: {}.", minimum);
                logger.debug("Max X,Y,Z: {}.", maximum);
            }
            return box;
        }
        
    }
    
}
