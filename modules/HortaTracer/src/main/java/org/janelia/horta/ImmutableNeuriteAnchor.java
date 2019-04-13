package org.janelia.horta;

import org.janelia.geometry3d.ConstVector3;

/**
 * TODO: obsolete ImmutableNeuriteAnchor class in favor of NeuronVertex API
 * @author Christopher Bruns
 */
public interface ImmutableNeuriteAnchor
{
    public float distanceSquared(ConstVector3 rhs);
    
    public ConstVector3 getLocationUm();

    public double getIntensity();

    public float getRadiusUm();
}
