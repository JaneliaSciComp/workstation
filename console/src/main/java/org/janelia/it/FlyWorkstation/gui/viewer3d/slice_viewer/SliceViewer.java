package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;

public class SliceViewer 
extends BaseGLViewer
{
	private static final long serialVersionUID = 1L;
	protected SliceRenderer renderer;

	public SliceViewer() {
		this(new SliceRenderer());
		this.renderer.setBackgroundColor(new Color(1.0f, 0.8f, 0.8f, 0.0f));
	}
	
	public SliceViewer(SliceRenderer renderer) {
		super(renderer);
		this.renderer = renderer;
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
