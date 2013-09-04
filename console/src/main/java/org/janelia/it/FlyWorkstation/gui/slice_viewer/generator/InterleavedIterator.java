package org.janelia.it.FlyWorkstation.gui.slice_viewer.generator;

import java.util.Iterator;

/**
 * Alternately generates values from two iterators, until both are exhausted.
 * @author brunsc
 * @param <E>
 *
 */
public class InterleavedIterator<E> 
implements Iterator<E>, Iterable<E> 
{
	private Iterator<E> first;
	private Iterator<E> second;
	private boolean useFirst = true;
	
	public InterleavedIterator(Iterable<E> first, Iterable<E> second) {
		this.first = first.iterator();
		this.second = second.iterator();
	}

	@Override
	public boolean hasNext() {
		return (first.hasNext() || second.hasNext());
	}

	@Override
	public E next() {
		Iterator<E> primary, secondary;
		if (useFirst) {
			primary = first;
			secondary = second;
		} else {
			primary = second;
			secondary = first;
		}
		useFirst = ! useFirst; // swap for next time
		if (primary.hasNext()) {
			E result = primary.next();
			return result;
		}
		else {
			E result = secondary.next();
			return result; // because primary is exhausted
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(); // because I'm lazy
	}

	@Override
	public Iterator<E> iterator() {
		return this;
	}
}
