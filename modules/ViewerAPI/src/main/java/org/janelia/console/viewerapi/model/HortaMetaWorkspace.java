package org.janelia.console.viewerapi.model;

import java.awt.Color;
import java.util.Collection;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 *
 * @author Christopher Bruns
 */
public interface HortaMetaWorkspace extends ObservableInterface
{
    VantageInterface getVantage();

    Collection<NeuronSet> getNeuronSets();
    TmNeuronTagMap getTagMetadata();
    void setTagMetadata(TmNeuronTagMap tagMeta);
    TmSample getSample();
    void setSample(TmSample sample);
    Color getBackgroundColor();
    void setBackgroundColor(Color color);   
    Collection<TmObjectMesh> getMeshActors();
    void addMeshActors(TmObjectMesh actor);
}
