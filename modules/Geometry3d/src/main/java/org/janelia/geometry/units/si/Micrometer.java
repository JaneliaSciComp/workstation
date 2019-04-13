package org.janelia.geometry.units.si;

import org.janelia.geometry.units.Unit;
import org.janelia.geometry.units.dimension.Length;

/**
 *
 * @author Christopher Bruns
 */
public class Micrometer 
implements Unit<Length>
{
    @Override
    public String getSymbol() {return "\\u03BCm";}

    @Override
    public String getName() {
        return "micrometer";
    }

    @Override
    public double getFactorToSi() {
        return 1e-6;
    }
}
