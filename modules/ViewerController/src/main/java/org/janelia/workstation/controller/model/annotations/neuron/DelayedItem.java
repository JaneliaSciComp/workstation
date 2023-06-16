package org.janelia.workstation.controller.model.annotations.neuron;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A work item with a delay. 
 * 
 * Adapted from
 * http://aredko.blogspot.com/2012/04/using-delayed-queues-in-practice.html
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class DelayedItem<T> implements Delayed {

	private final long origin;
	private final long delay;
	private final T item;

	DelayedItem(final T item, final long delay) {
		this.origin = System.currentTimeMillis();
		this.item = item;
		this.delay = delay;
	}

	T getItem() {
		return item;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
	}

	@Override
	public int compareTo(Delayed delayed) {
		if (delayed == this) {
			return 0;
		}

		if (delayed instanceof DelayedItem) {
			DelayedItem other = ((DelayedItem) delayed);
			long diff = (origin+delay) - (other.origin+other.delay);
			if (diff==0) {
				if (item.equals(other.getItem())) {
					return 0;
				} else {
					return -1; // need something to indicate they are different.
				}
			} else if (diff<0) {
				return -1;
			}
			return 1;
		}

		long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
		return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
	}

	@Override
	public int hashCode() {
		final int prime = 31;

		int result = 1;
		result = prime * result + ((item == null) ? 0 : item.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof DelayedItem)) {
			return false;
		}

		final DelayedItem other = (DelayedItem) obj;
		if (item == null) {
			if (other.item != null) {
				return false;
			}
		} else if (!item.equals(other.item)) {
			return false;
		}

		return true;
	}
}
