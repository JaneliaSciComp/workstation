package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.ScaleFitter;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/14/13
 * Time: 4:52 PM
 *
 * This will draw and update a scale bar on screen.
 */
public class ScaledMip3d extends Mip3d {

    public static final int MAX_LINEWIDTH = 300;
    public static final int MIN_LINE_WIDTH = 85;

    private ScaleFitter fitter;
    private int previousClosesPixelWidth = (MAX_LINEWIDTH + MIN_LINE_WIDTH) / 2;

    public ScaledMip3d() {
        fitter = new ScaleFitter( 0.01, MIN_LINE_WIDTH, MAX_LINEWIDTH - MIN_LINE_WIDTH );
    }

    /**
     * Overriding the paint method so can over-draw the scale.
     *
     * @param graphics
     */
    @Override
    public void paint( Graphics graphics ) {
        super.paint( graphics );
        if ( this.getVolumeModel() == null  ||  this.getVolumeModel().getCamera3d() == null ) {
            return;
        }

        try {
            double pixelsPerSceneUnit = getVolumeModel().getCamera3d().getPixelsPerSceneUnit();
            //        / getVolumeModel().getVoxelMicrometers()[ 0 ];
            System.out.println("Pixels per scene unit = "+ pixelsPerSceneUnit);
            double factor = 1.0 / pixelsPerSceneUnit;
            ScaleFitter.FitReportBean fitReport = fitter.findClosestPixelWidth(factor, 1, previousClosesPixelWidth);

            if ( fitReport.getMinInx() < (previousClosesPixelWidth - 40 )) {
                //DEBUG System.out.println("Recalculating");
                fitReport = fitter.findClosestPixelWidth(factor, -1, previousClosesPixelWidth);
            }
            previousClosesPixelWidth = fitReport.getMinInx();
            System.out.println("Found a close value of " + fitReport.getValue() + ", and a line width of " + fitReport.getPixelCount());

            graphics.setColor( Color.white );
            String labelText = (int)Math.round( fitReport.getValue() ) + " \u03BCm";
            graphics.setFont( new Font("Ariel", Font.PLAIN, 10 ) );
            int textHeight = graphics.getFontMetrics().getAscent(); // This is a better "height" estimate for most characters.
            int textWidth = graphics.getFontMetrics().charsWidth( labelText.toCharArray(), 0, labelText.length() );

            int lineWidth = fitReport.getPixelCount();

            int textY = this.getHeight() - textHeight - 15;
            int lineY = textY + textHeight;
            int endX = this.getWidth() - 5;
            int startX = endX - lineWidth;
            graphics.drawString(labelText, endX - textWidth, textY );
            graphics.drawLine( startX, lineY, endX, lineY );

        } catch ( Exception ex ) {
            ex.printStackTrace();
            throw new RuntimeException( ex );

        }

    }

    /* Here will be a listener to the camera, so that the proper line length may be reflected. */
}
