package org.janelia.workstation.controller.scripts.spatialfilter;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;
import java.util.Set;
import java.util.Map;

/**
 * Applies a filter to the neurons in the workspace to filter them down so the application
 * can scale more easily to handle larger numbers of automatically generated neuron fragments.
 * The filter, if applied, works seamlessly with the TmModelManipulator to transparently work
 * with the rest of the application to hide fragments that are not seen.
 * <p>
 * When a user performs a change to the workspace (adding/modifying/deleting neuron),
 * this can affect the neuron filter so different fragments become visible or become hidden
 * Since LVV/Horta remain ignorant of the master list of neuron fragments, we need to
 * individually notify LVV/Horta listeners of all the changes.
 */
public interface NeuronSpatialFilter {
    public String getLabel();
    public Set<Long> filterNeurons();
    public void initFilter(Collection<TmNeuronMetadata> neuronList);
    public NeuronUpdates deleteNeuron(TmNeuronMetadata neuron);
    public NeuronUpdates addNeuron(TmNeuronMetadata neuron);
    public NeuronUpdates updateNeuron(TmNeuronMetadata neuron);
    public NeuronUpdates selectVertex(TmGeoAnnotation annotation);
    public Map<String,Object> getFilterOptions();
    public Map<String, String> getFilterOptionsUnits();
    public void setFilterOptions(Map<String, Object> filterOptions);
    public void clearFilter();
    public int getNumTotalNeurons();
}


