package org.janelia.it.workstation.gui.viewer3d.stream;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Simple wrapper around another InputStream, as a sanity check while
 * developing PbdInputStream.
 * @author brunsc
 *
 */
public class PassThroughInputStream extends FilterInputStream {
	protected PassThroughInputStream(InputStream in) {
		super(in);
	}
}
