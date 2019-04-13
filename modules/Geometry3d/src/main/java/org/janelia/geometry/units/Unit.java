package org.janelia.geometry.units;

import org.janelia.geometry.units.dimension.Dimension;

/**
 *
 * @author Christopher Bruns
 */
public interface Unit<D extends Dimension> {
    public String getSymbol();
    public String getName();
    public double getFactorToSi();
}
