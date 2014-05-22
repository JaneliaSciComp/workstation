package org.janelia.it.workstation.gui.slice_viewer.generator;

import org.janelia.it.workstation.gui.slice_viewer.generator.InterleavedIterator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import static org.junit.Assert.*;

public class TestInterleavedIterator {

	@Test
	public void test() {
		// Make sure it works for at least the alternating z-slice use case
		Integer a[] = {0, -1, 1, -2, 2, -3, -4};
		List<Integer> expectedResult = Arrays.asList(a);
		//
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r1 = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(3);
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r2 = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(-1, -5, -1);
		org.janelia.it.workstation.gui.slice_viewer.generator.InterleavedIterator<Integer> i =
				new org.janelia.it.workstation.gui.slice_viewer.generator.InterleavedIterator<Integer>(r1, r2);
		List<Integer> actualResult = new Vector<Integer>();
		for (int val : i)
			actualResult.add(val);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void testRemove() {
		// This is not a useful behavior. Perhaps it should not be tested.
		org.janelia.it.workstation.gui.slice_viewer.generator.InterleavedIterator<Integer> i =
				new InterleavedIterator<Integer>(null, null);
		try {
			i.remove();
		} catch (UnsupportedOperationException e) {
			return;
		}
		fail("No exception thrown");
	}
}
