package org.janelia.console.viewerapi.model;

import java.util.Collection;
import java.util.List;

import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.openide.awt.UndoRedo;

/**
 *
 * @author Christopher Bruns
 */
public interface NeuronSet extends Collection<NeuronModel>
{
    boolean isReadOnly();
    
    // getMembershipChangeObservable() signals when whole neurons are added or removed from the collection
    ObservableInterface getMembershipChangeObservable();
    ObservableInterface getNameChangeObservable();
    String getName();
    NeuronModel createNeuron(String initialNeuronName);

    boolean isSpatialIndexValid();
    List<NeuronVertex> getAnchorsInMicronArea(double[] p1, double[] p2);
    List<NeuronVertex> getAnchorClosestToMicronLocation(double[] micronXYZ, int n);
    NeuronVertex getAnchorClosestToMicronLocation(double[] micronXYZ);

    NeuronModel getNeuronForAnchor(NeuronVertex anchor);
    TmGeoAnnotation getAnnotationForAnchor(NeuronVertex anchor);

    UndoRedo.Manager getUndoRedo(); // Manage edit operations per neuron collection
    // Sometimes there is one anchor selected for edit operations
    NeuronVertex getPrimaryAnchor(); // can be null
    void setPrimaryAnchor(NeuronVertex anchor); // set to null to clear
    ObservableInterface getPrimaryAnchorObservable();
    
    NeuronModel getNeuronByGuid(Long guid);
    void addEditNote(NeuronVertex anchor);
    void addTraceEndNote(NeuronVertex anchor);
    void addUnique1Note(NeuronVertex anchor);
    void addUnique2Note(NeuronVertex anchor);
    void changeNeuronVisibility(List<TmNeuronMetadata> neuron, boolean visibility);
    void changeNeuronNonInteractable (List<TmNeuronMetadata> neuron, boolean interactable);
    void changeNeuronUserToggleRadius (List<TmNeuronMetadata> neuronList, boolean userToggleRadius);
    void changeNeuronUserProperties (List<TmNeuronMetadata> neuronList, List<String> properties, boolean toggle);
    void changeNeuronOwnership (Long neuronId);
    void addObjectMesh(TmObjectMesh mesh);
    void removeObjectMesh(String meshName);    
    void updateObjectMeshName(String oldName, String updatedName);
    void setSelectMode(boolean select);
    void selectVertex (NeuronVertex anchor);

    void startUpMessagingDiagnostics(NeuronModel neuron);
}
