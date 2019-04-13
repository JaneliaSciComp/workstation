package org.janelia.horta.nodes;

import org.janelia.geometry3d.Vector3;
import org.janelia.horta.modelapi.SwcVertex;

/**
 *
 * @author Christopher Bruns
 */
public class BasicSwcVertex implements SwcVertex
{
    private final float[] location = {0,0,0};
    private float radius = 0.0f; // micrometers
    private int label = 1;
    private int typeIndex = 0;
    private SwcVertex parent = null;

    public BasicSwcVertex(float x, float y, float z)
    {
        location[0] = x;
        location[1] = y;
        location[2] = z;
    }

    @Override
    public float[] getLocation()
    {
        return location;
    }

    public void setLocation(Vector3 location)
    {
        System.arraycopy(location.toArray(), 0, this.location, 0, 3);
    }

    @Override
    public void setLocation(float x, float y, float z)
    {
        location[0] = x;
        location[1] = y;
        location[2] = z;
    }

    @Override
    public float getRadius()
    {
        return 1.0f;
    }

    @Override
    public void setRadius(float radius)
    {
        this.radius = radius;
    }

    @Override
    public int getLabel()
    {
        return label;
    }

    @Override
    public void setLabel(int label)
    {
        this.label = label;
    }

    @Override
    public int getTypeIndex()
    {
        return typeIndex;
    }

    @Override
    public void setTypeIndex(int typeIndex)
    {
        this.typeIndex = typeIndex;
    }

    @Override
    public SwcVertex getParent()
    {
        return parent;
    }

    @Override
    public void setParent(SwcVertex parent)
    {
        this.parent = parent;
    }

    @Override
    public boolean hasRadius()
    {
        return true;
    }

}
