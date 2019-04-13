package org.janelia.gltools.scenegraph;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Christopher Bruns
 */
public class RenderViewport
{
    private int widthPixels;
    private int heightPixels;
    private int originLeftPixels;
    private int originBottomPixels;
    private float aspectRatio;
    
    // How much of the parent drawable surface does this viewport consume?
    private float originLeftRelative;
    private float originBottomRelative;
    private float widthRelative;
    private float heightRelative;
    
    private List<CameraNode> cameras = new ArrayList<>();

    public RenderViewport(CameraNode cameraNode) {
        // Default to zero size
        this.widthPixels = 0;
        this.heightPixels = 0;
        this.originBottomPixels = 0;
        this.originLeftPixels = 0;

        // Default to occupying full canvas
        this.aspectRatio = 1.0f;
        this.heightRelative = 1.0f;
        this.widthRelative = 1.0f;
        this.originBottomRelative = 0.0f;
        this.originLeftRelative = 0.0f;
    }
    
    public int getWidthPixels()
    {
        return widthPixels;
    }

    public void setWidthPixels(int widthPixels)
    {
        this.widthPixels = widthPixels;
    }

    public int getHeightPixels()
    {
        return heightPixels;
    }

    public void setHeightPixels(int heightPixels)
    {
        this.heightPixels = heightPixels;
    }

    public int getOriginLeftPixels()
    {
        return originLeftPixels;
    }

    public void setOriginLeftPixels(int originLeftPixels)
    {
        this.originLeftPixels = originLeftPixels;
    }

    public int getOriginBottomPixels()
    {
        return originBottomPixels;
    }

    public void setOriginBottomPixels(int originBottomPixels)
    {
        this.originBottomPixels = originBottomPixels;
    }

    public float getAspectRatio()
    {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio)
    {
        this.aspectRatio = aspectRatio;
    }

    public float getOriginLeftRelative()
    {
        return originLeftRelative;
    }

    public void setOriginLeftRelative(float originLeftRelative)
    {
        this.originLeftRelative = originLeftRelative;
    }

    public float getOriginBottomRelative()
    {
        return originBottomRelative;
    }

    public void setOriginBottomRelative(float originBottomRelative)
    {
        this.originBottomRelative = originBottomRelative;
    }

    public float getWidthRelative()
    {
        return widthRelative;
    }

    public void setWidthRelative(float widthRelative)
    {
        this.widthRelative = widthRelative;
    }

    public float getHeightRelative()
    {
        return heightRelative;
    }

    public void setHeightRelative(float heightRelative)
    {
        this.heightRelative = heightRelative;
    }

    List<CameraNode> getCameras()
    {
        return cameras;
    }
    
}
