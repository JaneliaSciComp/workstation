package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Color;
import java.awt.HeadlessException;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnnotationObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NotesUpdateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.PathTraceListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmAnchoredPathListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.UpdateAnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.VolumeLoadListener;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;

/**
 * Interface for annotation controllers.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotationManager extends UpdateAnchorListener, PathTraceListener, VolumeLoadListener  {

    /**
     * Must be called after initialization, to establish two-way communication between the AnnotationManager and the UI.
     * @param quadViewUi
     * TODO: this completely breaks encapsulation, and should be eliminated
     */
    void setQuadViewUi(QuadViewUi quadViewUi);

    /**
     * Must be called after initialization, to establish two-way communication between the AnnotationManager and the
     * translator. 
     * TODO: this completely breaks encapsulation, and should be eliminated
     * @param lvvTranslator
     */
    void setLvvTranslator(LargeVolumeViewerTranslator lvvTranslator);
    
    /**
     * Returns the tile format for the current sample.
     */
    TileFormat getTileFormat();

    /**
     * Get the object that was initially loaded by the user. 
     */
    DomainObject getInitialObject();

    /**
     * Given the initial object, load the TmSample.
     * @return future result
     */
    SimpleListenableFuture<TmSample> loadSample();
    
    /**
     * Load the sample and any other annotations, as specified by the initial object.
     * @return future result
     */
    SimpleListenableFuture<Void> load();

    public void loadComplete();
    
    /**
     * Clean up operations called before this object is discarded.
     */
    void close();

    /**
     * Can the annotations be changed at all, or is this a read-only presentation?
     * @return true if edits are allowed
     */
    boolean editsAllowed();
    
    void deleteSubtreeRequested(Anchor anchor);

    void splitAnchorRequested(Anchor anchor);

    void rerootNeuriteRequested(Anchor anchor);

    void splitNeuriteRequested(Anchor anchor);

    void deleteLinkRequested(Anchor anchor);

    void addEditNoteRequested(Anchor anchor);

    void editNeuronTagsRequested(Anchor anchor);

    void anchorAdded(AnchorSeed seed);

    void moveAnchor(Anchor anchor);

    /**
     * move the annotation with the input ID to the input location.
     * Activity-logged by caller.
     */
    void moveAnnotation(Long neuronID, Long annotationID, Vec3 micronLocation);

    /**
     * merge the two neurites to which the two annotations belong.
     * Activity-logged by caller.
     *
     * @param sourceAnnotationID
     * @param targetAnnotationID
     */
    void mergeNeurite(Long sourceNeuronID, Long sourceAnnotationID, Long targetNeuronID, Long targetAnnotationID);

    void smartMergeNeuriteRequested(Anchor sourceAnchor, Anchor targetAnchor);

    void moveNeuriteRequested(Anchor anchor);

    /**
     * add an anchored path; not much to check, as the UI needs to check it even
     * before the request gets here
     */
    void addAnchoredPath(Long neuronID, TmAnchoredPathEndpoints endpoints, List<List<Integer>> points);

    /**
     * pop a dialog to add, edit, or delete note at the given annotation
     */
    void addEditNote(Long neuronID, Long annotationID);

    /**
     * returns the note attached to a given annotation; returns empty
     * string if there is no note; you'll get an exception if the
     * annotation ID doesn't exist
     */
    String getNote(Long neuronID, Long annotationID);

    /**
     * create a new neuron in the current workspace, prompting for name
     */
    void createNeuron();

    void deleteCurrentNeuron();

    /**
     * rename the currently selected neuron
     */
    void renameNeuron();

    void saveWorkspaceCopy();

    /**
     * find an annotation relative to the input annotation in
     * a given direction; to be used in navigation along the skeleton;
     * see code for exact behavior
     */
    Long relativeAnnotation(Long neuronId, Long annID, AnnotationNavigationDirection direction);

    /**
     * pop a dialog to choose neuron style; three variants work together to operate
     * from different input sources
     */
    void chooseNeuronColor();

    void chooseNeuronStyle(Anchor anchor);

    void chooseNeuronStyle(TmNeuronMetadata neuron);

    void setAllNeuronVisibility(boolean visibility);

    SimpleListenableFuture<Void> setBulkNeuronVisibility(Collection<TmNeuronMetadata> neuronList, boolean visibility);

    /** 
     * Hide others. Hide all then show current; this is purely a convenience function.
     */
    void hideUnselectedNeurons();

    void toggleSelectedNeurons();

    /**
     * as with chooseNeuronStyle, multiple versions allow for multiple entry points
     */
    void setCurrentNeuronVisibility(boolean visibility);

    void setNeuronVisibility(Anchor anchor, boolean visibility);

    NeuronStyle getNeuronStyle(TmNeuronMetadata neuron);

    void setNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style);

    void saveQuadViewColorModel();

    void saveColorModel3d(ImageColorModel colorModel);

    void setAutomaticRefinement(boolean state);

    void setAutomaticTracing(boolean state);

    void tracePathToParent(PathTraceToParentRequest request);

    void exportNeuronsAsSWC(File swcFile, int downsampleModulo, Collection<TmNeuronMetadata> neurons);

    void importSWCFiles(List<File> swcFiles);

    TmWorkspace getCurrentWorkspace();

    TmAnnotationObject getCurrentAnnotationObject();
    
    Collection<TmNeuronMetadata> getNeuronList();

    TmNeuronMetadata getCurrentNeuron();

    NeuronSet getNeuronSet();

    Set<String> getAvailableNeuronTags();

    TmNeuronMetadata getNeuronFromNeuronID(Long neuronID);

    boolean hasNeuronTag(TmNeuronMetadata neuron, String tag);

    Set<String> getNeuronTags(TmNeuronMetadata neuron);

    void setNeuronColors(List<TmNeuronMetadata> neurons, Color color) throws Exception;

    void addNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception;

    void addNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception;

    void removeNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception;

    void removeNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception;

    void clearNeuronTags(TmNeuronMetadata neuron) throws Exception;

    TmSample getCurrentSample();

    DomainObjectSelectionModel getSelectionModel();

    FilteredAnnotationModel getFilteredAnnotationModel();

    void addTmGeoAnnotationModListener(TmGeoAnnotationModListener listener);

    void removeTmGeoAnnotationModListener(TmGeoAnnotationModListener listener);

    void addTmAnchoredPathListener(TmAnchoredPathListener listener);

    void addGlobalAnnotationListener(GlobalAnnotationListener listener);

    void removeGlobalAnnotationListener(GlobalAnnotationListener listener);

    void setViewStateListener(ViewStateListener listener);

    void setNotesUpdateListener(NotesUpdateListener notesUpdateListener);

    void saveWorkspace(TmWorkspace workspace) throws Exception;

    void setSWCDataConverter(SWCDataConverter converter);

    void selectNeuron(TmNeuronMetadata neuron);

    void createWorkspace();

    TmGeoAnnotation getGeoAnnotationFromID(Long neuronID, Long annotationID);

    TmGeoAnnotation getNeuriteRootAnnotation(TmGeoAnnotation annotation);

    TmNeuronMetadata createNeuron(String name) throws Exception;

    TmGeoAnnotation addRootAnnotation(TmNeuronMetadata neuron, Vec3 xyz) throws Exception;

    TmGeoAnnotation addChildAnnotation(TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception;

    void updateAnnotationRadius(Long neuronID, Long annotationID, float radius) throws Exception;

    void moveNeurite(TmGeoAnnotation tmGeoAnnotation, TmNeuronMetadata neuron) throws Exception;

    void splitNeurite(Long neuronId, Long newRootId) throws Exception;
    
    void deleteLink(TmGeoAnnotation link) throws Exception;

    void fireSpatialIndexReady(TmAnnotationObject workspace);

    void generateRandomNeurons(Integer neuronCount, Integer meanPointsPerNeuron, BoundingBox3d boundingBox, Float branchProbability);
    
    /**
     * Convenience method, to cut down on redundant code.
     *
     * @param message passed as message param
     * @param title passed as title param.
     * @throws HeadlessException by called methods.
     */
    void presentError(String message, String title) throws HeadlessException;

}