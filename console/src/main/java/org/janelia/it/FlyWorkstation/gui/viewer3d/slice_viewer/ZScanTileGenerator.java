package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Generator representing a tile distance function like 
 *   f(x,y,z,lod) = abs(z-z0) for x/y/lod unchanged,
 *                  infinity otherwise
 * @author brunsc
 *
 */
public class ZScanTileGenerator 
implements Iterator<TileIndex>
{
	ZIter zIter;
	TileSet baseTiles;
	Iterator<Tile2d> baseIter;
	int z;

	public ZScanTileGenerator(TileSet tileSet, TileFormat tileFormat) {
		this.baseTiles = tileSet;
		// NOTE - assumes that all tiles in set have same Z value
		TileIndex exampleIndex = tileSet.iterator().next().getIndex();
		int deltaZ = 1;
		int zMin = tileFormat.getOrigin()[2];
		int zMax = zMin + tileFormat.getVolumeSize()[2];
		zIter = new ZIter(exampleIndex.getZ(), 
				deltaZ,
				zMin, zMax);
		this.baseIter = baseTiles.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return (zIter.hasNext() || baseIter.hasNext());
	}

	@Override
	public TileIndex next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	};
	
	
	/**
	 * Alternate positive and negative Z offsets, with increasing offset
	 * out to some limits.
	 * 
	 * @author brunsc
	 * 
	 */
	private static class ZIter implements Iterator<Integer>
	{
		private PosIter pos;
		private NegIter neg;
		private boolean doPos = true;
		
		public ZIter(int start, int delta, int min, int max) {
			pos = new PosIter(start, delta, max);
			neg = new NegIter(start, delta, min);
			// increment neg, because it is initially the same at pos, at zero.
			neg.next();
		}
		
		@Override
		public boolean hasNext() {
			return (pos.hasNext() || neg.hasNext());
		}

		@Override
		public Integer next() {
			if (doPos) {
				if (pos.hasNext()) {
					doPos = false;
					return pos.next();
				}
				else
					return neg.next();
			}
			else {
				if (neg.hasNext()) {
					doPos = true;
					return neg.next();
				}
				else
					return pos.next();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	};

	
	private static class PosIter implements Iterator<Integer>
	{
		private int max = 0;
		private int delta = 0;
		private int val = 0;
		
		public PosIter(int start, int delta, int max) {
			this.val = start;
			this.delta = delta;
			this.max = max;
		}
		
		@Override
		public boolean hasNext() {
			return val <= max;
		}

		@Override
		public Integer next() {
			int result = val;
			val += delta;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	};


	private static class NegIter implements Iterator<Integer>
	{
		private int min = 0;
		private int delta = 0;
		private int val = 0;
		
		public NegIter(int start, int delta, int min) {
			this.val = start;
			this.delta = delta;
			this.min = min;
		}
		
		@Override
		public boolean hasNext() {
			return val >= min;
		}

		@Override
		public Integer next() {
			int result = val;
			val -= delta;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

}
