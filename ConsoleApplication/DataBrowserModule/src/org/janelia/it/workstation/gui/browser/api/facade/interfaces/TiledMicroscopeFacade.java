package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;

/**
 * Implementations provide access to tiled microscope domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface TiledMicroscopeFacade {
	
    public List<String> getTmSamplePaths() throws Exception;

    public void updateSamplePaths(List<String> paths) throws Exception;
    
    public Collection<TmSample> getTmSamples() throws Exception;

    public TmSample create(TmSample tmSample) throws Exception;

    public TmSample update(TmSample tmSample) throws Exception;

    public void remove(TmSample tmSample) throws Exception;

    public Collection<TmWorkspace> getTmWorkspaces() throws Exception;

    public TmWorkspace create(TmWorkspace tmWorkspace) throws Exception;

    public TmWorkspace update(TmWorkspace tmWorkspace) throws Exception;

    public void remove(TmWorkspace tmWorkspace) throws Exception;

    public Collection<Pair<TmNeuronMetadata,InputStream>> getWorkspaceNeuronPairs(Long workspaceId) throws Exception;

    public TmNeuronMetadata createMetadata(TmNeuronMetadata neuronMetadata) throws Exception;

    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception;

    public TmNeuronMetadata updateMetadata(TmNeuronMetadata neuronMetadata) throws Exception;

    public TmNeuronMetadata update(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception;

    public void remove(TmNeuronMetadata neuronMetadata) throws Exception;
}
