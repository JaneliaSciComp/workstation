package org.janelia.horta.nodes;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Observer;

import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.VantageInterface;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 * @author Christopher Bruns
 */
public class BasicHortaWorkspace implements HortaMetaWorkspace {
    private final VantageInterface vantage;
    private final ComposableObservable changeObservable = new ComposableObservable();
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private final Collection<NeuronSet> neuronLists = new HashSet<>();
    private final Collection<TmObjectMesh> meshActors = new HashSet<>();
    private TmNeuronTagMap tagMeta = null;
    private TmSample sample;

    public BasicHortaWorkspace(VantageInterface vantage) {
        this.vantage = vantage;
    }

    @Override
    public VantageInterface getVantage()
    {
        return vantage;
    }

    @Override
    public void setChanged()
    {
        changeObservable.setChanged();
    }

    @Override
    public void notifyObservers()
    {
        changeObservable.notifyObservers();
    }

    @Override
    public void notifyObservers(Object arg) {
        changeObservable.notifyObservers(arg);
    }
    
    @Override
    public void addObserver(Observer observer)
    {
        changeObservable.addObserver(observer);
    }

    @Override
    public void deleteObserver(Observer observer)
    {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers()
    {
        changeObservable.deleteObservers();
    }

    @Override
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(Color color)
    {
        if (backgroundColor.equals(color)) return;
        backgroundColor = color;
        setChanged();
    }
    
    @Override
    public TmNeuronTagMap getTagMetadata()
    {
        return tagMeta;
    }

    @Override
    public void setTagMetadata(TmNeuronTagMap meta)
    {
       tagMeta = meta;
    }

    @Override
    public Collection<NeuronSet> getNeuronSets()
    {
        return neuronLists;
    }

    @Override
    public boolean hasChanged()
    {
        return changeObservable.hasChanged();
    }

    @Override
    public TmSample getSample() {
        return sample;
    }

    @Override
    public void setSample(TmSample sample) {
        this.sample = sample;
    }
    
    public Collection<TmObjectMesh> getMeshActors() {
        return meshActors;
    }
    
    public void addMeshActors(TmObjectMesh actor) {
        meshActors.add(actor);
    }
}
