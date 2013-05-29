package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;

public class TraceMouseModeAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	protected MouseModalWidget widget;
	protected TraceMode traceMode;
	
	public TraceMouseModeAction(MouseModalWidget widget, Skeleton skeleton) {
		putValue(NAME, "Trace");
		putValue(SMALL_ICON, Icons.getIcon("nib.png"));
		String acc = "P";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"<html>"
				+"Set mouse mode to trace neurons ["+acc+"]<br>"
				+"<br>"
				+"SHIFT-click to place a new anchor<br>" // done
				+"Click and drag to move anchor<br>" // done
				+"Click to designate parent anchor<br>" // done
				+"Middle-button drag to Pan XY<br>" // done
				+"Scroll wheel to scan Z<br>" // done
				+"SHIFT-scroll wheel to zoom<br>" // done
				+"Right-click for context menu" // TODO
				+"</html>");
		traceMode = new TraceMode(skeleton);
		traceMode.setComponent(widget);
		this.widget = widget;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		widget.setMouseMode(traceMode);
		putValue(SELECTED_KEY, true);
	}

	public TraceMode getTraceMode() {
		return traceMode;
	}

	public void setTraceMode(TraceMode traceMode) {
		this.traceMode = traceMode;
	}

}
