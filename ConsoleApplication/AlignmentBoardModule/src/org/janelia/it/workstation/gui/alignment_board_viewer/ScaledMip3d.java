package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.ScaleFitter;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/14/13
 * Time: 4:52 PM
 *
 * This will draw and update a scale bar on screen.
 */
public class ScaledMip3d extends Mip3d {

    public static final int MAX_LINEWIDTH = 240;
    public static final int MIN_LINE_WIDTH = 85;
    public static final String SCALE_UNITS = " \u03BCm";

    private ScaleFitter fitter;
    private int previousMinimumIndex = ((MAX_LINEWIDTH + MIN_LINE_WIDTH) / 2) - MIN_LINE_WIDTH;

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
            ScaleFitter.FitReportBean fitReport = getFitReport();
            previousMinimumIndex = fitReport.getMinInx();
            String labelText = (int)Math.round( fitReport.getValue() ) + SCALE_UNITS;
            paintScale(graphics, fitReport, labelText);

        } catch ( Exception ex ) {
            ex.printStackTrace();
            throw new RuntimeException( ex );

        }

    }

    private void paintScale( Graphics graphics, ScaleFitter.FitReportBean fitReport, String labelText ) {
        graphics.setColor( Color.white );
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
    }

    private ScaleFitter.FitReportBean getFitReport() throws Exception {
        double pixelsPerSceneUnit = getVolumeModel().getCamera3d().getPixelsPerSceneUnit();
        //System.out.println("Pixels per scene unit = "+ pixelsPerSceneUnit);
        double axisLengthDivisor = getAxisLengthDivisor();
        if ( axisLengthDivisor == 0.0 ) {
            axisLengthDivisor = 1.0;
        }
        double factor = ( getVolumeModel().getVoxelMicrometers()[0] / pixelsPerSceneUnit ) * axisLengthDivisor ;
        ScaleFitter.FitReportBean fitReport = fitter.findClosestPixelWidth(factor, 1, previousMinimumIndex);

        // If the value changes too much, see if we can get closer going the other way.
        if ( fitReport.getMinInx() < (previousMinimumIndex - 10 )) {
            // Try and find a value within tolerance, that is still closer to the previous value.
            ScaleFitter.FitReportBean oppositeFitReport = fitter.findClosestPixelWidth(factor, -1, previousMinimumIndex);
            if ( Math.abs( oppositeFitReport.getMinInx() - previousMinimumIndex ) <
                 Math.abs( fitReport.getMinInx() - previousMinimumIndex ) ) {
                fitReport = oppositeFitReport;
            }
        }
        //System.out.println("Found a close value of " + fitReport.getValue() + ", and a line width of " + fitReport.getPixelCount());
        return fitReport;
    }

}
