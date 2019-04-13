package org.janelia.console.viewerapi.model;

/**
 * Method in common to all neuron reconstructions
 * @author Christopher Bruns
 */
public interface NeuronVertex
{
    float[] getLocation();
    void setLocation(float x, float y, float z);
    
    // NeuronVertex getParentVertex(); // can be null
    // void setParentVertex(NeuronVertex parent);
    
    boolean hasRadius();
    float getRadius();
    void setRadius(float radius);
}
