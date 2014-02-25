package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/5/13
 * Time: 4:47 PM
 *
 * Inverts the comparison order of two renderable beans.
 */
public class InvertingComparator implements Comparator<RenderableBean> {
    private Comparator<RenderableBean> wrappedComparator;
    public InvertingComparator( Comparator<RenderableBean> wrappedComparator ) {
        this.wrappedComparator = wrappedComparator;
    }
    public int compare(RenderableBean first, RenderableBean second) {
        return wrappedComparator.compare( first, second ) * -1;
    }
}
