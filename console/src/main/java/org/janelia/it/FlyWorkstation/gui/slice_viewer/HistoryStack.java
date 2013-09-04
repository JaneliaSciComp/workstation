package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.util.List;
import java.util.Vector;

public class HistoryStack<E>
{
	private List<E> stack = new Vector<E>();
	private Integer currentIndex = null;

	public E back() {
		if (currentIndex == null)
			return null;
		if (currentIndex <= 0)
			return null;
		currentIndex -= 1;
		return stack.get(currentIndex);
	}
	
	public void clear() {
		stack.clear();
		currentIndex = null;
	}
	
	public E next() {
		if (currentIndex == null)
			return null;
		if (currentIndex >= (stack.size() - 1))
			return null;
		currentIndex += 1;
		return stack.get(currentIndex);
	}
	
	public void push(E item) {
		if (item == null)
			return;
		// Don't duplicate current item
		// (and leave future intact)
		if ( (currentIndex != null) && (stack.get(currentIndex) == item) )
			return;
		if (stack.size() == 0)
			currentIndex = 0;
		else
			currentIndex += 1;
		// Delete later entries, like a browser does
		// (delete future)
		for (int i = currentIndex; i < stack.size(); ++i)
			stack.remove(i);
		stack.add(item);
		assert(currentIndex == (stack.size() - 1));
	}

	public void remove(E item) {
		synchronized(stack) {
			// 1 - Adjust currentIndex for what will be new arrangement
			if (currentIndex != null) {
				int max = Math.min(currentIndex, stack.size()-1);
				for (int i = 0; i <= max; ++i)
					if (stack.get(i) == item)
						currentIndex -= 1; // There will be one less item
				if (currentIndex < 0)
					currentIndex = 0;
			}
			// 2 - Actually remove items
			while (stack.remove(item))
				; // keep removing until none of this item are left
			if (stack.size() == 0)
				currentIndex = null;
		}
	}
}
