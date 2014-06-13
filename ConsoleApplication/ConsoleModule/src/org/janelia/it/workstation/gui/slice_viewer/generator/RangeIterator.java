package org.janelia.it.workstation.gui.slice_viewer.generator;

import java.util.Iterator;

/**
 * Works like python xrange().
 * Produces a sequence of integers up to, but NOT INCLUDING
 * limit.
 * 
 * @author brunsc
 *
 */
public class RangeIterator 
implements Iterator<Integer>, Iterable<Integer> 
{
	private int start = 0;
	private int value = 0;
	private int increment = 1;
	private int limit;
	
	public RangeIterator(int limit) {
		this.limit = limit;
	}
	
	public RangeIterator(int start, int limit) {
		this.start = this.value = start;
		this.limit = limit;
	}
	
	public RangeIterator(int start, int limit, int increment) {
		this.start = this.value = start;
		this.limit = limit;
		this.increment = increment;
	}

	@Override
	public boolean hasNext() {
		if ((limit - start) >= 0)
			return (value < limit); // increasing sequence
		else
			return (value > limit); // decreasing sequence
	}

	@Override
	public Integer next() {
		int result = value;
		value = value + increment;
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Integer> iterator() {
		return this;
	}
}
