package org.janelia.horta.render;

import java.awt.Color;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.ColorBackgroundActor;
import org.janelia.gltools.RenderPass;

/**
 * BackgroundRenderPass clears the background on the main display screen.
 * @author Christopher Bruns
 */
public class BackgroundRenderPass extends RenderPass
{
    private final ColorBackgroundActor backgroundActor;

    public BackgroundRenderPass() {
        super(null);
        Color topColor = new Color(0.02f, 0.01f, 0.00f, 0.0f);
        Color bottomColor = new Color(0.10f, 0.06f, 0.00f, 0.0f);
        backgroundActor = new ColorBackgroundActor(
                topColor, 
                bottomColor);
        addActor(backgroundActor);
        setCacheResults(false);
    }
    
    public void setColor(Color topColor, Color bottomColor) {
        backgroundActor.setColor(topColor, bottomColor);
    }

}
