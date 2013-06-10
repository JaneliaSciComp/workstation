package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.RangeSlider;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 9:11 AM
 *
 * Common-use methods for getting standardized coordinate end points from sliders.
 */
public class CoordCropper3D {
    /**
     * Given the user-selection among the sliders, return the full set of start/end coords in all three
     * dimensions.  If a non 1.0 down sample rate is given retro-adjust so that the pre-downsampled
     * value is used.
     *
     * @param xRS range slider for x-axis
     * @param yRS range slider for y-axis
     * @param zRS range slider for z-axis
     * @param downSampleRate how much was the volume down-sampled (a divisor) before presentation under sliders?
     * @return coords the user has chosen, using the sliders.
     */
    public float[] getCropCoords( RangeSlider xRS, RangeSlider yRS, RangeSlider zRS, double downSampleRate ) {
        return getCropCoords( new RangeSlider[]{ xRS, yRS, zRS }, downSampleRate );
    }

    public float[] getCropCoords( RangeSlider[] sliders, double downSampleRate ) {
        float[] cropCoords = new float[ 6 ];
        for ( int i = 0; i < 3; i++ ) {
            cropCoords[ i * 2 ] = (float)sliders[ i ].getValue() * (float)downSampleRate;
            cropCoords[ i * 2 + 1 ] = (float)sliders[ i ].getUpperValue() * (float)downSampleRate;
        }
        return cropCoords;
    }

    /**
     * Given the sliders using which, the users have set their desired export ranges, return the fractional versions
     * of the crop coords.  This is useful for applications (like GPU) which use normalized 0.0..1.0 coordinate
     * systems.
     *
     * @param sliders adjusted by user to indicate required capture region.
     * @return normalized coordinates 0-1 range.
     */
    public float[] getNormalizedCropCoords( RangeSlider[] sliders ) {
        float[] cropCoords = new float[ 6 ];
        for ( int i = 0; i < 3; i++ ) {
            cropCoords[ i * 2 ] = (float)sliders[ i ].getValue() / (float)sliders[ i ].getMaximum();
            cropCoords[ i * 2 + 1 ] = (float)sliders[ i ].getUpperValue()  / (float)sliders[ i ].getMaximum();
        }
        return cropCoords;
    }

    /**
     * Given coords already normalized to 0..1 range, this method back-converts them to absolute, micrometer
     * based coordinates.  This makes them compatible with the original volume, rather than with the GPU.
     *
     * @param normalizedCoords 0..1.0 range, start/stop in x,y,z
     * @param maxima maximum values in x, y, z.
     * @return de-fractional versions along axes.
     */
    public float[] getDenormalizedCropCoords( float[] normalizedCoords, int[] maxima, double downSampleRate ) {
        float[] cropCoords = new float[ 6 ];
        for ( int i = 0; i < 3; i++ ) {
            cropCoords[ i * 2 ] = maxima[ i ] * normalizedCoords[ i * 2 ] * (int)downSampleRate;
            cropCoords[ i * 2 + 1 ] = maxima[ i ] * normalizedCoords[ i * 2 + 1 ] * (int)downSampleRate;
        }

        for ( int i = 0; i < cropCoords.length; i++ ) {
            System.out.print( cropCoords[ i ] + "  ");
        }
        System.out.println();

        return cropCoords;
    }

    /**
     * Given coords already normalized to 0..1 range, this method back-converts them to absolute, micrometer
     * based coordinates.  This makes them compatible with the original volume, rather than with the GPU.
     *
     * @param normalizedCoords 0..1.0 range, start/stop in x,y,z
     * @param maxima maximum values in x, y, z.
     * @return de-fractional versions along axes.
     */
    public float[] getDenormalizedCropCoords( float[] normalizedCoords, int[] maxima ) {
        float[] cropCoords = new float[ 6 ];
        for ( int i = 0; i < 3; i++ ) {
            cropCoords[ i * 2 ] = maxima[ i ] * normalizedCoords[ i * 2 ];
            cropCoords[ i * 2 + 1 ] = maxima[ i ] * normalizedCoords[ i * 2 + 1 ];
        }

        for ( int i = 0; i < cropCoords.length; i++ ) {
            System.out.print( cropCoords[ i ] + "  ");
        }
        System.out.println();

        return cropCoords;
    }

}
