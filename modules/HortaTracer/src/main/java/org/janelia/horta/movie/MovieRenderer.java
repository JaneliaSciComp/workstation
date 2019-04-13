package org.janelia.horta.movie;

import java.awt.image.BufferedImage;

/**
 *
 * @author brunsc
 */
public interface MovieRenderer
{
    BufferedImage getRenderedFrame(ViewerState state);
    BufferedImage getRenderedFrame(ViewerState state, int imageWidth, int imageHeight);
    boolean supportsCustomSize();
}
