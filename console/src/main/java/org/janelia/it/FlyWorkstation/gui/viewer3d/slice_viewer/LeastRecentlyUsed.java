package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

public class LeastRecentlyUsed<E> 
implements Iterable<E>, Collection<E>, Queue<E>
{
	private Map<E, QItem<E>> map = new HashMap<E, QItem<E>>();
	private QItem<E> tail = null;
	private QItem<E> head = null;
	private int maxSize = 300;
	
	@Override
	public boolean add(E item) {
		if (map.containsKey(item))
			return false;
		if (item == null)
			throw new NullPointerException();
		addLast(item);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for (E item : c)
			if (add(item))
				result = true;
		return result;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return map.keySet().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for (Object item : c)
			if (remove(item))
				result = true;
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		Object result[] = new Object[size()];
		int index = 0;
		for (E item : this) {
			result[index] = item;
			index += 1;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] c) {
		T result[] = c;
		if (c.length < size())
			result = (T[]) Array.newInstance(c.getClass().getComponentType(), size());
		else if (result.length > size())
			result[size()] = null;
		int index = 0;
		for (E item : this) {
			result[index] = (T) item;
			index += 1;
		}
		return result;
	}
	
	private void addLast(QItem<E> dqi) {
		if (dqi == null)
			return;
		if (tail == null)
			head = dqi;
		else if (tail.item == dqi.item)
			return; // already at tail
		else
			tail.previous = dqi;
		dqi.next = tail;
		dqi.previous = null;
		tail = dqi;
		map.put(dqi.item, dqi);
	}
	
	public synchronized void addLast(E item) {
		if (item == null)
			return; // we do not accept null items
		// check if it's already first
		if (tail != null)
			if (tail.item == item)
				return; // item is already first!
		QItem<E> dqi = map.get(item);
		if (dqi == null) { // new item
			dqi = new QItem<E>(item);
			addLast(dqi);
			while ((size() > maxSize) && (removeFirst() != null))
				;
		}
		else { // old item, need to move from previous position to head
			if (dqi.next != null)
				dqi.next.previous = dqi.previous;
			if (dqi.previous != null)
				dqi.previous.next = dqi.previous;
			addLast(dqi);
		}
	}
	
	public synchronized void clear() {
		tail = head = null;
		map.clear();
	}
	
	public boolean contains(Object item) {
		return map.containsKey(item);
	}
	
	public E getFirst() {
		if (tail == null)
			return null;
		return tail.item;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Iter<E>(tail);
	}
	
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	
	public int size() {
		return map.size();
	}
	
	public synchronized E removeFirst() {
		if (head == null)
			return null;
		E item = head.item;
		map.remove(item);
		if (map.isEmpty())
			tail = head = null;
		return item;
	}
	
	public synchronized boolean remove(Object item) {
		QItem<E> dqi = map.get(item);
		if (dqi == null)
			return false;
		if (dqi == tail)
			tail = dqi.next;
		if (dqi == head)
			head = dqi.previous;
		if (dqi.next != null)
			dqi.next.previous = dqi.previous;
		if (dqi.previous != null)
			dqi.previous.next = dqi.next;
		map.remove(item);
		return true;
	}	

	
	public static class Iter<E> implements Iterator<E> {
		QItem<E> current;
		
		public Iter(QItem<E> start) {
			this.current = start;
		}
		
		@Override
		public boolean hasNext() {
			return (current != null);
		}

		@Override
		public E next() {
			E result = current.item;
			current = current.next;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	
	static class QItem<E> {
		public QItem<E> previous = null;
		public QItem<E> next = null;
		public E item;
		
		public QItem(E item) {
			this.item = item;
			this.next = this.previous = null;
		}
	}

	
	@Override
	public E element() {
		return head.item;
	}

	@Override
	public boolean offer(E arg0) {
		return add(arg0);
	}

	@Override
	public E peek() {
		return head.item;
	}

	@Override
	public E poll() {
		E result = head.item;
		remove(result);
		return result;
	}

	@Override
	public E remove() {
		if (map.isEmpty())
			throw new NoSuchElementException();
		return poll();
	}


}
