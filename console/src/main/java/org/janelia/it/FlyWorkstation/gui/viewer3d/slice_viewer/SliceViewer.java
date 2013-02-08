package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Observable;
import java.util.Observer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;

public class SliceViewer 
extends BaseGLViewer
implements Observer
{
	private static final long serialVersionUID = 1L;
	protected SliceRenderer renderer;

	public SliceViewer() {
		this(new SliceRenderer());
		// pink color for testing only
		this.renderer.setBackgroundColor(new Color(1.0f, 0.8f, 0.8f, 0.0f));
	}
	
	public SliceViewer(SliceRenderer renderer)
	{
		super(renderer);
		this.renderer = renderer;
		renderer.getCamera().addObserver(this);
        setPreferredSize( new Dimension( 400, 400 ) );
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
	
	@Override
	public void update(Observable arg0, Object arg1) {
		repaint();
	}
}
