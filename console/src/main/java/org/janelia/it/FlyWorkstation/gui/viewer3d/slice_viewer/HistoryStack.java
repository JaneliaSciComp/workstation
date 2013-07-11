package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.List;
import java.util.Vector;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;

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
		int i = 0;
		while (i < stack.size()) {
			while (stack.get(i) == item) {
				stack.remove(i);
				if (currentIndex >= i)
					currentIndex -= 1;
			}
			i += 1;
		}
	}
}
