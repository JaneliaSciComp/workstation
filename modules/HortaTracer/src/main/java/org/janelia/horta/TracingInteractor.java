package org.janelia.horta;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import Jama.Matrix;
import org.janelia.workstation.controller.action.AnnotationSetRadiusAction;
import org.janelia.workstation.controller.action.NeuronChooseColorAction;
import org.janelia.workstation.controller.model.DefaultNeuron;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.controller.model.annotations.neuron.VertexCollectionWithNeuron;
import org.janelia.workstation.controller.model.annotations.neuron.VertexWithNeuron;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.DensityCursorActor;
import org.janelia.horta.actors.ParentVertexActor;
import org.janelia.horta.actors.SpheresActor;
import org.janelia.horta.actors.VertexHighlightActor;
import org.janelia.horta.options.TileLoadingPanel;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.SpatialIndexManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.NeuronCreateAction;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.eventbus.SelectionAnnotationEvent;
import org.janelia.workstation.controller.listener.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.openide.util.NbPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from C:\Users\brunsc\Documents\Fiji_Plugins\Auto_Trace\Semi_Trace.java
 * @author Christopher Bruns
 */
public class TracingInteractor extends MouseAdapter
        implements MouseInputListener, KeyListener,
        NeuronVertexDeletionListener, NeuronVertexCreationListener,
        NeuronVertexUpdateListener, NeuronSelectionListener, NeuronDeletionListener {
    private final VolumeProjection volumeProjection;
    private TmWorkspace metaWorkspace;
    private int snapRadius = 5; // pixels
        
    // For selection affordance
    // For GUI feedback on existing model, contains zero or one vertex.
    // Larger yellow overlay over an existing vertex under the mouse pointer.
    private TmNeuronMetadata highlightHoverModel;
    private VertexHighlightActor highlightActor;
    private SpheresActor densityCursorActor;
    private SpheresActor parentActor;
    NeuronVertexUpdateListener updateActorListener;
    private TmGeoAnnotation cachedHighlightVertex = null;
    private TmNeuronMetadata cachedHighlightNeuron = null;
    
    // For Tracing
    // Larger blueish vertex with a "P" for current selected persisted parent
    // first model is an ephemeral single vertex neuron model for display of "P"
    private TmNeuronMetadata parentVertexModel; // TODO: begin point of auto tracing
    private TmGeoAnnotation cachedParentVertex = null;
    // second model is the actual associated in-memory full parent neuron domain model
    private TmNeuronMetadata cachedParentNeuronModel = null;
    
    // White ghost vertex for potential new vertex under cursor 
    // TODO: Maybe color RED until a good path from parent is found
    // This is the new neuron cursor
    private TmNeuronMetadata densityCursorModel;
    private Vector3 cachedDensityCursorXyz = null;
    
    private TmNeuronMetadata anchorEditModel;
    
    // Data structure to help unravel serial undo/redo appendVertex commands
    // Map<List<Float>, VertexAdder> appendCommandForVertex = new HashMap<>();
    
    RadiusEstimator radiusEstimator = 
            // new TwoDimensionalRadiusEstimator(); // TODO: Use this again
            new ConstantRadiusEstimator(DefaultNeuron.radius);
    
    private StatusDisplayer.Message previousHoverMessage;
    
    private TmWorkspace defaultWorkspace = null;

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private TmGeoAnnotation cachedDragVertex;

    public void setMetaWorkspace (TmWorkspace metaWorkspace) {
        this.metaWorkspace = metaWorkspace;
    }

    public void setDefaultWorkspace(TmWorkspace defaultWorkspace) {
        if (this.defaultWorkspace == defaultWorkspace)
            return;
        this.defaultWorkspace = defaultWorkspace;
    }
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // System.out.println("KeyTyped");
        // System.out.println(keyEvent.getKeyCode()+", "+KeyEvent.VK_ESCAPE);
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        TmNeuronTagMap tagMeta = TmModelManager.getInstance().getCurrentTagMap();
        if (tagMeta != null) {
            Map<String, Map<String, Object>> groupMappings = tagMeta.getAllTagGroupMappings();
            Iterator<String> groups = tagMeta.getAllTagGroupMappings().keySet().iterator();
            while (groups.hasNext()) {
                String groupName = groups.next();
                Map<String, Object> fooMap = groupMappings.get(groupName);
                String keyMap = (String) fooMap.get("keymap");
                if (keyMap != null && keyMap.equals(KeymapUtil.getTextByKeyStroke(KeyStroke.getKeyStrokeForEvent(e)))) {
                    // toggle property
                    Boolean toggled = (Boolean) fooMap.get("toggled");
                    if (toggled == null) {
                        toggled = Boolean.FALSE;
                    }
                    toggled = !toggled;
                    fooMap.put("toggled", toggled);

                    // get all neurons in group
                    Set<TmNeuronMetadata> neurons = tagMeta.getNeurons(groupName);
                    List<TmNeuronMetadata> neuronList = new ArrayList<TmNeuronMetadata>(neurons);

                    // set toggle state
                    for (TmNeuronMetadata neuron : neurons) {
                        String property = (String) fooMap.get("toggleprop");
                        if (property != null) {
                            if (property.equals("Radius")) {
                                TmModelManager.getInstance().getCurrentView().toggleNeuronRadius(neuron.getId());
                            } else if (property.equals("Visibility")) {
                                TmModelManager.getInstance().getCurrentView().toggleHidden(neuron.getId());
                            } else if (property.equals("Background")) {
                                TmModelManager.getInstance().getCurrentView().toggleNeuronInteractable(neuron.getId());
                            } else if (property.equals("Crosscheck")) {
                                TmModelManager.getInstance().getCurrentView().toggleNeuronInteractable(neuron.getId());
                                TmModelManager.getInstance().getCurrentView().toggleNeuronRadius(neuron.getId());
                            }

                        }
                    }
                    NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(this,
                            neurons);
                    ViewerEventBus.postEvent(updateEvent);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // System.out.println("KeyReleased");
    }

    public TracingInteractor(
            VolumeProjection volumeProjection,
            UndoRedo.Manager undoRedoManager) 
    {
        this.volumeProjection = volumeProjection;
        // connectMouseToComponent();
        //this.undoRedoManager = undoRedoManager;
    }
    
    public List<GL3Actor> createActors(NeuronVertexUpdateListener listener) {
        TmWorkspace dummyWorkspace = new TmWorkspace();
        dummyWorkspace.setId(12345678L);
        highlightHoverModel = new TmNeuronMetadata(dummyWorkspace,
                "Hover highlight");
        highlightHoverModel.setId(new Long(123123123L));

        densityCursorModel = new TmNeuronMetadata(dummyWorkspace,
                "Hover density");

        densityCursorModel.setId(new Long(343445345345L));
        parentVertexModel = new TmNeuronMetadata(dummyWorkspace,
                "Selected parent vertex");
        anchorEditModel = new TmNeuronMetadata(dummyWorkspace,
                "Interactive anchor edit view");

        updateActorListener = listener;
        List<GL3Actor> result = new ArrayList<>();

        // Create actors in the order that they should be rendered;

        // Create a special single-vertex actor for highlighting the selected parent vertex
        parentVertexModel.setColor(Color.MAGENTA);
        parentActor = new ParentVertexActor(parentVertexModel);
        parentActor.setMinPixelRadius(5.0f);
        result.add(parentActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        highlightHoverModel.setColor(Color.MAGENTA);
        highlightActor = new VertexHighlightActor(highlightHoverModel);
        highlightActor.setMinPixelRadius(7.0f);
        result.add(highlightActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        densityCursorModel.setColor(Color.MAGENTA);
        densityCursorActor = new DensityCursorActor(densityCursorModel);
        densityCursorActor.setMinPixelRadius(1.0f);
        result.add(densityCursorActor);

        anchorEditModel.setColor(Color.MAGENTA);
        SpheresActor anchorEditActor = new VertexHighlightActor(anchorEditModel);
        result.add(anchorEditActor);
        
        return result;
    }
    
    // List<Float> for comparing vertex locations, even if the underlying vertex object has changed identity.
    private static List<Float> vtxKey(TmGeoAnnotation vtx) {
        float[] vtxLocation = TmModelManager.getInstance().getLocationInMicrometers(vtx.getX(),
                vtx.getY(), vtx.getZ());
        return Arrays.asList(vtxLocation[0], vtxLocation[1], vtxLocation[2]);
    }
    
    // Mouse clicking for recentering, selection, and tracing
    @Override
    public void mouseClicked(MouseEvent event) {
        // Cache the current state, in case subsequent asynchronous changes occur to hoveredDensity etc.
        InteractorContext context = createContext();

        // single click on primary (usually left) button
        if ( (event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1) )
        {
            // bare click on a point = select, always
            if (volumeProjection.isNeuronModelAt(event.getPoint()) && !event.isShiftDown() 
                && context.canSelectParent()) {
                    context.selectParent();
            } else {
                Preferences pref = NbPreferences.forModule(TileLoadingPanel.class);
                String clickMode = pref.get(TileLoadingPanel.PREFERENCE_ANNOTATIONS_CLICK_MODE,
                    TileLoadingPanel.PREFERENCE_ANNOTATIONS_CLICK_MODE_DEFAULT);

                // first, the new mode:
                if (clickMode.equals(TileLoadingPanel.CLICK_MODE_LEFT_CLICK)) {
                    if (context.canAppendVertex()) {
                        context.appendVertex();
                        return;
                    }                      
                } 
                
                // everything else requires a shift
                if (event.isShiftDown()) {
                    // another chance tadd a o point
                    if (context.canCreateRootVertex()) {
                        context.createRootVertex();
                    } else if (context.canAppendVertex()) {
                        context.appendVertex();
                    } else if (context.canMergeNeurite()) { // Maybe merge two neurons
                        context.mergeNeurites();
                        }
                    else if (context.canCreateNeuron()) {
                        context.createNeuron();
                    }
                    else {
                        // TODO: What happens if you shift click on a readOnly workspace?
                    }
                }
            }        
        }
    }
    
    @Override
    public void mouseExited(MouseEvent event) {
        // System.out.println("mouse exited");
        
        // keep showing hover highlight cursor, if dragging, even when mouse exits
        int buttonsDownMask = MouseEvent.BUTTON1_DOWN_MASK
                | MouseEvent.BUTTON2_DOWN_MASK
                | MouseEvent.BUTTON3_DOWN_MASK;
        if ( (event.getModifiersEx() & buttonsDownMask) != 0 )
            return;
        
        // Stop displaying hover highlight when cursor exits the viewport
        Collection<TmGeoAnnotation> highlightVertexes = highlightHoverModel.getGeoAnnotationMap().values();
        if (clearHighlightHoverVertex()) {
            updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                    highlightHoverModel.getGeoAnnotationMap().get(0), highlightHoverModel));
        }

    }

    public boolean selectParentVertex(TmGeoAnnotation vertex, TmNeuronMetadata neuron)
    {
        if (vertex == null) return false;
        
        if (cachedParentVertex == vertex)
            return false;
        cachedParentVertex = vertex;
        cachedParentNeuronModel = neuron;
        
        // Remove any previous vertex
        parentVertexModel.getGeoAnnotationMap().clear();
        parentVertexModel.getEdges().clear();

        // update model parent vertex
        TmModelManager.getInstance().getCurrentSelections().setCurrentVertex(vertex);
        TmModelManager.getInstance().getCurrentSelections().setCurrentNeuron(neuron);

        float[] loc = new float[]{vertex.getX().floatValue(),
                vertex.getY().floatValue(), vertex.getZ().floatValue()};

        // Create a modified vertex to represent the enlarged, highlighted actor
        TmGeoAnnotation parentVertex = new TmGeoAnnotation();
        parentVertex.setX(new Double(loc[0]));
        parentVertex.setY(new Double(loc[1]));
        parentVertex.setZ(new Double(loc[2])); // same center location as real vertex
        // Set parent actor radius X% larger than true vertex radius, and at least 2 pixels larger
        float startRadius = DefaultNeuron.radius;
        if (vertex.getRadius()!=null)
            startRadius = vertex.getRadius().floatValue();
        float parentRadius = startRadius * 1.15f;
        // plus at least 2 pixels bigger - this is handled in actor creation time
        parentVertex.setRadius(new Double(parentRadius));
        // blend neuron color with pale yellow parent color
        float parentColor[] = {0.5f, 0.6f, 1.0f, 0.5f}; // pale blue and transparent
        float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
        if (neuron != null) {
            neuronColor = neuron.getColor().getColorComponents(neuronColor);
        }
        float parentBlend = 0.75f;
        Color blendedColor = new Color(
                neuronColor[0] - parentBlend * (neuronColor[0] - parentColor[0]),
                neuronColor[1] - parentBlend * (neuronColor[1] - parentColor[1]),
                neuronColor[2] - parentBlend * (neuronColor[2] - parentColor[2]),
                parentColor[3] // always use the same alpha transparency value
                );
        parentVertexModel.setVisible(true);
        parentVertexModel.setColor(blendedColor);
        parentActor.setColor(blendedColor);
        
        parentVertexModel.addGeometricAnnotation(parentVertex);
        parentActor.updateGeometry();

        updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                parentVertexModel.getGeoAnnotationMap().get(0), parentVertexModel));
        updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                vertex, neuron));
        log.info("Horta parent vertex set");

        return true; 
    }
    
    private boolean parentIsSelected() {
         if (parentVertexModel == null) return false;
        if (parentVertexModel.getGeoAnnotationMap().isEmpty()) return false;
        return true;
    }
    
    private boolean densityIsHovered() {
        if (densityCursorModel == null) return false;
        if (densityCursorModel.getGeoAnnotationMap().isEmpty()) return false;
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearParentVertex() 
    {
        if (parentVertexModel.getGeoAnnotationMap().isEmpty()) {
            return false;
        }
        parentVertexModel.getGeoAnnotationMap().clear();
        parentVertexModel.getEdges().clear();

        cachedParentVertex = null;
        cachedParentNeuronModel = null;
        log.info("Horta parent vertex cleared");
        return true;
    }
    
    private boolean setDensityCursor(Vector3 xyz, Point screenPoint)
    {
        if (xyz == null) return false;
        
        if (cachedDensityCursorXyz == xyz)
            return false;
        cachedDensityCursorXyz = xyz;
        
        // Remove any previous vertex
        densityCursorModel.getGeoAnnotationMap().clear();
        densityCursorModel.getEdges().clear();

        // Create a modified vertex to represent the enlarged, highlighted actor
        TmGeoAnnotation densityVertex = new TmGeoAnnotation();
        densityVertex.setX(new Double(xyz.getX()));
        densityVertex.setY(new Double(xyz.getY()));
        densityVertex.setZ(new Double(xyz.getZ()));

        float radius = radiusEstimator.estimateRadius(screenPoint, volumeProjection);
        // densityVertex.setRadius(DefaultNeuron.radius); // TODO: measure radius and set this rationally
        densityVertex.setRadius(new Double(radius));

        // blend neuron color with white(?) provisional vertex color
        Color vertexColor = new Color(0.2f, 1.0f, 0.8f, 0.5f);

        densityCursorModel.setVisible(true);
        densityCursorModel.setColor(vertexColor);
        densityCursorActor.setColor(vertexColor);
        // densityCursorModel.setColor(Color.MAGENTA); // for debugging

        densityCursorModel.addGeometricAnnotation(densityVertex);
        densityCursorActor.updateGeometry();
        updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                densityCursorModel.getGeoAnnotationMap().get(0), densityCursorModel));
        return true; 
    }
    
    // Clear display of existing vertex highlight
    private boolean clearDensityCursor()
    {
        densityCursorModel.getGeoAnnotationMap().clear();
        densityCursorModel.getEdges().clear();
        cachedDensityCursorXyz = null;
        return true;
    }
    
    // GUI feedback for hovering existing vertex under cursor
    // returns true if a previously unhighlighted vertex is highlighted
    private boolean highlightHoverVertex(TmGeoAnnotation vertex, TmNeuronMetadata neuron)
    {
        if (vertex == null) return false;
        
        if (cachedHighlightVertex == vertex)
            return false; // No change
        cachedHighlightVertex = vertex;
        cachedHighlightNeuron = neuron;
        
        boolean doShowStatusMessage = true;
        if (doShowStatusMessage) {
            String message = "";
            if (neuron != null) {
                message += neuron.getName() + ": ";
            }
            message += "; Vertex Object ID = " + System.identityHashCode(vertex);
            if (message.length() > 0)
                previousHoverMessage = StatusDisplayer.getDefault().setStatusText(message, 2);            
        }
        
        boolean doShowVertexActor = true;
        if (doShowVertexActor) {
            // Remove any previous vertex
            highlightHoverModel.getGeoAnnotationMap().clear();
            highlightHoverModel.getEdges().clear();

            // Create a modified vertex to represent the enlarged, highlighted actor
            TmGeoAnnotation highlightVertex = new TmGeoAnnotation();
            highlightVertex.setX(vertex.getX());
            highlightVertex.setY(vertex.getY());
            highlightVertex.setZ(vertex.getZ()); // same center location as real vertex
            // Set highlight actor radius X% larger than true vertex radius, and at least 2 pixels larger
            float startRadius = DefaultNeuron.radius;
            if (vertex.getRadius()!=null)
                startRadius = vertex.getRadius().floatValue();
            float highlightRadius = startRadius * 1.30f;
            // we add at least 2 pixels to glyph size - this is handled in actor creation time
            highlightVertex.setRadius(new Double(highlightRadius));
            // blend neuron color with pale yellow highlight color
            float highlightColor[] = {1.0f, 1.0f, 0.6f, 0.5f}; // pale yellow and transparent
            float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
            if (neuron != null) {
                if (TmViewState.getColorForNeuron(neuron.getId())==null &&
                        neuron.getColor()==null) {
                    neuron.setColor(TmViewState.generateNewColor(neuron.getId()));
                }
                neuronColor = neuron.getColor().getColorComponents(neuronColor);
            }
            float highlightBlend = 0.75f;
            Color blendedColor = new Color(
                    neuronColor[0] - highlightBlend * (neuronColor[0] - highlightColor[0]),
                    neuronColor[1] - highlightBlend * (neuronColor[1] - highlightColor[1]),
                    neuronColor[2] - highlightBlend * (neuronColor[2] - highlightColor[2]),
                    highlightColor[3] // always use the same alpha transparency value
                    );
            highlightHoverModel.setVisible(true);
            highlightHoverModel.setColor(blendedColor);
            highlightActor.setColor(blendedColor);
            // highlightHoverModel.setColor(Color.MAGENTA); // for debugging
            
            highlightHoverModel.addGeometricAnnotation(highlightVertex);
            highlightActor.updateGeometry();
            updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                    highlightHoverModel.getGeoAnnotationMap().get(0), highlightHoverModel));
        }
        
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearHighlightHoverVertex() 
    {
        if (highlightHoverModel.getGeoAnnotationMap().isEmpty()) {
            return false;
        }
        highlightHoverModel.getGeoAnnotationMap().clear();
        previousHoverPoint = null;
        cachedHighlightVertex = null;
        cachedHighlightNeuron = null;
        return true;
    }
    
    private ConstVector3 previousDragXYZ = null;
 
    @Override
    public void mouseDragged(MouseEvent event) 
    {
        // log.info("Tracing Dragging");
        if (cachedDragVertex == null)
            return; // no vertex to drag
        if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly())
            return; // not allowed to move that vertex
        if (! SwingUtilities.isLeftMouseButton(event))
            return; // left button drag only
        if (!TmModelManager.checkOwnership(NeuronManager.getInstance().getNeuronFromNeuronID(cachedDragVertex.getNeuronId()))) {
            return;
        }

        // log.info("Dragging a vertex");       
        // Update display (only) of dragged vertex
        // Update location of hover vertex glyph
        ConstVector3 p1 = volumeProjection.worldXyzForScreenXyInPlane(event.getPoint());
        ConstVector3 dXYZ = p1.minus(previousDragXYZ);
        TmGeoAnnotation hoverVertex = highlightHoverModel.getGeoAnnotationMap().values().iterator().next();
        float[] hoverLocation = TmModelManager.getInstance().getLocationInMicrometers(hoverVertex.getX(),
                hoverVertex.getY(), hoverVertex.getZ());
        ConstVector3 oldLocation = new Vector3(hoverLocation);
        ConstVector3 newLocation = oldLocation.plus(dXYZ);
        Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();
        // Matrix m2v = MatrixUtilities.deserializeMatrix(sample.getMicronToVoxMatrix(), "micronToVoxMatrix");
        // Convert from image voxel coordinates to Cartesian micrometers
        // TmGeoAnnotation is in voxel coordinates
        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                {newLocation.getX(),},
                {newLocation.getY(),},
                {newLocation.getZ(),},
                {1.0,},});
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix voxLoc = m2v.times(micLoc);
        Vec3 voxelXyz = new Vec3(
                (float) voxLoc.get(0, 0),
                (float) voxLoc.get(1, 0),
                (float) voxLoc.get(2, 0));
        hoverVertex.setX(new Double(voxelXyz.getX()));
        hoverVertex.setY(new Double(voxelXyz.getY()));
        hoverVertex.setZ(new Double(voxelXyz.getZ()));

        // Trigger display update
        updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                highlightHoverModel.getGeoAnnotationMap().get(0), highlightHoverModel));

        // Update incremental screen location
        previousDragXYZ = p1;

        event.consume(); // Don't let OrbitPanZoomInteractor drag the world
        // log.info("Consumed tracing drag event");
    }
    
    @Override
    public void mouseMoved(MouseEvent event) 
    {
        // TODO: update old provisional tracing behavior
        moveHoverCursor(event.getPoint());
    }
    
    private ConstVector3 startingDragVertexLocation = null;
    @Override
    public void mousePressed(MouseEvent event) {
        // log.info("Begin drag");
        if ( (cachedHighlightVertex != null) && (!TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) ) {
            cachedDragVertex = cachedHighlightVertex;
            previousDragXYZ = volumeProjection.worldXyzForScreenXyInPlane(event.getPoint());
            startingDragVertexLocation = new Vector3(cachedHighlightVertex.getX(),
                    cachedHighlightVertex.getY(), cachedHighlightVertex.getZ());
            // log.info("Begin drag vertex");
        }
        else {
            cachedDragVertex = null;
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent event) {
        // log.info("End drag");
        // Maybe complete an "anchor dragged" gesture
        if ( (cachedDragVertex != null) && (!TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) ) {
            assert(cachedDragVertex.getId() == cachedHighlightVertex.getId());
            // log.info("End drag vertex");
            if (highlightHoverModel.getGeoAnnotationMap().isEmpty())
                    return;
            TmGeoAnnotation hoverVertex = highlightHoverModel.getGeoAnnotationMap().values().iterator().next();
            float[] location = new float[]{hoverVertex.getX().floatValue(),
                    hoverVertex.getY().floatValue(), hoverVertex.getZ().floatValue()};
            ConstVector3 newLocation = new Vector3(location);
            if (! newLocation.equals(startingDragVertexLocation)) 
            {
                moveAnchor(cachedHighlightNeuron, cachedHighlightVertex, newLocation);
            }
        }
        cachedDragVertex = null;
    }
    
    private boolean moveAnchor(TmNeuronMetadata neuron, TmGeoAnnotation anchor, ConstVector3 newLocation)
    {
        if (!TmModelManager.checkOwnership(neuron)) {
            JOptionPane.showMessageDialog(
                volumeProjection.getMouseableComponent(),
                "Could not gain ownership of neuron " + neuron.getName() + " owned by " + neuron.getOwnerKey(),
                "Failed to move neuron anchor",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Vec3 destination = new Vec3(
                newLocation.getX(),
                newLocation.getY(),
                newLocation.getZ());
        try {
            NeuronManager.getInstance().moveAnnotation(neuron.getId(),
                    anchor.getId(),
                    destination);
            clearParentVertex();
            selectParentVertex(anchor, neuron);
        } catch (Exception error) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "User drag-moved anchor in Horta", "Failed to move neuron anchor", JOptionPane.INFORMATION_MESSAGE);
            FrameworkAccess.handleException(error);
            return false;
        }

        return true;
    }

    // Show provisional Anchor radius and position for current mouse location
    private Point previousHoverPoint = null;
    public void moveHoverCursor(Point screenPoint) {
        if (screenPoint == previousHoverPoint)
            return; // no change from last time
        previousHoverPoint = screenPoint;
        
        // Question: Which of these three locations is the current mouse cursor in?
        //  1) upon an existing neuron model vertex
        //  2) upon a region of image density
        //  3) neither
        
        // 1) (maybe) Highlight existing neuron annotation model vertex
        Point hoverPoint = screenPoint;
        boolean foundGoodHighlightVertex = true; // start optimistic...
        TmGeoAnnotation nearestVertex = null;
        TmNeuronMetadata neuronModel = null;
        SpatialIndexManager spatialManager = TmModelManager.getInstance().getSpatialIndexManager();
        if (volumeProjection.isNeuronModelAt(hoverPoint)) { // found an existing annotation model under the cursor
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(hoverPoint);
            // NeuronVertexSpatialIndex vix = volumeProjection.getVertexIndex();

            if (defaultWorkspace != null) {
                try {
                    double[] loc = new double[]{cursorXyz.getX(), cursorXyz.getY(), cursorXyz.getZ()};
                    final boolean bSearchMultipleAnchors = true;
                    if (bSearchMultipleAnchors) {
                        // Skip hidden neurons

                        List<TmGeoAnnotation> nearestVertexes = spatialManager.getAnchorClosestToMicronLocation(loc, 3);
                        float minDistSquared = Float.MAX_VALUE;
                        for (TmGeoAnnotation v : nearestVertexes) {
                            if (v == null)
                                continue;
                            TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(v.getNeuronId());
                            if (neuron == null) 
                                continue;
                            if (!neuron.isVisible() || TmModelManager.getInstance().getCurrentView().isNonInteractable(neuron.getId())) {
                                // log.info("skipping invisible neuron");
                                continue;
                            }
                            float[] location = TmModelManager.getInstance().getLocationInMicrometers(v.getX(),
                                    v.getY(), v.getZ());
                            Vector3 xyz = new Vector3(location).minus(cursorXyz);
                            float d2 = xyz.dot(xyz);
                            // log.info("vertex distance = {} um", Math.sqrt(d2));
                            if (d2 < minDistSquared) {
                                nearestVertex = v;
                                minDistSquared = d2;
                            }
                        }
                    }
                    else {
                        nearestVertex = spatialManager.getAnchorClosestToMicronLocation(loc);
                    }
                }
                catch (UnsupportedOperationException e) {
                    FrameworkAccess.handleException(e);
                    log.warn("Workspace does not support spatial queries. Falling back on old Horta spatial index.");
                }
                if (nearestVertex != null) {
                    neuronModel = NeuronManager.getInstance().getNeuronFromNeuronID(nearestVertex.getNeuronId());
                    if (TmModelManager.getInstance().getCurrentView().isHidden(neuronModel.getId())) {
                        nearestVertex = null;
                    }
                }
            }
            else {
                log.error("No default workspace found");
            }
            
            if (nearestVertex == null) // no vertices to be found?
                foundGoodHighlightVertex = false;
            else {
                if (neuronModel == null) {
                    // TODO: Should not happen
                    log.warn("Unexpected null neuron");
                }
                // Is cursor too far from closest vertex?
                float[] location = TmModelManager.getInstance().getLocationInMicrometers(nearestVertex.getX(),
                        nearestVertex.getY(), nearestVertex.getZ());
                Vector3 vertexXyz = new Vector3(location);
                float dist = vertexXyz.distance(cursorXyz);
                float radius = DefaultNeuron.radius;
                if (nearestVertex.getRadius()!=null)
                    radius = nearestVertex.getRadius().floatValue();
                float absoluteHoverRadius = 2.50f * radius; // look this far away, relative to absolute vertex size
                // Also look a certain distance away in pixels, in case the view is way zoomed out.
                float screenHoverRadius = 10 / volumeProjection.getPixelsPerSceneUnit(); // look within at least 10 pixels
                // TODO: accept vertices within a certain number of pixels too
                if (dist > (absoluteHoverRadius + screenHoverRadius))
                    foundGoodHighlightVertex = false;
            }
        }
        if (nearestVertex == null)
            foundGoodHighlightVertex = false;
        // Make sure we can find the parent neuron, to avoid mysterious NPEs
        if (neuronModel == null)
            foundGoodHighlightVertex = false;
        if (foundGoodHighlightVertex) {
            highlightHoverVertex(nearestVertex, neuronModel);
        }
        else {
            if (clearHighlightHoverVertex())
                updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                        highlightHoverModel.getGeoAnnotationMap().get(0), highlightHoverModel));
            // Clear previous vertex message, if necessary
            if (previousHoverMessage != null) {
                previousHoverMessage.clear(2);
                previousHoverMessage = null;
            }
        }
        
        // 2) (maybe) show provisional anchor at current image density
        if ( (! foundGoodHighlightVertex) && (volumeProjection.isVolumeDensityAt(hoverPoint))) 
        {
            Point optimizedPoint = optimizePosition(hoverPoint);
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(optimizedPoint);
            Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();

            if (m2v != null) {
                Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                        {cursorXyz.getX(),},
                        {cursorXyz.getY(),},
                        {cursorXyz.getZ(),},
                        {1.0,},});
                // NeuronVertex API requires coordinates in micrometers
                Jama.Matrix voxLoc = m2v.times(micLoc);
                Vector3 newLoc = new Vector3(voxLoc.get(0, 0), voxLoc.get(1, 0),
                        voxLoc.get(2, 0));
                setDensityCursor(newLoc, optimizedPoint);
            }
        }
        else {
            if (clearDensityCursor())
                updateActorListener.neuronVertexUpdated(new VertexWithNeuron(
                        densityCursorModel.getGeoAnnotationMap().get(0), densityCursorModel));
        }
        
        // TODO: build up from current parent toward current mouse position
    }

    private Point optimizePosition(Point screenPoint) {
        // TODO - this method is pretty crude; but maybe good enough?
        screenPoint = optimizeX(screenPoint, getSnapRadius());
        screenPoint = optimizeY(screenPoint, getSnapRadius());
        // Refine to local maximum
        Point prevPoint = new Point(screenPoint.x, screenPoint.y);
        int stepCount = 0;
        do {
            stepCount += 1;
            if (stepCount > 5) 
                break;
            screenPoint = optimizeX(screenPoint, 2);
            screenPoint = optimizeY(screenPoint, 2);
        } while (! prevPoint.equals(screenPoint));
        return screenPoint;
    }
    
    // Find a nearby brighter pixel
    private Point optimizeX(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, -1, 0, max);
        Point p2 = searchOptimizeBrightness(p, 1, 0, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point optimizeY(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, 0, -1, max);
        Point p2 = searchOptimizeBrightness(p, 0, 1, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point searchOptimizeBrightness(Point point, int dx, int dy, int max_step) {
        double i_orig = volumeProjection.getIntensity(point);
        double best_i = i_orig;
        int best_t = 0;
        double max_drop = 10 + 0.05 * i_orig;
        for (int t = 1; t <= max_step; ++t) {
            double i_test = volumeProjection.getIntensity(new Point(
                    point.x + t * dx, 
                    point.y + t * dy));
            if (i_test > best_i) {
                best_i = i_test;
                best_t = t;
            } else if (i_test < (best_i - max_drop)) {
                break; // Don't cross valleys
            }
        }
        if (best_t == 0) {
            return point;
        } else {
            return new Point(point.x + best_t * dx, point.y + best_t * dy);
        }
    }

    @Override
    public void neuronVertexesDeleted(VertexCollectionWithNeuron doomed) {
        // Possibly remove parent glyph, if vertex is deleted
        if (cachedParentVertex == null) 
            return; // no sense checking 
        Set<TmGeoAnnotation> allDoomedVertexes = new HashSet<>(); // remember them efficiently, for later checking reparent
        List<Float> pvXyz = vtxKey(cachedParentVertex);
        for (TmGeoAnnotation doomedVertex : doomed.vertexes) {
            allDoomedVertexes.add(doomedVertex);
            if (vtxKey(doomedVertex).equals(pvXyz)) {
                clearParentVertex();
                break;
            }
        }
    }

    @Override
    public void neuronVertexCreated(VertexWithNeuron vertexWithNeuron) {
        selectParentVertex(vertexWithNeuron.vertex, vertexWithNeuron.neuron);
    }

    public InteractorContext createContext() {
        return new InteractorContext();
    }

    @Override
    public void neuronVertexUpdated(VertexWithNeuron vertexWithNeuron) {
        if (cachedParentVertex == null) {
            return; // no sense checking now
        }
        if (cachedParentVertex == vertexWithNeuron.vertex) {
            // To keep things simple, just delete and recreate
            clearParentVertex();
            selectParentVertex(vertexWithNeuron.vertex, vertexWithNeuron.neuron);
        }
    }

    public int getSnapRadius() {
        return snapRadius;
    }

    public void setSnapRadius(int snapRadius) {
        this.snapRadius = snapRadius;
    }

    @Override
    public void vertexSelected(TmGeoAnnotation selectedVertex) {
        selectParentVertex(selectedVertex, NeuronManager.getInstance().getNeuronFromNeuronID(selectedVertex.getNeuronId()));
    }

    @Override
    public void neuronSelected(TmNeuronMetadata selectedNeuron) {
        InteractorContext context = createContext();
        context.selectNeuron(selectedNeuron);
    }

    @Override
    public void neuronsDeleted(Collection<TmNeuronMetadata> deletedNeurons) {
        clearParentVertex();
    }


    // Cached state of interactor at one moment in time, so hovered density, for
    // example, does not go stale before the user selects a menu option.
    public class InteractorContext
    {
        private final TmGeoAnnotation hoveredVertex;
        private final TmNeuronMetadata hoveredNeuron;
        private final TmGeoAnnotation parentVertex;
        private final TmNeuronMetadata parentNeuron;
        private final TmGeoAnnotation densityVertex;
        
        private InteractorContext() {
            // Persist interactor state at moment of contruction
            // Cache the current state, in case asynchronous changes occur
            hoveredVertex = cachedHighlightVertex;
            hoveredNeuron = cachedHighlightNeuron;
            boolean haveParent = parentIsSelected();
            parentVertex = haveParent ? cachedParentVertex : null;
            parentNeuron = haveParent ? cachedParentNeuronModel : null;
            densityVertex = densityIsHovered() ? densityCursorModel.getGeoAnnotationMap().values().iterator().next() : null;
        }
        
        public TmGeoAnnotation getCurrentParentAnchor() {
            return parentVertex;
        }
        
        public TmGeoAnnotation getHighlightedAnchor() {
            return hoveredVertex;
        }
        
        public TmGeoAnnotation getHighlightedDensity() {
            return densityVertex;
        }
        
        public boolean canAppendVertex() {
            if (parentVertex == null) return false;
            if (densityVertex == null) return false;
            if (parentNeuron == null) return false;
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }

        public boolean canCreateRootVertex() {
            TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
            if (currNeuron == null) return false;
            if (currNeuron.getAnnotationCount()>0) return false;
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }
        
        public boolean appendVertex() {
            long beginAppendTime = System.nanoTime();
            if (! canAppendVertex())
                return false;
            if (!TmModelManager.checkOwnership(
                    NeuronManager.getInstance().getNeuronFromNeuronID(parentVertex.getNeuronId()))) {
                return false;
            }
            try {
                if (densityVertex!=null) {
                    Vec3 newLoc = new Vec3(densityVertex.getX(), densityVertex.getY(),
                            densityVertex.getZ());
                    TmGeoAnnotation newAnn = NeuronManager.getInstance().addChildAnnotation(parentVertex, newLoc);
                    if (newAnn!=null) {
                        selectParentVertex(newAnn, parentNeuron);
                    }
                }
            }  catch (Exception error) {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "User append vertex", "Failed to append vertex", JOptionPane.INFORMATION_MESSAGE);
                FrameworkAccess.handleException(error);
                return false;
            }
            return true;
        }

        private boolean createRootVertex() {
            try {
                TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
                if (densityVertex!=null && currNeuron!=null) {
                    Vec3 newLoc = new Vec3(densityVertex.getX(), densityVertex.getY(),
                            densityVertex.getZ());
                    TmGeoAnnotation newAnn = NeuronManager.getInstance().addRootAnnotation(currNeuron, newLoc);
                    if (newAnn!=null)
                        selectParentVertex(newAnn, currNeuron);
                }
            }  catch (Exception error) {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "User add root vertex", "Failed to create root vertex", JOptionPane.INFORMATION_MESSAGE);
                FrameworkAccess.handleException(error);
                return false;
            }
            return true;

        }
        
        public boolean canClearParent() {
            if (parentVertex == null)
                return false;
            return true;
        }
        
        public void clearParent() {
            if (! canClearParent())
                return;
            clearParentVertex();
            SelectionAnnotationEvent annEvent = new SelectionAnnotationEvent(this,
                    null, false, true);
            ViewerEventBus.postEvent(annEvent);
        }
        
        public boolean canCreateNeuron() {
            if (parentNeuron != null) return false; // must not already have an active neuron
            if (parentVertex != null) return false; // must not already have an active vertex
            if (hoveredVertex != null) return false; // must not be looking at an existing vertex
            if (densityVertex == null) return false; // must have a place to plant the seed
            if (defaultWorkspace == null) return false; // must have a workspace to place the neuron into
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }
        
        public void createNeuron() {
            if (! canCreateNeuron())
                return;

            NeuronCreateAction createAction = new NeuronCreateAction();
            if (densityVertex!=null) {
                Vec3 newLoc = new Vec3(densityVertex.getX(), densityVertex.getY(),
                        densityVertex.getZ());
                createAction.execute(true, newLoc);
            }
        }
        
        public boolean canDeleteNeuron() {
            if (hoveredVertex == null)
                return false;
            if (hoveredNeuron == null)
                return false;
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly())
                return false;
            return true;
        }
        
        public boolean canSplitNeurite() {
            if (hoveredNeuron == null) return false;
            if (hoveredVertex == null) return false;
            if (hoveredVertex.isRoot()) return false;
            // TODO: ensure the two anchors/vertices are connected
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }
        
        public boolean splitNeurite() {
            if (!canSplitNeurite())
                return false;
            if (!TmModelManager.checkOwnership(NeuronManager.getInstance().getNeuronFromNeuronID(hoveredVertex.getNeuronId()))) {
                return false;
            }

          return true;
        }

        private List<Long> findAncestors(Long neuronId, TmGeoAnnotation vertex) {
            List<Long> ancestors = new ArrayList<>();
            TmGeoAnnotation temp = vertex;
            while (temp!=null && temp.getParentId()!=temp.getNeuronId()) {
                Long parentId = temp.getParentId();
                ancestors.add(parentId);
                TmGeoAnnotation parentAnn = NeuronManager.getInstance().getGeoAnnotationFromID(neuronId, parentId);
                temp = parentAnn;
            }
            return ancestors;
        }
        
        public boolean canMergeNeurite() {
            if (parentNeuron == null) return false;
            if (hoveredVertex == null) return false;
            if (parentVertex == null) return false;
            if (hoveredVertex == parentVertex) return false;
            // cannot merge a neuron with itself
            // TODO: same neuron is OK, but not same connected "neurite"
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }
        
        public boolean mergeNeurites() {
            if (!canMergeNeurite())
                return false;
            if (hoveredNeuron == parentNeuron) {
                List<Long> targetAncestors = findAncestors(hoveredNeuron.getId(), hoveredVertex);
                List<Long> sourceAncestors = findAncestors(hoveredNeuron.getId(), parentVertex);

                Set<Long> loopVertices = new HashSet<>();
                for (Long sourceAncestor: sourceAncestors) {
                    if (targetAncestors.contains(sourceAncestor)) {
                        int sourceIndex = sourceAncestors.indexOf(sourceAncestor);
                        int targetIndex = targetAncestors.indexOf(sourceAncestor);
                        loopVertices.addAll(sourceAncestors.subList(0, sourceIndex));
                        loopVertices.addAll(targetAncestors.subList(0, targetIndex));
                        break;
                    }
                }
                if (loopVertices.size()>0) {
                    TmModelManager.getInstance().getCurrentReviews().clearLoopedAnnotations();
                    TmModelManager.getInstance().getCurrentReviews().addLoopedAnnotationsList(loopVertices);

                    NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(this,
                            Arrays.asList(hoveredNeuron));
                    ViewerEventBus.postEvent(updateEvent);
                }
                return false;
            }
            if (!TmModelManager.checkOwnership(
                    NeuronManager.getInstance().getNeuronFromNeuronID(hoveredVertex.getNeuronId()))) {
                return false;
            }       
            if (!TmModelManager.checkOwnership(
                    NeuronManager.getInstance().getNeuronFromNeuronID(parentVertex.getNeuronId()))) {
                return false;
            }

            try {
                NeuronManager.getInstance().mergeNeurite(
                        hoveredNeuron.getId(), hoveredVertex.getId(),
                        parentNeuron.getId(), parentVertex.getId());
                //selectParent();
            } catch (Exception error) {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "Merge 2 neurites in Horta", "Failed to merge neurites", JOptionPane.INFORMATION_MESSAGE);
                FrameworkAccess.handleException(error);
                return false;
            }
            return true;
        }
        
        public boolean canRecolorNeuron() {
            if (hoveredVertex == null)
                return false;
            if (hoveredNeuron == null)
                return false;
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly())
                return false;
            return true;
        }
        
        public void recolorNeuron() {
            if (! canRecolorNeuron())
                return;
            if (!TmModelManager.checkOwnership(hoveredNeuron)) {
                return;
            }
            if (hoveredNeuron == null) {
                return;
            }
            NeuronChooseColorAction action = new NeuronChooseColorAction();
            action.chooseNeuronColor(hoveredNeuron);
        }
        
        public boolean canSelectParent() {
            if (hoveredVertex == null) return false;
            if (hoveredNeuron == null) return false;
            return true;
        }

        public void selectNeuron(TmNeuronMetadata neuron) {
            cachedParentNeuronModel = neuron;
        }
        
        public void selectParent() {
            TmModelManager.getInstance().getCurrentSelections().setCurrentNeuron(hoveredNeuron);
            TmModelManager.getInstance().getCurrentSelections().setCurrentVertex(hoveredVertex);
            SelectionAnnotationEvent event = new SelectionAnnotationEvent(this,
                    Arrays.asList(new TmGeoAnnotation[]{hoveredVertex}), true, false
            );
            ViewerEventBus.postEvent(event);
            selectParentVertex(hoveredVertex, hoveredNeuron);
            NeuronManager.getInstance().updateFragsByAnnotation(hoveredNeuron.getId(), hoveredVertex.getId());

        }

        boolean canUpdateAnchorRadius() {
            if (hoveredVertex == null) return false;
            if (hoveredNeuron == null) return false;
            if (TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) return false;
            return true;
        }

        boolean updateAnchorRadius() {
            if (! canUpdateAnchorRadius()) {
                return false;
            }
            if (!TmModelManager.checkOwnership(hoveredNeuron)) {
                return false;
            }
            AnnotationSetRadiusAction vertexRadiusAction = new AnnotationSetRadiusAction();
            vertexRadiusAction.execute(hoveredNeuron.getId(), hoveredVertex.getId());
            return true;
        }

        TmNeuronMetadata getHighlightedNeuron() {
            return hoveredNeuron;   
        }
    }
    
    class RadiusDialog extends JOptionPane 
    {
        private final float radiusMin = 0.10f;
        private final float radiusMax = 10.0f;
        private final float logRadiusRange = (float)Math.log(radiusMax/radiusMin);
        
        private final JSlider slider;
        private final int sliderMax = 100;
        private final JFormattedTextField radiusField; 

        private final float initialRadius;
        private float currentRadius;
        
        private final TmGeoAnnotation anchor;
        private final TmNeuronMetadata neuron;
        
        private float radiusForSliderValue(int sliderValue) 
        {
            double intRatio = sliderValue / (double)sliderMax; // range 0-1
            double logRadius = logRadiusRange * intRatio; // range 0-logRadiusRange
            double radius = Math.exp(logRadius) * radiusMin;
            return (float)radius;
        }
        
        private int sliderValueForRadius(float radius) {
            double radiusRatio = Math.log(radius / radiusMin) / logRadiusRange; // range 0-1
            int sliderValue = (int)Math.round(radiusRatio * sliderMax);
            return sliderValue;
        }
        
        public void revertRadiusChange() {
            anchor.setRadius(new Double(initialRadius));
        }

        public void commitRadius() {
            if (currentRadius == initialRadius)
                return; // no change
            String errorMessage = "Failed to adjust anchor radius";
            try {
                log.info("User adjusted anchor radius in Horta");

                SimpleWorker setter = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        NeuronManager manager = NeuronManager.getInstance();
                        manager.updateNeuronRadius(neuron.getId(), currentRadius);
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing here
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(error);
                    }
                };
                setter.execute();

                // repaint right now...
                updateActorListener.neuronVertexUpdated(new VertexWithNeuron(anchor, neuron));
                return;
            }
            catch (Exception exc) {
                errorMessage += ":\n" + exc.getMessage();
            }
            JOptionPane.showMessageDialog(
                    volumeProjection.getMouseableComponent(),
                    errorMessage,
                    "Failed to adjust neuron anchor radius",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        public RadiusDialog(TmNeuronMetadata parentNeuron, TmGeoAnnotation vertex)
        {
            anchor = vertex;
            neuron = parentNeuron;
            initialRadius = vertex.getRadius().floatValue();
            currentRadius = initialRadius;

            // Populate a temporary little neuron model, to display current radius before committing
            // this is all disabled; wasn't working right; notably, when you add the anchor
            //  to the temp model, it resets its neuron ID to the fake temp model ID,
            //  and that breaks things; frankly not sure how/if it ever worked
            //anchorEditModel.getGeoAnnotationMap().clear();
            //anchorEditModel.addGeometricAnnotation(anchor);
            // Also add adjacent anchors TODO:

           // anchorEditModel.getVertexUpdatedObservable().setChanged();
           // anchorEditModel.getVertexUpdatedObservable().notifyObservers(new VertexWithNeuron(anchor, anchorEditModel));

            slider = new JSlider();
            slider.setMaximum(sliderMax);
            slider.setValue(sliderValueForRadius(currentRadius));

            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int sliderValue = slider.getValue();
                    int oldValue = sliderValueForRadius(currentRadius);

                    if (oldValue == sliderValue) {
                        return; // no (significant) change
                    }

                    float newRadius = radiusForSliderValue(sliderValue);

                    int sanityCheck = sliderValueForRadius(newRadius);
                    if (sliderValue != sanityCheck) {
                        log.error("Radius slider value {} diverged to {} with radius {}", sliderValue, sanityCheck, newRadius);
                    }
                    // log.info("Radius visually adjusted to {} micrometers", newRadius);

                    anchor.setRadius(new Double(currentRadius));

                    // Update the display only, by signalling change to the model, but not to the neuron (yet)
                   // anchorEditModel.getVertexUpdatedObservable().setChanged();
                    //anchorEditModel.getVertexUpdatedObservable().notifyObservers(new VertexWithNeuron(anchor, anchorEditModel));

                    currentRadius = newRadius;
                    radiusField.setValue(newRadius);
                }
            });

            NumberFormat radiusFormat = new DecimalFormat("#.##");
            radiusField = new JFormattedTextField(radiusFormat);
            radiusField.setValue(currentRadius);
            radiusField.addPropertyChangeListener("value", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    float newRadius = Float.parseFloat(radiusField.getValue().toString());
                    if (newRadius < radiusMin)
                        return;
                    if (newRadius > radiusMax)
                        return;
                    int sliderValue = sliderValueForRadius(newRadius);
                    if (sliderValue == slider.getValue())
                        return;
                    slider.setValue(sliderValue);
                }
            });

            setMessage(new Object[] {
                "Adjust Radius for Neuron Anchor",
                slider,
                radiusField
            });
            setOptionType(JOptionPane.OK_CANCEL_OPTION);

            JDialog dialog = createDialog("Adjust Radius");
            dialog.setVisible(true);

            // Turn off editing model after dialog is done displaying
            // currently all disabled
            // anchorEditModel.getGeoAnnotationMap().clear();
            // anchorEditModel.getEdges().clear();
            // anchorEditModel.getVertexesRemovedObservable().setChanged();
            // anchorEditModel.getVertexesRemovedObservable().notifyObservers(null);
        }

    }
}
