package org.janelia.it.workstation.units;

/**
 * Units-aware point or vector in 2D space.
 * @author brunsc
 *
 */
public interface Displacement2d
{
	PhysicalQuantity<org.janelia.it.workstation.units.BaseDimension.Length> getX();
	PhysicalQuantity<org.janelia.it.workstation.units.BaseDimension.Length> getY();
}
