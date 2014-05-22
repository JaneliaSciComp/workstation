package org.janelia.it.workstation.gui.slice_viewer.generator;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRangeIterator {

	@Test
	public void testPositiveControl() {
		assertTrue(true);
		assertFalse(false);
	}
	
	@Test
	public void testRangeIteratorConstructor() {
		//
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0, 1, 1);
		assertTrue(r.hasNext());
		r.next();
		assertFalse(r.hasNext());
		// 
		r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0, 1, 2);
		assertTrue(r.hasNext());
		r.next();
		assertFalse(r.hasNext());
		// 
		r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0, 2, 2);
		assertTrue(r.hasNext());
		r.next();
		assertFalse(r.hasNext());
		// 
		r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0, 2, 1);
		assertTrue(r.hasNext());
		r.next();
		assertTrue(r.hasNext());
		r.next();
		assertFalse(r.hasNext());
	}

	@Test
	public void testHasNext() {
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(1);
		assertTrue(r.hasNext());
		r.next();
		assertFalse(r.hasNext());
		r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0);
		assertFalse(r.hasNext());
	}

	@Test
	public void testNext() {
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(0, 1, 1);
		assertEquals(0, r.next().intValue());
	}

	@Test
	public void testRemove() {
		org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator r = new org.janelia.it.workstation.gui.slice_viewer.generator.RangeIterator(1);
		r.next();
		try {
			r.remove();
		} catch (UnsupportedOperationException e) {
			return;
		}
		fail("No exception thrown");
	}

}
