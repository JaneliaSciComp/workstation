package org.janelia.it.FlyWorkstation.gui.slice_viewer.generator;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

public class TestInterleavedIterator {

	@Test
	public void test() {
		// Make sure it works for at least the alternating z-slice use case
		Integer a[] = {0, -1, 1, -2, 2, -3, -4};
		List<Integer> expectedResult = Arrays.asList(a);
		//
		RangeIterator r1 = new RangeIterator(3);
		RangeIterator r2 = new RangeIterator(-1, -5, -1);
		InterleavedIterator<Integer> i = 
				new InterleavedIterator<Integer>(r1, r2);
		List<Integer> actualResult = new Vector<Integer>();
		for (int val : i)
			actualResult.add(val);
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void testRemove() {
		// This is not a useful behavior. Perhaps it should not be tested.
		InterleavedIterator<Integer> i = 
				new InterleavedIterator<Integer>(null, null);
		try {
			i.remove();
		} catch (UnsupportedOperationException e) {
			return;
		}
		fail("No exception thrown");
	}
}
