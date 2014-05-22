package org.janelia.it.workstation.geom;

import java.util.Collection;
import java.util.Vector;

public class SizedVector<E> extends Vector<E> 
{
	private static final long serialVersionUID = 1L;

	public SizedVector(int capacity) {
		super(capacity);
		super.setSize(capacity);
	}
	
	// Ah Java.  Much of the work is turning stuff off.
	// Disable any methods that might change the size.
	@Override
	public boolean add(E element) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void addElement(E element) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	@Override
	public void insertElementAt(E element, int index) {
		throw new UnsupportedOperationException();
	}
	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void removeAllElements() {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean removeElement(Object obj) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void removeElementAt(int index) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void removeRange(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setSize(int newSize) {
		throw new UnsupportedOperationException();
	}
}
