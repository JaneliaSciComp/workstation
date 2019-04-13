package org.janelia.console.viewerapi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 *
 * @author Christopher Bruns
 */
public class BasicSampleLocation implements SampleLocation
{
    private URL sampleUrl = null;
    private double focusXUm = 0;
    private double focusYUm = 0;
    private double focusZUm = 0;
    private double micrometersPerWindowHeight = 100;
    private int defaultColorChannel = 0;
    private boolean compressed = false;
    private Long neuronId = null;  // Optional
    private Long neuronVertexId = null;  // Optional
    private Long workspaceId = null;  // Optional
    private Long sampleId = null;     // Optional
    private TmSample sample = null;     // Optional
    private float[] rotation;
    private boolean interpolate = false;

    public BasicSampleLocation()
    {
    }

    @Override
    public URL getSampleUrl()
    {
        return sampleUrl;
    }

    @Override
    public void setSampleUrl(URL sampleUrl)
    {
        this.sampleUrl = sampleUrl;
    }

    @Override
    public double getFocusXUm()
    {
        return focusXUm;
    }

    public void setFocusXUm(double focusXUm)
    {
        this.focusXUm = focusXUm;
    }

    @Override
    public double getFocusYUm()
    {
        return focusYUm;
    }

    public void setFocusYUm(double focusYUm)
    {
        this.focusYUm = focusYUm;
    }

    @Override
    public double getFocusZUm()
    {
        return focusZUm;
    }

    public void setFocusZUm(double focusZUm)
    {
        this.focusZUm = focusZUm;
    }

    @Override
    public double getMicrometersPerWindowHeight()
    {
        return micrometersPerWindowHeight;
    }

    @Override
    public void setMicrometersPerWindowHeight(double micrometersPerWindowHeight)
    {
        this.micrometersPerWindowHeight = micrometersPerWindowHeight;
    }

    @Override
    public void setFocusUm(double x, double y, double z)
    {
        setFocusXUm(x);
        setFocusYUm(y);
        setFocusZUm(z);
    }

    @Override
    public int getDefaultColorChannel()
    {
        return defaultColorChannel;
    }

    @Override
    public void setDefaultColorChannel(int channelIndex)
    {
        defaultColorChannel = channelIndex;
    }

    @Override
    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * Identifier for some workspace ID that may be known, about this sample.
     * Optional, since it cannot currently be provided by all callers.
     * 
     * @return 
     */
    @Override
    public Long getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    /**
     * Identifier for some sample ID that may be known, about this sample.
     * Optional, since it cannot currently be provided by all callers.
     * 
     * @return 
     */
    @Override
    public Long getSampleId() {
        return sampleId;
    }
    
    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }
    
    public TmSample getSample() {
        return sample;
    }
    
    public void setSample(TmSample sample) {
        this.sample = sample;
    }
    
    @Override
    public float[] getRotationAsQuaternion() {
        return rotation;
    }

    @Override
    public void setRotationAsQuaternion(float[] rotation) {
        this.rotation = rotation;
    }

    @Override
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    @Override
    public boolean getInterpolate() {
        return interpolate;
    }

    @Override
    public Long getNeuronId() {
        return neuronId;
    }

    @Override
    public Long getNeuronVertexId() {
        return neuronVertexId;
    }
        
    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    public void setNeuronVertexId(Long neuronVertex) {
        this.neuronVertexId = neuronVertex;
    }
}
