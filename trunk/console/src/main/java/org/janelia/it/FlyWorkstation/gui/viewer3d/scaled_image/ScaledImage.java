package org.janelia.it.FlyWorkstation.gui.viewer3d.scaled_image;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 11/9/12
 * Time: 12:38 PM
 *
 * Image to adjust the size of another image, for convenience.
 */

import javax.media.jai.JAI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ScaledImage extends BufferedImage {
    private int scaledWidth;
    private int scaledHeight;

    public ScaledImage(int scaledWidth, int scaledHeight, BufferedImage rawImage) {
        super( scaledWidth, scaledHeight, rawImage.getType() );
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;

        // Scale up the default image.
        BufferedImage finalImage = getScaledImage( rawImage );

        // Get the raster from the default image.
        WritableRaster raster = this.getRaster();
        raster.setRect( finalImage.getRaster() );
        rawImage.setData(raster);
    }

    @Override
    public int getWidth() {
        return scaledWidth;
    }

    @Override
    public int getHeight() {
        return scaledHeight;
    }

    private BufferedImage getScaledImage( BufferedImage rawImage ) {
        int imageWidth = rawImage.getWidth();

        BufferedImage rtnVal = rawImage;
        double scalingFactor = (double) scaledWidth / (double)imageWidth;

        if ( scalingFactor != 1.0 ) {
            RenderingHints localHints = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);

            AffineTransform affineTransform = new AffineTransform();
            affineTransform.scale( scalingFactor, scalingFactor );

            AffineTransformOp scaleOp =
                    new AffineTransformOp(affineTransform, localHints );//AffineTransformOp.TYPE_BILINEAR);
            BufferedImage targetImage = new BufferedImage(scaledWidth, scaledHeight, rawImage.getType());
            scaleOp.filter( rawImage, targetImage );
            rtnVal = targetImage;
        }

        return rtnVal;
    }

}

