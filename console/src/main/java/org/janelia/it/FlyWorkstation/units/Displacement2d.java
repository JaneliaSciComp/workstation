package org.janelia.it.FlyWorkstation.units;

/**
 * Units-aware point or vector in 2D space.
 * @author brunsc
 *
 */
public interface Displacement2d
{
	PhysicalQuantity<BaseDimension.Length> getX();
	PhysicalQuantity<BaseDimension.Length> getY();
}
